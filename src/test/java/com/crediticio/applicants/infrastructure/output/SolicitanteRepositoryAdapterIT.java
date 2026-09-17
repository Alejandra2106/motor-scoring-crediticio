package com.crediticio.applicants.infrastructure.output;

import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.applicants.domain.HistorialCrediticio;
import com.crediticio.applicants.domain.Solicitante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración de persistencia contra PostgreSQL local real (no H2).
 *
 * Requiere que PostgreSQL local esté disponible y que las variables de entorno
 * DB_URL, DB_USERNAME y DB_PASSWORD apunten a una base de datos accesible, usando
 * el perfil "it" (src/test/resources/application-it.properties). Flyway aplicará
 * V1__create_solicitante_table.sql contra esa base al iniciar el contexto.
 *
 * Al terminar en "IT" (no "Test"), Surefire no la ejecuta con `./mvnw test`.
 * Ejecución manual: ./mvnw test -Dtest=SolicitanteRepositoryAdapterIT -Dspring.profiles.active=it
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@Import(SolicitanteRepositoryAdapter.class)
class SolicitanteRepositoryAdapterIT {

    @Autowired
    private SolicitanteRepositoryAdapter solicitanteRepositoryAdapter;

    @Autowired
    private SolicitanteJpaRepository solicitanteJpaRepository;

    @Test
    void elContextoCargaConFlywayYLaValidacionDeEsquemaDeHibernate() {
        assertThat(solicitanteRepositoryAdapter).isNotNull();
        assertThat(solicitanteJpaRepository).isNotNull();
    }

    @Test
    void debeGuardarElSolicitanteYGenerarIdYFechaDeRegistro() {
        Solicitante guardado = solicitanteRepositoryAdapter.guardar(solicitanteNuevo("6001001"));

        assertThat(guardado.getIdSolicitante()).isNotNull();
        assertThat(guardado.getFechaRegistro()).isNotNull();
    }

    @Test
    void debePersistirYPermitirLeerTodosLosCamposDelSolicitante() {
        Solicitante guardado = solicitanteRepositoryAdapter.guardar(solicitanteNuevo("6001002"));

        Optional<SolicitanteJpaEntity> leido = solicitanteJpaRepository.findById(guardado.getIdSolicitante());

        assertThat(leido).isPresent();
        SolicitanteJpaEntity entity = leido.get();
        assertThat(entity.getNombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(entity.getNumeroDocumento()).isEqualTo("6001002");
        assertThat(entity.getIngresosMensuales()).isEqualByComparingTo(new BigDecimal("3000000"));
        assertThat(entity.getDeudasMensuales()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(entity.getNumeroMoras()).isZero();
        assertThat(entity.getHistorialCrediticio()).isEqualTo(HistorialCrediticio.BUENO);
        assertThat(entity.getAntiguedadLaboral()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(entity.getFechaRegistro()).isNotNull();
    }

    @Test
    void debeLanzarDocumentoDuplicadoAlGuardarUnNumeroDeDocumentoYaExistente() {
        solicitanteRepositoryAdapter.guardar(solicitanteNuevo("6001003"));

        assertThatThrownBy(() -> solicitanteRepositoryAdapter.guardar(solicitanteNuevo("6001003")))
                .isInstanceOf(DocumentoDuplicadoException.class);
    }

    @Test
    void debeExistePorNumeroDocumentoDetectarUnDocumentoYaRegistrado() {
        solicitanteRepositoryAdapter.guardar(solicitanteNuevo("6001004"));

        assertThat(solicitanteRepositoryAdapter.existePorNumeroDocumento("6001004")).isTrue();
        assertThat(solicitanteRepositoryAdapter.existePorNumeroDocumento("6009999")).isFalse();
    }

    @Test
    void laBaseDeDatosDebeRechazarIngresosMensualesNegativosAunSaltandoLaValidacionDeDominio() {
        // El dominio (Solicitante) jamás permitiría este valor: se construye la entidad JPA
        // directamente para comprobar que la restricción de integridad definitiva vive en la
        // base de datos (CHECK ck_solicitante_ingresos_mensuales), no solo en la aplicación.
        SolicitanteJpaEntity entityConIngresosNegativos = new SolicitanteJpaEntity(
                "Ana María Pérez", "6001005", new BigDecimal("-1"), new BigDecimal("500000"), 0,
                HistorialCrediticio.BUENO, new BigDecimal("2.5"));

        assertThatThrownBy(() -> solicitanteJpaRepository.saveAndFlush(entityConIngresosNegativos))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Solicitante solicitanteNuevo(String numeroDocumento) {
        return Solicitante.nuevo(
                "Ana María Pérez",
                numeroDocumento,
                new BigDecimal("3000000"),
                new BigDecimal("500000"),
                0,
                HistorialCrediticio.BUENO,
                new BigDecimal("2.5"));
    }
}
