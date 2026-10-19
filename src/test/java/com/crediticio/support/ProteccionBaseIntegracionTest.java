package com.crediticio.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica la protección de las pruebas de integración sin ninguna base de datos: cada caso arma un
 * entorno en memoria y comprueba si el contexto podría iniciarse.
 */
class ProteccionBaseIntegracionTest {

    private static final String URL_DEDICADA = "jdbc:postgresql://localhost:5432/motor_scoring_it";

    private static GenericApplicationContext contexto(Map<String, Object> propiedadesJvm,
            Map<String, Object> otrasPropiedades) {
        GenericApplicationContext contexto = new GenericApplicationContext();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                new MapPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, propiedadesJvm));
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of()));
        environment.getPropertySources().addLast(new MapPropertySource("configuracion", otrasPropiedades));
        contexto.setEnvironment(environment);
        return contexto;
    }

    private static Map<String, Object> jvmValida() {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("it.base-dedicada", "motor_scoring_it");
        propiedades.put("spring.datasource.url", URL_DEDICADA);
        return propiedades;
    }

    private static void inicializar(Map<String, Object> jvm, Map<String, Object> otras) {
        new ProteccionBaseIntegracion().initialize(contexto(jvm, otras));
    }

    @Test
    void debeAdmitirLaBaseDedicadaIndicadaExplicitamente() {
        assertThatCode(() -> inicializar(jvmValida(), Map.of())).doesNotThrowAnyException();
    }

    @Test
    void debeAbortarSiLaUrlSoloVieneDeDbUrlODeUnArchivoDeConfiguracion() {
        Map<String, Object> jvm = jvmValida();
        jvm.remove("spring.datasource.url");

        assertThatThrownBy(() -> inicializar(jvm, Map.of("spring.datasource.url", URL_DEDICADA)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no usan DB_URL");
    }

    @Test
    void debeAbortarSiLaUrlApuntaALaBasePrincipal() {
        Map<String, Object> jvm = jvmValida();
        jvm.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/motor_scoring");

        assertThatThrownBy(() -> inicializar(jvm, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'motor_scoring'");
    }

    @Test
    void debeAbortarSiLaUrlApuntaAOtraBaseDedicada() {
        Map<String, Object> jvm = jvmValida();
        jvm.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/otra_base_it");

        assertThatThrownBy(() -> inicializar(jvm, Map.of())).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "motor_scoring", "MOTOR_SCORING_IT", "motor_scoring_it_copia"})
    void debeAbortarSinUnaBaseDedicadaValida(String base) {
        Map<String, Object> jvm = jvmValida();
        jvm.put("it.base-dedicada", base);

        assertThatThrownBy(() -> inicializar(jvm, Map.of())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void debeAbortarSinBaseDedicada() {
        Map<String, Object> jvm = jvmValida();
        jvm.remove("it.base-dedicada");

        assertThatThrownBy(() -> inicializar(jvm, Map.of())).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://host1:5432,host2:5432/motor_scoring_it",
            "jdbc:postgresql://localhost:5432/motor_scoring_it?PGDBNAME=motor_scoring",
            "jdbc:postgresql://localhost:5432/motor_scoring_it?currentSchema=public&options=x",
            "jdbc:postgresql:motor_scoring_it",
            "jdbc:h2:mem:motor_scoring_it"})
    void debeAbortarConUrlsAmbiguasOConParametrosNoAdmitidos(String url) {
        Map<String, Object> jvm = jvmValida();
        jvm.put("spring.datasource.url", url);

        assertThatThrownBy(() -> inicializar(jvm, Map.of())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void debeAbortarSiOtraFuenteSustituyeLaUrl() {
        GenericApplicationContext contexto = contexto(jvmValida(), Map.of());
        ((StandardEnvironment) contexto.getEnvironment()).getPropertySources().addFirst(new MapPropertySource(
                "prioritaria", Map.of("spring.datasource.url", "jdbc:postgresql://localhost:5432/motor_scoring")));

        assertThatThrownBy(() -> new ProteccionBaseIntegracion().initialize(contexto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void debeAbortarConUnaConexionAlternativaDeFlyway() {
        assertThatThrownBy(() -> inicializar(jvmValida(),
                Map.of("spring.flyway.url", "jdbc:postgresql://localhost:5432/motor_scoring")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.flyway.url");
    }

    @Test
    void lasMigracionesPropuestasExigenUnEsquemaDedicado() {
        assertThatThrownBy(() -> inicializar(jvmValida(),
                Map.of("spring.flyway.locations", "classpath:db/migration,filesystem:database/propuestas/hu12")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void debeAdmitirUnEsquemaDedicadoCoherenteConLaUrlYFlyway() {
        Map<String, Object> jvm = jvmValida();
        jvm.put("it.esquema-dedicado", "hu12_it");
        jvm.put("spring.datasource.url", URL_DEDICADA + "?currentSchema=hu12_it");

        assertThatCode(() -> inicializar(jvm, Map.of("spring.flyway.schemas", "hu12_it",
                "spring.flyway.locations", "classpath:db/migration,filesystem:database/propuestas/hu12")))
                .doesNotThrowAnyException();
    }

    @Test
    void debeAbortarSiElEsquemaDedicadoNoCoincideConLaUrlOConFlyway() {
        Map<String, Object> jvm = jvmValida();
        jvm.put("it.esquema-dedicado", "hu12_it");
        jvm.put("spring.datasource.url", URL_DEDICADA + "?currentSchema=public");
        assertThatThrownBy(() -> inicializar(jvm, Map.of("spring.flyway.schemas", "hu12_it")))
                .isInstanceOf(IllegalStateException.class);

        jvm.put("spring.datasource.url", URL_DEDICADA + "?currentSchema=hu12_it");
        assertThatThrownBy(() -> inicializar(jvm, Map.of("spring.flyway.schemas", "public")))
                .isInstanceOf(IllegalStateException.class);

        jvm.put("it.esquema-dedicado", "public");
        assertThatThrownBy(() -> inicializar(jvm, Map.of("spring.flyway.schemas", "public")))
                .isInstanceOf(IllegalStateException.class);
    }
}
