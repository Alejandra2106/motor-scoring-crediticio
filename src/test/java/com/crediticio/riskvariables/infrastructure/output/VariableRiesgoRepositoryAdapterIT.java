package com.crediticio.riskvariables.infrastructure.output;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import com.crediticio.riskvariables.domain.VariableRiesgoNoEncontradaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;












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

    @BeforeEach
    void limpiarEstadoPrevioDeVariablesDePrueba() throws SQLException {
        limpiarVariablesDePrueba();
    }

    @AfterEach
    void limpiarEstadoPosteriorDeVariablesDePrueba() throws SQLException {
        limpiarVariablesDePrueba();
    }

    private String[] valoresUtilizadosPorLaClase() {
        return new String[] {
                NombreVariableRiesgo.INGRESOS_MENSUALES.name(),
                NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO.name(),
                NombreVariableRiesgo.NUMERO_MORAS.name(),
                NombreVariableRiesgo.HISTORIAL_CREDITICIO.name(),
                NombreVariableRiesgo.ANTIGUEDAD_LABORAL.name()
        };
    }

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
        
        
        
        assertThatThrownBy(() -> ejecutarInsertNativo(
                "INGRESOS_MENSUALES_INVALIDA", "Descripción válida con más de diez caracteres", "ACTIVA"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void laBaseDeDatosDebeRechazarUnEstadoFueraDelConjuntoPermitidoAunSaltandoLaValidacionDeDominio() {
        
        
        assertThatThrownBy(() -> ejecutarInsertNativo(
                "NIVEL_ENDEUDAMIENTO", "Descripción válida con más de diez caracteres", "SUSPENDIDA"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void laBaseDeDatosDebePermitirElEstadoInactivaDesdeHu04() throws SQLException {
        VariableRiesgo guardada = variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.HISTORIAL_CREDITICIO));

        
        
        
        try {
            assertThatCode(() -> ejecutarInsertNativo(
                    "ANTIGUEDAD_LABORAL", "Descripción válida con más de diez caracteres", "INACTIVA"))
                    .doesNotThrowAnyException();

            Optional<VariableRiesgoJpaEntity> leida = variableRiesgoJpaRepository.findById(guardada.getIdVariableRiesgo());
            assertThat(leida).isPresent();
        } finally {
            eliminarPorVariableNativo("ANTIGUEDAD_LABORAL");
        }
    }

    @Test
    void debeBuscarPorIdUnaVariableExistenteYRetornarVacioSiNoExiste() {
        VariableRiesgo guardada = variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.INGRESOS_MENSUALES));

        Optional<VariableRiesgo> encontrada = variableRiesgoRepositoryAdapter.buscarPorId(guardada.getIdVariableRiesgo());
        Optional<VariableRiesgo> noEncontrada = variableRiesgoRepositoryAdapter.buscarPorId(-1L);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(noEncontrada).isEmpty();
    }

    @Test
    void debeCambiarEstadoDeActivaAInactivaYPersistirFechaModificacionPreservandoLosDemasCampos() {
        VariableRiesgo guardada = variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO));
        assertThat(guardada.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);

        VariableRiesgo actualizada = variableRiesgoRepositoryAdapter.guardar(
                guardada.cambiarEstado(EstadoVariableRiesgo.INACTIVA));

        assertThat(actualizada.getEstado()).isEqualTo(EstadoVariableRiesgo.INACTIVA);
        assertThat(actualizada.getIdVariableRiesgo()).isEqualTo(guardada.getIdVariableRiesgo());
        assertThat(actualizada.getVariable()).isEqualTo(guardada.getVariable());
        assertThat(actualizada.getDescripcion()).isEqualTo(guardada.getDescripcion());
        assertThat(actualizada.getFechaCreacion()).isEqualTo(guardada.getFechaCreacion());

        VariableRiesgoJpaEntity entityLeida = variableRiesgoJpaRepository.findById(guardada.getIdVariableRiesgo())
                .orElseThrow();
        assertThat(entityLeida.getEstado()).isEqualTo(EstadoVariableRiesgo.INACTIVA);
        assertThat(entityLeida.getFechaModificacion()).isNotNull();
    }

    @Test
    void debeReactivarUnaVariablePreviamenteInactivada() {
        VariableRiesgo guardada = variableRiesgoRepositoryAdapter.guardar(
                variableRiesgoNueva(NombreVariableRiesgo.HISTORIAL_CREDITICIO));
        VariableRiesgo inactivada = variableRiesgoRepositoryAdapter.guardar(
                guardada.cambiarEstado(EstadoVariableRiesgo.INACTIVA));

        VariableRiesgo reactivada = variableRiesgoRepositoryAdapter.guardar(
                inactivada.cambiarEstado(EstadoVariableRiesgo.ACTIVA));

        assertThat(reactivada.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(reactivada.getIdVariableRiesgo()).isEqualTo(guardada.getIdVariableRiesgo());
    }

    @Test
    void debeLanzarVariableRiesgoNoEncontradaAlCambiarEstadoDeUnaVariableInexistente() {
        VariableRiesgo inexistente = VariableRiesgo.reconstruir(
                -1L, NombreVariableRiesgo.NUMERO_MORAS, "Descripción válida con más de diez caracteres",
                EstadoVariableRiesgo.INACTIVA, null);

        assertThatThrownBy(() -> variableRiesgoRepositoryAdapter.guardar(inexistente))
                .isInstanceOf(VariableRiesgoNoEncontradaException.class);
    }

    private void limpiarVariablesDePrueba() throws SQLException {
        for (String variable : valoresUtilizadosPorLaClase()) {
            limpiarVariableDePrueba(variable);
        }
    }

    private void limpiarVariableDePrueba(String variable) throws SQLException {
        List<Long> idsRiesgo = obtenerIdsRiesgoPorVariable(variable);
        List<Long> idsRiesgoConReglasDependientes = obtenerIdsRiesgoConReglasDependientes(idsRiesgo);

        if (!idsRiesgoConReglasDependientes.isEmpty()) {
            throw new IllegalStateException(
                    "No es seguro limpiar la variable de prueba '" + variable + "' porque existen filas de regla_scoring " +
                            "dependientes de los ids_riesgo " + idsRiesgoConReglasDependientes + ". " +
                            "Este IT no registra un identificador de creación para distinguir qué regla_scoring pertenece a la prueba " +
                            "frente a otros datos reales o de otras pruebas.");
        }

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM riesgo WHERE variable = ?")) {
            statement.setString(1, variable);
            statement.executeUpdate();
        }

        assertThat(contarFilasPorVariable(variable)).isZero();
    }

    private List<Long> obtenerIdsRiesgoPorVariable(String variable) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id_riesgo FROM riesgo WHERE variable = ?")) {
            statement.setString(1, variable);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_riesgo"));
                }
            }
        }
        return ids;
    }

    private List<Long> obtenerIdsRiesgoConReglasDependientes(List<Long> idsRiesgo) throws SQLException {
        if (idsRiesgo.isEmpty()) {
            return List.of();
        }

        List<Long> idsConReglas = new ArrayList<>();
        String placeholders = String.join(", ", java.util.Collections.nCopies(idsRiesgo.size(), "?"));

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT DISTINCT id_riesgo FROM regla_scoring WHERE id_riesgo IN (" + placeholders + ")")) {
            for (int i = 0; i < idsRiesgo.size(); i++) {
                statement.setLong(i + 1, idsRiesgo.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    idsConReglas.add(resultSet.getLong("id_riesgo"));
                }
            }
        }
        return idsConReglas;
    }

    private int contarFilasPorVariable(String variable) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM riesgo WHERE variable = ?")) {
            statement.setString(1, variable);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private void eliminarPorVariableNativo(String variable) throws SQLException {
        limpiarVariableDePrueba(variable);
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
