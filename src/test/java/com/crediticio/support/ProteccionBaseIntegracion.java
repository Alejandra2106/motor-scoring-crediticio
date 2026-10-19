package com.crediticio.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Protección de las pruebas de integración de persistencia (@DataJpaTest con el perfil "it"). Spring
 * la invoca antes del refresh del contexto, es decir, antes de crear el DataSource, Flyway o
 * Hibernate. Todas las comprobaciones son estáticas, sobre la configuración resuelta: no abre
 * ninguna conexión, no crea bases ni esquemas y no escribe datos. Si algo no cuadra, lanza una
 * excepción y el contexto no llega a crearse, de modo que ni Spring ni Flyway se conectan.
 *
 * Comprobaciones:
 * 1. -Dit.base-dedicada=&lt;nombre&gt;, en minúsculas, terminado en "_it" y distinto de la base
 *    principal "motor_scoring".
 * 2. spring.datasource.url indicada como propiedad de la JVM (-D), nunca tomada de DB_URL ni de
 *    archivos de configuración, y efectivamente usada por Spring (ninguna otra fuente la sustituye).
 * 3. La URL tiene la forma jdbc:postgresql://host[:puerto]/base[?currentSchema=esquema], con un solo
 *    host y sin otros parámetros, y su base es exactamente la autorizada.
 * 4. Ninguna conexión alternativa para Flyway (spring.flyway.url / user / password).
 * 5. Esquema dedicado (-Dit.esquema-dedicado): si se indica, termina en "_it" (nunca "public"), la
 *    URL fija únicamente currentSchema con él y spring.flyway.schemas es ese mismo esquema. Si
 *    spring.flyway.locations incluye migraciones propuestas (database/propuestas), es obligatorio.
 *
 * Los mensajes no incluyen URL, usuarios ni contraseñas.
 */
public class ProteccionBaseIntegracion implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String PROPIEDAD_BASE_DEDICADA = "it.base-dedicada";
    public static final String PROPIEDAD_ESQUEMA_DEDICADO = "it.esquema-dedicado";
    private static final String BASE_PRINCIPAL = "motor_scoring";
    private static final String SUFIJO_DEDICADO = "_it";
    private static final String FUENTE_PROPIEDADES_JVM = "systemProperties";
    private static final String UBICACION_MIGRACIONES_PROPUESTAS = "database/propuestas";
    private static final List<String> CONEXIONES_ALTERNATIVAS_DE_FLYWAY = List.of(
            "spring.flyway.url", "spring.flyway.user", "spring.flyway.password");
    private static final Set<String> PARAMETROS_URL_ADMITIDOS = Set.of("currentSchema");
    /** Un único host (sin comas), puerto opcional, nombre de base y parámetros opcionales. */
    private static final Pattern URL_POSTGRESQL =
            Pattern.compile("^jdbc:postgresql://([^/,?]+)/([A-Za-z0-9_]+)(\\?(.*))?$");
    private static final Pattern NOMBRE_DEDICADO = Pattern.compile("^[a-z][a-z0-9_]*_it$");

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String baseDedicada = exigirBaseDedicada(environment);
        String url = exigirUrlExplicita(environment);
        String parametros = verificarUrl(url, baseDedicada);
        rechazarConexionesAlternativas(environment);
        verificarEsquemaDedicado(environment, parametros);
    }

    private static String exigirBaseDedicada(ConfigurableEnvironment environment) {
        String base = propiedadJvm(environment, PROPIEDAD_BASE_DEDICADA);
        if (base == null || !NOMBRE_DEDICADO.matcher(base).matches() || BASE_PRINCIPAL.equals(base)) {
            throw new IllegalStateException("Debe indicarse la base dedicada autorizada con -D"
                    + PROPIEDAD_BASE_DEDICADA + "=<nombre en minúsculas terminado en '" + SUFIJO_DEDICADO + "'>");
        }
        return base;
    }

    private static String exigirUrlExplicita(ConfigurableEnvironment environment) {
        String url = propiedadJvm(environment, "spring.datasource.url");
        if (url == null) {
            throw new IllegalStateException("spring.datasource.url debe indicarse explícitamente con -D; las pruebas "
                    + "de integración no usan DB_URL");
        }
        if (!url.equals(environment.getProperty("spring.datasource.url"))) {
            throw new IllegalStateException("Otra fuente de configuración sustituye a spring.datasource.url");
        }
        return url;
    }

    /** Devuelve los parámetros de la URL (o cadena vacía) tras validar su forma y su base. */
    private static String verificarUrl(String url, String baseDedicada) {
        Matcher matcher = URL_POSTGRESQL.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalStateException("spring.datasource.url debe tener la forma "
                    + "jdbc:postgresql://host[:puerto]/base[?currentSchema=esquema], con un solo host");
        }
        String baseDeLaUrl = matcher.group(2);
        if (!baseDedicada.equals(baseDeLaUrl)) {
            throw new IllegalStateException("La URL apunta a la base '" + baseDeLaUrl
                    + "', distinta de la base dedicada autorizada '" + baseDedicada + "'");
        }
        String parametros = matcher.group(4) == null ? "" : matcher.group(4);
        for (String parametro : parametros.isEmpty() ? new String[0] : parametros.split("&")) {
            String nombre = parametro.split("=", 2)[0];
            if (!PARAMETROS_URL_ADMITIDOS.contains(nombre)) {
                throw new IllegalStateException("La URL contiene el parámetro '" + nombre
                        + "', no admitido en las pruebas de integración");
            }
        }
        return parametros;
    }

    private static void rechazarConexionesAlternativas(ConfigurableEnvironment environment) {
        for (String propiedad : CONEXIONES_ALTERNATIVAS_DE_FLYWAY) {
            if (environment.containsProperty(propiedad)) {
                throw new IllegalStateException("La propiedad " + propiedad + " definiría una conexión distinta "
                        + "de spring.datasource.url; no se admite en las pruebas de integración");
            }
        }
    }

    private static void verificarEsquemaDedicado(ConfigurableEnvironment environment, String parametrosUrl) {
        String esquema = propiedadJvm(environment, PROPIEDAD_ESQUEMA_DEDICADO);
        String ubicaciones = environment.getProperty("spring.flyway.locations", "");
        if (esquema == null) {
            if (ubicaciones.contains(UBICACION_MIGRACIONES_PROPUESTAS)) {
                throw new IllegalStateException("Las migraciones propuestas solo pueden aplicarse en un esquema "
                        + "dedicado (-D" + PROPIEDAD_ESQUEMA_DEDICADO + ")");
            }
            return;
        }
        if (!NOMBRE_DEDICADO.matcher(esquema).matches()) {
            throw new IllegalStateException("El esquema dedicado debe estar en minúsculas y terminar en '"
                    + SUFIJO_DEDICADO + "'");
        }
        if (!parametrosUrl.equals("currentSchema=" + esquema)) {
            throw new IllegalStateException("spring.datasource.url debe fijar únicamente currentSchema=" + esquema);
        }
        if (!esquema.equals(environment.getProperty("spring.flyway.schemas"))) {
            throw new IllegalStateException("spring.flyway.schemas debe ser exactamente " + esquema);
        }
    }

    private static String propiedadJvm(ConfigurableEnvironment environment, String nombre) {
        PropertySource<?> fuente = environment.getPropertySources().get(FUENTE_PROPIEDADES_JVM);
        Object valor = fuente == null ? null : fuente.getProperty(nombre);
        return valor == null ? null : valor.toString();
    }
}
