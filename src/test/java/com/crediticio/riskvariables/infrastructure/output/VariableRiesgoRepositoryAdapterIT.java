package com.crediticio.riskvariables.infrastructure.output;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
 * V2__create_riesgo_table.sql contra esa base al iniciar el contexto.
 *
 * Al terminar en "IT" (no "Test"), Surefire no la ejecuta con `./mvnw test`.
 * Ejecución manual: ./mvnw test -Dtest=VariableRiesgoRepositoryAdapterIT -Dspring.profiles.active=it
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@Import(VariableRiesgoRepositoryAdapter.class)
class VariableRiesgoRepositoryAdapterIT {

    @Autowired
    private VariableRiesgoRepositoryAdapter variableRiesgoRepositoryAdapter;

    @Autowired
    private VariableRiesgoJpaRepository variableRiesgoJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void elContextoCargaConFlywayYLaValidacionDeEsquemaDeHibernate() {
        assertThat(variableRiesgoRepositoryAdapter).isNotNull();
        assertThat(variableRiesgoJpaRepository).isNotNull();
    }

    @Test
    void debeGuardarLaVariableYGenerarIdYFechaDeCreacion() {
        VariableRiesgo guardada = variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.INGRESOS_MENSUALES));

        assertThat(guardada.getIdVariableRiesgo()).isNotNull();
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
    }

    @Test
    void debePersistirYPermitirLeerTodosLosCamposDeLaVariable() {
        VariableRiesgo guardada = variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO));

        Optional<VariableRiesgoJpaEntity> leida = variableRiesgoJpaRepository.findById(guardada.getIdVariableRiesgo());

        assertThat(leida).isPresent();
        VariableRiesgoJpaEntity entity = leida.get();
        assertThat(entity.getVariable()).isEqualTo(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO);
        assertThat(entity.getDescripcion()).isEqualTo("Nivel de endeudamiento total del solicitante evaluado");
        assertThat(entity.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(entity.getFechaCreacion()).isNotNull();
    }

    @Test
    void debeLanzarVariableRiesgoDuplicadaAlGuardarUnaVariableYaExistente() {
        variableRiesgoRepositoryAdapter.guardar(variableRiesgoNueva(NombreVariableRiesgo.NUMERO_MORAS));

        assertThatThrownBy(() -> variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.NUMERO_MORAS)))
                .isInstanceOf(VariableRiesgoDuplicadaException.class);
    }

    @Test
    void debeExistePorVariableDetectarUnaVariableYaRegistrada() {
        variableRiesgoRepositoryAdapter.guardar(variableRiesgoNueva(NombreVariableRiesgo.ANTIGUEDAD_LABORAL));

        assertThat(variableRiesgoRepositoryAdapter.existePorVariable(NombreVariableRiesgo.ANTIGUEDAD_LABORAL)).isTrue();
        assertThat(variableRiesgoRepositoryAdapter.existePorVariable(NombreVariableRiesgo.HISTORIAL_CREDITICIO)).isFalse();
    }

    @Test
    void laBaseDeDatosDebeRechazarUnaVariableFueraDelConjuntoPermitidoAunSaltandoLaValidacionDeDominio() {
        // El dominio (NombreVariableRiesgo) jamás permitiría un valor fuera del enum: se
        // inserta directamente vía SQL nativo para comprobar que la restricción de integridad
        // definitiva (ck_riesgo_variable) vive en la base de datos, no solo en la aplicación.
        assertThatThrownBy(() -> ejecutarInsertNativo(
                "INGRESOS_MENSUALES_INVALIDA", "Descripción válida con más de diez caracteres", "ACTIVA"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void laBaseDeDatosDebeRechazarUnEstadoDiferenteDeActivaAunSaltandoLaValidacionDeDominio() {
        // El dominio (EstadoVariableRiesgo) solo conoce ACTIVA en HU03: se comprueba aquí que
        // la restricción de integridad definitiva (ck_riesgo_estado) vive en la base de datos.
        assertThatThrownBy(() -> ejecutarInsertNativo(
                "NIVEL_ENDEUDAMIENTO", "Descripción válida con más de diez caracteres", "INACTIVA"))
                .isInstanceOf(SQLException.class);
    }

    private void ejecutarInsertNativo(String variable, String descripcion, String estado) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO riesgo (variable, descripcion, estado) VALUES (?, ?, ?)")) {
            statement.setString(1, variable);
            statement.setString(2, descripcion);
            statement.setString(3, estado);
            statement.executeUpdate();
        }
    }

    private VariableRiesgo variableRiesgoNueva(NombreVariableRiesgo variable) {
        String descripcion = switch (variable) {
            case INGRESOS_MENSUALES -> "Ingresos mensuales netos declarados por el solicitante";
            case NIVEL_ENDEUDAMIENTO -> "Nivel de endeudamiento total del solicitante evaluado";
            case NUMERO_MORAS -> "Número de moras registradas en el historial crediticio";
            case HISTORIAL_CREDITICIO -> "Historial crediticio consolidado del solicitante evaluado";
            case ANTIGUEDAD_LABORAL -> "Antigüedad laboral acumulada en el empleo actual";
        };
        return VariableRiesgo.nueva(variable, descripcion);
    }
}
