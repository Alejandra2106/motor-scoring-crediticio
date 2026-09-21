package com.crediticio.scoring.infrastructure.output;

import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.scoring.domain.EstadoReglaScoring;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.domain.TipoVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración de persistencia contra PostgreSQL local real (no H2).
 *
 * Requiere que PostgreSQL local esté disponible y que las variables de entorno
 * DB_URL, DB_USERNAME y DB_PASSWORD apunten a una base de datos accesible, usando
 * el perfil "it" (src/test/resources/application-it.properties). Flyway aplicará
 * V4__create_regla_scoring_table.sql contra esa base al iniciar el contexto.
 *
 * La tabla "riesgo" solo admite un registro por NombreVariableRiesgo (UNIQUE) y puede
 * contener ya datos reales de una verificación manual anterior (HU03/HU04); por eso las
 * variables de riesgo necesarias como fixture se garantizan con un upsert idempotente
 * (obtenerOAsegurarRiesgoActivo) en vez de insertarlas siempre "en limpio", y nunca se
 * eliminan al finalizar.
 *
 * Al terminar en "IT" (no "Test"), Surefire no la ejecuta con `./mvnw test`.
 * Ejecución manual: ./mvnw test -Dtest=ReglaScoringRepositoryAdapterIT -Dspring.profiles.active=it
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@Import(ReglaScoringRepositoryAdapter.class)
class ReglaScoringRepositoryAdapterIT {

    @Autowired
    private ReglaScoringRepositoryAdapter reglaScoringRepositoryAdapter;

    @Autowired
    private ReglaScoringJpaRepository reglaScoringJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void elContextoCargaConFlywayYLaValidacionDeEsquemaDeHibernate() {
        assertThat(reglaScoringRepositoryAdapter).isNotNull();
        assertThat(reglaScoringJpaRepository).isNotNull();
    }

    @Test
    void debeGuardarLaReglaYGenerarIdYFechaDeCreacion() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);

        ReglaScoring guardada = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, TipoVariable.NUMERICO, true));

        assertThat(guardada.getIdRegla()).isNotNull();
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
    }

    @Test
    void debePersistirYPermitirLeerTodosLosCamposDeLaRegla() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO);

        ReglaScoring guardada = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.MENOR_O_IGUAL, "0.40", 10, TipoVariable.NUMERICO, true));

        Optional<ReglaScoringJpaEntity> leida = reglaScoringJpaRepository.findById(guardada.getIdRegla());

        assertThat(leida).isPresent();
        ReglaScoringJpaEntity entity = leida.get();
        assertThat(entity.getIdRiesgo()).isEqualTo(idRiesgo);
        assertThat(entity.getOperador()).isEqualTo("<=");
        assertThat(entity.getValorCondicion()).isEqualTo("0.40");
        assertThat(entity.getPuntaje()).isEqualTo(10);
        assertThat(entity.getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
        assertThat(entity.getFechaCreacion()).isNotNull();
    }

    @Test
    void debeLanzarReglaScoringDuplicadaAlGuardarLaMismaCombinacion() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NUMERO_MORAS);
        String valorUnico = String.valueOf(System.nanoTime() % 1000);
        reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.IGUAL, valorUnico, 5, TipoVariable.NUMERICO, false));

        assertThatThrownBy(() -> reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.IGUAL, valorUnico, 5, TipoVariable.NUMERICO, false)))
                .isInstanceOf(ReglaScoringDuplicadaException.class);
    }

    @Test
    void debeExisteCombinacionDetectarUnaReglaYaRegistradaYNoDetectarUnaDistinta() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.ANTIGUEDAD_LABORAL);
        String valorUnico = String.valueOf(System.nanoTime() % 1000);
        reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.MAYOR, valorUnico, 15, TipoVariable.NUMERICO, true));

        assertThat(reglaScoringRepositoryAdapter.existeCombinacion(idRiesgo, OperadorScoring.MAYOR, valorUnico)).isTrue();
        assertThat(reglaScoringRepositoryAdapter.existeCombinacion(idRiesgo, OperadorScoring.MENOR, valorUnico)).isFalse();
    }

    @Test
    void debeBuscarPorIdUnaReglaExistenteYRetornarVacioSiNoExiste() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        String valorUnico = String.valueOf(System.nanoTime() % 1000);
        ReglaScoring guardada = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.IGUAL, valorUnico, 10, TipoVariable.NUMERICO, true));

        Optional<ReglaScoring> encontrada = reglaScoringRepositoryAdapter.buscarPorId(guardada.getIdRegla());
        Optional<ReglaScoring> noEncontrada = reglaScoringRepositoryAdapter.buscarPorId(-1L);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getValorCondicion()).isEqualTo(valorUnico);
        assertThat(noEncontrada).isEmpty();
    }

    @Test
    void debeActualizarLaReglaExistentePreservandoIdReglaIdRiesgoEstadoYFechaCreacion() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO);
        String valorOriginal = String.valueOf(System.nanoTime() % 1000);
        ReglaScoring guardada = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.MAYOR_O_IGUAL, valorOriginal, 20, TipoVariable.NUMERICO, true));

        ReglaScoring editada = guardada.editar(OperadorScoring.MENOR, "0.40", 30, TipoVariable.NUMERICO, true);
        ReglaScoring actualizada = reglaScoringRepositoryAdapter.guardar(editada);

        assertThat(actualizada.getIdRegla()).isEqualTo(guardada.getIdRegla());
        assertThat(actualizada.getIdRiesgo()).isEqualTo(idRiesgo);
        assertThat(actualizada.getOperador()).isEqualTo(OperadorScoring.MENOR);
        assertThat(actualizada.getValorCondicion()).isEqualTo("0.40");
        assertThat(actualizada.getPuntaje()).isEqualTo(30);
        assertThat(actualizada.getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
        assertThat(actualizada.getFechaCreacion()).isEqualTo(guardada.getFechaCreacion());

        ReglaScoringJpaEntity entityLeida = reglaScoringJpaRepository.findById(guardada.getIdRegla()).orElseThrow();
        assertThat(entityLeida.getOperador()).isEqualTo("<");
        assertThat(entityLeida.getValorCondicion()).isEqualTo("0.40");
        assertThat(entityLeida.getPuntaje()).isEqualTo(30);

        long totalReglasConIdRegla = reglaScoringJpaRepository.count();
        assertThat(reglaScoringJpaRepository.findById(guardada.getIdRegla())).isPresent();
        assertThat(totalReglasConIdRegla).isGreaterThanOrEqualTo(1);
    }

    @Test
    void existeCombinacionConExclusionDebePermitirLaMismaCombinacionDeLaPropiaRegla() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NUMERO_MORAS);
        String valorUnico = String.valueOf(System.nanoTime() % 1000);
        ReglaScoring guardada = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.IGUAL, valorUnico, 5, TipoVariable.NUMERICO, false));

        boolean existeExcluyendoseASiMisma = reglaScoringRepositoryAdapter.existeCombinacion(
                idRiesgo, OperadorScoring.IGUAL, valorUnico, guardada.getIdRegla());

        assertThat(existeExcluyendoseASiMisma).isFalse();
    }

    @Test
    void existeCombinacionConExclusionDebeDetectarDuplicadoContraOtraReglaExistente() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.ANTIGUEDAD_LABORAL);
        String valorA = String.valueOf(System.nanoTime() % 1000);
        String valorB = String.valueOf((System.nanoTime() + 1) % 1000);
        ReglaScoring reglaA = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.MAYOR, valorA, 15, TipoVariable.NUMERICO, true));
        ReglaScoring reglaB = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.MAYOR, valorB, 15, TipoVariable.NUMERICO, true));

        boolean editarReglaBHaciaLaCombinacionDeReglaA = reglaScoringRepositoryAdapter.existeCombinacion(
                idRiesgo, OperadorScoring.MAYOR, valorA, reglaB.getIdRegla());

        assertThat(editarReglaBHaciaLaCombinacionDeReglaA).isTrue();
    }

    @Test
    void laBaseDeDatosDebeRechazarUnUpdateQueDuplicaOtraReglaExistente() throws SQLException {
        // Defensa en profundidad: aunque la aplicación ya excluye la propia regla en
        // existeCombinacion, la restricción UNIQUE de la base de datos debe seguir
        // rechazando un UPDATE que produzca la misma combinación de otra fila.
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NUMERO_MORAS);
        String valorA = String.valueOf(System.nanoTime() % 1000);
        String valorB = String.valueOf((System.nanoTime() + 1) % 1000);
        reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.IGUAL, valorA, 5, TipoVariable.NUMERICO, false));
        ReglaScoring reglaB = reglaScoringRepositoryAdapter.guardar(
                ReglaScoring.nueva(idRiesgo, OperadorScoring.IGUAL, valorB, 5, TipoVariable.NUMERICO, false));

        ReglaScoring reglaBEditadaHaciaValorA = reglaB.editar(OperadorScoring.IGUAL, valorA, 5,
                TipoVariable.NUMERICO, false);

        assertThatThrownBy(() -> reglaScoringRepositoryAdapter.guardar(reglaBEditadaHaciaValorA))
                .isInstanceOf(ReglaScoringDuplicadaException.class);
    }

    @Test
    void laBaseDeDatosDebeRechazarUnOperadorFueraDelConjuntoPermitidoAunSaltandoLaValidacionDeDominio() throws SQLException {
        // El dominio (OperadorScoring) jamás permitiría un símbolo fuera del enum: se inserta
        // directamente vía SQL nativo para comprobar que la restricción definitiva
        // (ck_regla_scoring_operador) vive en la base de datos, no solo en la aplicación.
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.HISTORIAL_CREDITICIO);

        try (Connection connection = dataSource.getConnection()) {
            assertThatThrownBy(() -> ejecutarInsertReglaNativo(connection, idRiesgo, "!=", "BUENO", 10))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void laBaseDeDatosDebeRechazarUnPuntajeFueraDelRangoPermitidoAunSaltandoLaValidacionDeDominio() throws SQLException {
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);

        try (Connection connection = dataSource.getConnection()) {
            assertThatThrownBy(() -> ejecutarInsertReglaNativo(connection, idRiesgo, ">=", "1000000", 101))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void laBaseDeDatosDebeRechazarUnIdRiesgoInexistentePorLaClaveForanea() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThatThrownBy(() -> ejecutarInsertReglaNativo(connection, -1L, "=", "BUENO", 10))
                    .isInstanceOf(SQLException.class);
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

    private void ejecutarInsertReglaNativo(Connection connection, Long idRiesgo, String operador,
            String valorCondicion, int puntaje) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, idRiesgo);
            statement.setString(2, operador);
            statement.setString(3, valorCondicion);
            statement.setInt(4, puntaje);
            statement.executeUpdate();
        }
    }
}
