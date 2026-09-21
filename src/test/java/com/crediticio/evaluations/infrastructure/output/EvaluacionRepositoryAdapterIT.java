package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.infrastructure.output.ReglaScoringRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración de persistencia contra PostgreSQL local real (no H2), siguiendo el
 * mismo patrón que {@code ReglaScoringRepositoryAdapterIT} (HU05).
 *
 * Requiere que PostgreSQL local esté disponible y que las variables de entorno DB_URL,
 * DB_USERNAME y DB_PASSWORD apunten a una base de datos accesible, usando el perfil "it"
 * (src/test/resources/application-it.properties). Flyway aplicará
 * V5__create_evaluacion_tables.sql contra esa base al iniciar el contexto.
 *
 * Al terminar en "IT" (no "Test"), Surefire no la ejecuta con `./mvnw test`.
 * Ejecución manual: ./mvnw test -Dtest=EvaluacionRepositoryAdapterIT -Dspring.profiles.active=it
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@Import({EvaluacionRepositoryAdapter.class, ReglaScoringRepositoryAdapter.class})
class EvaluacionRepositoryAdapterIT {

    @Autowired
    private EvaluacionRepositoryAdapter evaluacionRepositoryAdapter;

    @Autowired
    private ReglaScoringRepositoryAdapter reglaScoringRepositoryAdapter;

    @Autowired
    private EvaluacionJpaRepository evaluacionJpaRepository;

    @Autowired
    private DetalleEvaluacionJpaRepository detalleEvaluacionJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void elContextoCargaConFlywayYLaValidacionDeEsquemaDeHibernate() {
        assertThat(evaluacionRepositoryAdapter).isNotNull();
        assertThat(evaluacionJpaRepository).isNotNull();
    }

    @Test
    void debeGuardarLaEvaluacionYGenerarIdYFechaDeEvaluacion() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        ReglaFixture regla = crearReglaFixture(idRiesgo, ">=", "3000000", 20);

        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante,
                List.of(DetalleEvaluacion.nuevo(regla.idRegla(), ">=", regla.valorCondicion(), 20, true, 20)));

        Evaluacion guardada = evaluacionRepositoryAdapter.guardar(evaluacion);

        assertThat(guardada.getIdEvaluacion()).isNotNull();
        assertThat(guardada.getIdSolicitante()).isEqualTo(idSolicitante);
        assertThat(guardada.getFechaEvaluacion()).isNotNull();
    }

    @Test
    void debePersistirTodosLosDetallesDeLaEvaluacionConSuSnapshot() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Long idRiesgo1 = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        Long idRiesgo2 = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NUMERO_MORAS);
        ReglaFixture regla1 = crearReglaFixture(idRiesgo1, ">=", "3000000", 20);
        ReglaFixture regla2 = crearReglaFixture(idRiesgo2, "=", "0", 10);

        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante, List.of(
                DetalleEvaluacion.nuevo(regla1.idRegla(), ">=", regla1.valorCondicion(), 20, true, 20),
                DetalleEvaluacion.nuevo(regla2.idRegla(), "=", regla2.valorCondicion(), 10, false, 0)));

        Evaluacion guardada = evaluacionRepositoryAdapter.guardar(evaluacion);

        List<DetalleEvaluacionJpaEntity> detallesPersistidos =
                detalleEvaluacionJpaRepository.findByIdEvaluacion(guardada.getIdEvaluacion());
        assertThat(detallesPersistidos).hasSize(2);
        assertThat(guardada.getScoreTotal()).isEqualTo(20);
        assertThat(guardada.getDetalles()).extracting(DetalleEvaluacion::getIdDetalleEvaluacion)
                .doesNotContainNull();
    }

    @Test
    void debePreservarElSnapshotHistoricoDespuesDeEditarLaReglaOriginal() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO);
        ReglaFixture regla = crearReglaFixture(idRiesgo, "<=", "40", 10);

        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante,
                List.of(DetalleEvaluacion.nuevo(regla.idRegla(), "<=", regla.valorCondicion(), 10, true, 10)));
        Evaluacion guardada = evaluacionRepositoryAdapter.guardar(evaluacion);

        // La regla se edita después (mismo registro, HU06): el detalle histórico no debe cambiar.
        // Se edita a través del propio EntityManager de la prueba (no una conexión JDBC aparte):
        // el detalle recién insertado ya referencia esta fila de regla_scoring por FK, lo que
        // mantiene un lock FOR KEY SHARE sobre ella hasta que termine la transacción de la
        // prueba; una conexión distinta intentando el UPDATE se bloquearía indefinidamente
        // esperando ese lock.
        String valorEditadoUnico = "20." + System.nanoTime();
        ReglaScoring reglaExistente = reglaScoringRepositoryAdapter.buscarPorId(regla.idRegla()).orElseThrow();
        ReglaScoring reglaEditada = reglaExistente.editar(
                OperadorScoring.MENOR, valorEditadoUnico, 5, TipoVariable.NUMERICO, true);
        reglaScoringRepositoryAdapter.guardar(reglaEditada);

        DetalleEvaluacionJpaEntity detallePersistido =
                detalleEvaluacionJpaRepository.findByIdEvaluacion(guardada.getIdEvaluacion()).get(0);
        assertThat(detallePersistido.getOperadorAplicado()).isEqualTo("<=");
        assertThat(detallePersistido.getValorCondicionAplicado()).isEqualTo(regla.valorCondicion());
        assertThat(detallePersistido.getPuntajeReglaAplicado()).isEqualTo(10);
        assertThat(detallePersistido.getPuntajeObtenido()).isEqualTo(10);
    }

    @Test
    void laBaseDeDatosDebeRechazarUnDetalleConIdReglaInexistentePorLaClaveForanea() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante,
                List.of(DetalleEvaluacion.nuevo(-1L, "=", "10", 5, true, 5)));

        assertThatThrownBy(() -> evaluacionRepositoryAdapter.guardar(evaluacion))
                .isInstanceOf(DataIntegrityViolationException.class);

        // La evaluación insertada antes del fallo del detalle no queda visible para otras
        // conexiones: la transacción completa no se confirma (RF15/atomicidad).
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM evaluacion WHERE id_solicitante = ?")) {
            statement.setLong(1, idSolicitante);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getLong(1)).isZero();
            }
        }
    }

    private Long crearSolicitanteFixture() throws SQLException {
        String numeroDocumento = "9" + (System.nanoTime() % 10_000_000_000_000L);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO solicitante (nombre_completo, numero_documento, ingresos_mensuales, "
                                + "deudas_mensuales, numero_moras, historial_crediticio, antiguedad_laboral) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_solicitante")) {
            statement.setString(1, "Solicitante de prueba HU07");
            statement.setString(2, numeroDocumento);
            statement.setBigDecimal(3, new BigDecimal("4000000"));
            statement.setBigDecimal(4, new BigDecimal("1200000"));
            statement.setInt(5, 0);
            statement.setString(6, "BUENO");
            statement.setBigDecimal(7, new BigDecimal("2"));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Long obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo variable) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO riesgo (variable, descripcion, estado) VALUES (?, ?, 'ACTIVA') "
                                + "ON CONFLICT (variable) DO UPDATE SET estado = 'ACTIVA' RETURNING id_riesgo")) {
            statement.setString(1, variable.name());
            statement.setString(2, "Descripción válida con más de diez caracteres");
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private ReglaFixture crearReglaFixture(Long idRiesgo, String operador, String valorCondicion, int puntaje)
            throws SQLException {
        String valorUnico = valorCondicion + "." + System.nanoTime();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_regla")) {
            statement.setLong(1, idRiesgo);
            statement.setString(2, operador);
            statement.setString(3, valorUnico);
            statement.setInt(4, puntaje);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new ReglaFixture(resultSet.getLong(1), valorUnico);
            }
        }
    }

    private record ReglaFixture(Long idRegla, String valorCondicion) {
    }
}
