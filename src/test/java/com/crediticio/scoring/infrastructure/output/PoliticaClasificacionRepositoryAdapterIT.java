package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;
import com.crediticio.scoring.application.dto.RangoClasificacionRequest;
import com.crediticio.scoring.application.dto.RangoClasificacionResponse;
import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.scoring.domain.ConfiguracionClasificacionNoInicializadaException;
import com.crediticio.scoring.domain.DefinicionRango;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import com.crediticio.scoring.domain.PoliticaClasificacionInvalidaException;
import com.crediticio.scoring.domain.PoliticaVigenteNoDisponibleException;
import com.crediticio.scoring.domain.PublicacionPoliticaConcurrenteException;
import com.crediticio.scoring.ports.input.ConsultarPoliticaVigenteUseCase;
import com.crediticio.scoring.ports.input.PublicarPoliticaClasificacionUseCase;
import com.crediticio.scoring.ports.output.ActorActualPort;
import com.crediticio.support.ClavesRsaPrueba;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Prueba de integración de la publicación y la consulta de políticas de clasificación (HU11)
 * contra PostgreSQL real, con transacciones confirmadas de verdad (la clase no es transaccional).
 *
 * Solo debe ejecutarse contra una base PostgreSQL dedicada y desechable. La protección
 * ({@link ProteccionBaseDedicada}) es un ApplicationContextInitializer: Spring lo invoca antes
 * del refresh del contexto, es decir, antes de crear cualquier bean (incluidos el DataSource,
 * Flyway e Hibernate). En ese momento valida, con la configuración ya resuelta por Spring, que
 * la conexión configurada apunta exactamente a la base autorizada en la ejecución y aborta si
 * existe cualquier otra fuente de conexión. Ver {@link ProteccionBaseDedicada} para sus
 * garantías y limitaciones.
 *
 * Se ejecuta en un esquema dedicado y temporal, que no debe existir antes (la protección lo exige).
 * Las políticas son inmutables (HU11 CA11, también en la base con V9), así que no se borran fila a
 * fila: los cambios sobre la fila singleton de configuración se hacen dentro de transacciones que se
 * revierten, o se devuelve su valor inicial si una publicación de la propia prueba lo cambió, y al
 * terminar @AfterAll elimina el esquema completo solo si la base demuestra que lo creó esta ejecución.
 *
 * Al terminar en "IT", Surefire no la ejecuta con `./mvnw test`. Ejecución manual, aislada, desde
 * la raíz del proyecto y sin `clean`, indicando explícitamente la base y el esquema dedicados
 * (usuario y contraseña se toman de DB_USERNAME y DB_PASSWORD):
 * ./mvnw.cmd -B test "-Dtest=PoliticaClasificacionRepositoryAdapterIT" "-Dspring.profiles.active=it"
 *     "-Dhu11.it.base-dedicada=motor_scoring_it" "-Dhu11.it.esquema-dedicado=hu11_d2_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it?currentSchema=hu11_d2_it"
 *     "-Dspring.flyway.schemas=hu11_d2_it"
 * Si la URL efectiva no apunta exactamente a la base indicada en hu11.it.base-dedicada, o el
 * esquema no cumple las condiciones, la protección aborta antes de crear el contexto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "scoring.jwt.emisor=auth-api-prueba")
@ActiveProfiles("it")
@ContextConfiguration(initializers = PoliticaClasificacionRepositoryAdapterIT.ProteccionBaseDedicada.class)
class PoliticaClasificacionRepositoryAdapterIT {

    private static final String SUFIJO_BASE_DEDICADA = "_it";
    /** Plazo máximo para esperar una tarea concurrente (la publicación completa en otro hilo). */
    private static final int SEGUNDOS_MAXIMOS_DE_ESPERA = 30;
    /**
     * Plazo máximo de cada consulta de control de concurrencia (pg_stat_activity, pg_cancel_backend,
     * pg_backend_pid y el bloqueo de la fila singleton). Es independiente del plazo de la tarea:
     * limita una sola sentencia, no la publicación completa.
     */
    private static final int SEGUNDOS_LIMITE_DE_CONSULTA = 10;
    /**
     * Autor fijo y determinista de todas las publicaciones de esta prueba (HU11 CA14). Se inyecta
     * con @TestBean, sin depender del hilo: las pruebas de concurrencia publican desde otros hilos,
     * donde el contexto de seguridad (ligado al hilo) no existe.
     */
    private static final UUID AUTOR_PRUEBA = UUID.fromString("6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a4b");
    private static final String INSERTAR_POLITICA = "INSERT INTO politica_clasificacion "
            + "(version, fecha_creacion, fecha_publicacion, publicado_por) VALUES (?, ?, ?, ?)";
    private static final String INSERTAR_POLITICA_SIN_AUTOR =
            "INSERT INTO politica_clasificacion (version, fecha_creacion, fecha_publicacion) VALUES (?, ?, ?)";
    private static final String RESTRICCION_AUTOR = "ck_politica_clasificacion_publicado_por";
    private static final String INSERTAR_RANGO =
            "INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, limite_superior) VALUES (?, ?, ?, ?)";

    /**
     * HU10 exige una clave pública para validar los JWT al iniciar el contexto completo. Esta prueba no
     * usa tokens: se genera una clave efímera de prueba, en un directorio temporal creado aquí mismo.
     */
    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("scoring.jwt.clave-publica", () -> {
            Path claves;
            try {
                claves = Files.createTempDirectory("hu11-it-claves");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            claves.toFile().deleteOnExit();
            Path clave = ClavesRsaPrueba.escribirClavePublica(ClavesRsaPrueba.generar(), claves);
            clave.toFile().deleteOnExit();
            return "file:" + clave;
        });
    }

    @TestBean
    private ActorActualPort actorActualPort;

    static ActorActualPort actorActualPort() {
        return () -> Optional.of(AUTOR_PRUEBA);
    }

    @Autowired
    private PublicarPoliticaClasificacionUseCase publicarPoliticaClasificacionUseCase;

    @Autowired
    private ConsultarPoliticaVigenteUseCase consultarPoliticaVigenteUseCase;

    @Autowired
    private PoliticaClasificacionRepositoryAdapter politicaClasificacionRepositoryAdapter;

    @Autowired
    private PoliticaClasificacionJpaRepository politicaClasificacionJpaRepository;

    @MockitoSpyBean
    private RangoClasificacionJpaRepository rangoClasificacionJpaRepository;

    @MockitoSpyBean
    private ConfiguracionClasificacionActualJpaRepository configuracionClasificacionActualJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Environment environment;

    /** OID del esquema dedicado, registrado una vez con Flyway ya aplicado; null si no llegó a registrarse. */
    private static volatile Long oidEsquemaCreado;

    private final Set<Long> politicasCreadas = new LinkedHashSet<>();
    private final List<String> publicacionesSinTerminar = new ArrayList<>();
    private Long idPoliticaVigenteInicial;
    private boolean interrupcionPendiente;

    /**
     * Los errores de conexión se informan solo con su SQLState y tipo, y la identidad de la base
     * (que incluye el puerto) no se imprime si no coincide.
     */
    @BeforeEach
    void verificarBaseDedicadaYRegistrarPoliticaVigenteInicial() {
        assertThat(ProteccionBaseDedicada.baseVerificada)
                .as("La protección de base dedicada no se aplicó antes de iniciar el contexto")
                .isNotNull();
        ProteccionBaseDedicada.IdentidadBase identidadDeLasPruebas;
        try (Connection connection = dataSource.getConnection()) {
            identidadDeLasPruebas = ProteccionBaseDedicada.identificar(connection);
        } catch (SQLException ex) {
            throw new AssertionError("No fue posible verificar la base del DataSource de las pruebas ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + ")");
        }
        if (!ProteccionBaseDedicada.baseVerificada.equals(identidadDeLasPruebas)) {
            throw new AssertionError("El DataSource de las pruebas no apunta a la base verificada antes de "
                    + "iniciar el contexto");
        }
        try {
            idPoliticaVigenteInicial = idPoliticaVigente();
        } catch (SQLException ex) {
            throw new AssertionError("No fue posible leer la política vigente inicial ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + ")");
        }
        if (oidEsquemaCreado == null) {
            registrarEsquemaCreadoPorFlyway();
        }
    }

    /**
     * Una sola vez, con Flyway ya aplicado: exige que el historial del esquema dedicado tenga
     * exactamente una marca de creación de esquema (Flyway lo creó en esta ejecución) y registra su
     * OID, que @AfterAll exige antes de eliminarlo.
     */
    private void registrarEsquemaCreadoPorFlyway() {
        String esquema = ProteccionBaseDedicada.esquemaVerificado;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT n.oid, (SELECT count(*) FROM flyway_schema_history WHERE type = 'SCHEMA') "
                                + "FROM pg_namespace n WHERE n.nspname = ?")) {
            statement.setString(1, esquema);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("el esquema dedicado debe existir").isTrue();
                assertThat(resultSet.getLong(2)).as("marcas de creación de esquema de Flyway").isEqualTo(1);
                oidEsquemaCreado = resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new AssertionError("No fue posible registrar el esquema dedicado ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + ")");
        }
    }

    /**
     * Elimina el esquema dedicado completo solo si la base demuestra que lo creó esta ejecución:
     * dentro de una transacción bloquea su historial de Flyway, exige el mismo OID registrado tras
     * crearlo y exactamente una marca de creación de Flyway. En cualquier otro caso no elimina nada y
     * falla indicando el motivo. DROP SCHEMA no dispara los triggers por fila ni de TRUNCATE de V9.
     */
    @AfterAll
    static void eliminarElEsquemaDedicadoCreadoPorLaPrueba() {
        String esquema = ProteccionBaseDedicada.esquemaVerificado;
        if (esquema == null) {
            return; // la protección abortó antes de que se creara nada
        }
        String motivo;
        try (Connection connection = DriverManager.getConnection(ProteccionBaseDedicada.urlVerificada,
                ProteccionBaseDedicada.usuarioVerificado, ProteccionBaseDedicada.claveVerificada)) {
            connection.setAutoCommit(false);
            try {
                motivo = motivoParaNoEliminarElEsquema(connection, esquema);
                if (motivo == null) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("DROP SCHEMA " + esquema + " CASCADE");
                    }
                    connection.commit();
                    return;
                }
                connection.rollback();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException falloAlRevertir) {
                    ex.addSuppressed(falloAlRevertir);
                }
                throw ex;
            }
        } catch (SQLException ex) {
            motivo = "falló la comprobación o el DROP; transacción revertida ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + ")";
        }
        throw new AssertionError("No se eliminó el esquema " + esquema + ": " + motivo
                + ". Revíselo manualmente antes de volver a ejecutar la prueba.");
    }

    private static String motivoParaNoEliminarElEsquema(Connection connection, String esquema) throws SQLException {
        if (oidEsquemaCreado == null) {
            return "no se registró el OID del esquema tras su creación";
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + esquema + ".flyway_schema_history IN ACCESS EXCLUSIVE MODE");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT n.oid, (SELECT count(*) FROM " + esquema + ".flyway_schema_history WHERE type = 'SCHEMA') "
                        + "FROM pg_namespace n WHERE n.nspname = ?")) {
            statement.setString(1, esquema);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return "el esquema ya no existe";
                }
                if (resultSet.getLong(1) != oidEsquemaCreado) {
                    return "el esquema existente no es el que creó esta ejecución (OID distinto)";
                }
                if (resultSet.getLong(2) != 1) {
                    return "el historial de Flyway no contiene exactamente una marca de creación de esquema";
                }
            }
        }
        return null;
    }

    /**
     * Ninguna espera de la prueba deja el hilo interrumpido antes de este punto: la interrupción
     * se anota y se restaura al final, para que no interfiera con las operaciones de limpieza.
     *
     * Los fallos de limpieza y las publicaciones que no terminaron se reúnen en un único error,
     * para que ninguno oculte al otro: la prueba nunca termina con éxito si la limpieza no se
     * completó o si una publicación pudo confirmar datos después de terminar la prueba.
     */
    @AfterEach
    void eliminarSoloLosDatosCreadosPorLaPrueba() {
        List<String> fallos = new ArrayList<>();
        Throwable falloInesperado = null;
        try {
            limpiarDatosCreadosPorLaPrueba(fallos);
        } catch (SQLException ex) {
            fallos.add("La limpieza de los datos creados por la prueba no se completó ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + "); pueden quedar datos en la base dedicada");
        } catch (RuntimeException | Error ex) {
            falloInesperado = ex;
            fallos.add("La limpieza de los datos creados por la prueba no se completó ("
                    + describirFalloDeLimpieza(ex) + "); pueden quedar datos en la base dedicada");
        } finally {
            if (interrupcionPendiente) {
                Thread.currentThread().interrupt();
            }
        }
        if (!publicacionesSinTerminar.isEmpty()) {
            fallos.add("Publicaciones concurrentes que no terminaron antes de la limpieza y podrían confirmar datos "
                    + "en la base dedicada después de terminar la prueba: " + publicacionesSinTerminar);
        }
        if (!fallos.isEmpty()) {
            AssertionError error = new AssertionError(String.join(System.lineSeparator(), fallos));
            if (falloInesperado != null) {
                error.addSuppressed(falloInesperado);
            }
            throw error;
        }
    }

    /**
     * Solo incluye el mensaje de una IllegalStateException lanzada por esta misma clase de prueba
     * (por ejemplo, los rechazos de la conexión de limpieza), cuyos textos son fijos y no contienen
     * datos sensibles. Para cualquier otra excepción se muestra únicamente su tipo, porque su
     * mensaje podría incluir datos de conexión.
     */
    private static String describirFalloDeLimpieza(Throwable ex) {
        StackTraceElement[] traza = ex.getStackTrace();
        boolean lanzadaPorEstaPrueba = traza.length > 0
                && traza[0].getClassName().startsWith(PoliticaClasificacionRepositoryAdapterIT.class.getName());
        if (ex instanceof IllegalStateException && lanzadaPorEstaPrueba && ex.getMessage() != null) {
            return ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    /**
     * Restaura la política vigente inicial si una publicación de la prueba la cambió. Cada sentencia
     * tiene un tiempo límite, para que la limpieza no espere indefinidamente por un bloqueo que
     * mantenga una publicación pendiente. Si hay publicaciones pendientes, no se restaura la fila
     * singleton (podría estar bloqueada por ellas); ese hecho se añade a {@code fallos}. Las
     * políticas creadas no se borran (son inmutables, V9): quedan en el esquema temporal dedicado,
     * que {@code @AfterAll} elimina entero.
     *
     * La limpieza usa una conexión propia, abierta con DriverManager y verificada contra la base
     * protegida, que nunca pasa por el pool de Hikari: una conexión con una transacción en estado
     * incierto no puede volver al pool ni reutilizarse. Tampoco se llama a setAutoCommit(true),
     * porque la conexión se descarta al terminar. Lo que ocurra con una transacción abierta al
     * cerrar esa conexión no está verificado (JDBC lo deja a la implementación del driver), así que
     * nunca se presenta como reversión garantizada.
     *
     * El UPDATE que restaura la política vigente inicial se confirma (con confirmación automática)
     * antes de empezar a borrar las políticas de prueba. Es intencional: la clave foránea de la
     * configuración impide borrar la política que referencia, y así la configuración vigente nunca
     * queda apuntando a una política que se está eliminando. Si después falla un borrado, la
     * configuración ya apunta a la política inicial y las políticas de prueba restantes quedan sin
     * referenciar.
     *
     * La conexión se cierra explícitamente para distinguir, si falla el cierre, entre una limpieza
     * ya confirmada, una limpieza que falló antes de confirmar y una transacción en estado incierto.
     */
    private void limpiarDatosCreadosPorLaPrueba(List<String> fallos) throws SQLException {
        AtomicBoolean transaccionIncierta = new AtomicBoolean(false);
        Connection connection = abrirConexionPropiaDeLimpieza(fallos);
        boolean limpiezaConfirmada = false;
        try {
            Long idPoliticaVigenteActual = idPoliticaVigente(connection);
            boolean vigenteCreadaPorLaPrueba = idPoliticaVigenteActual != null
                    && politicasCreadas.contains(idPoliticaVigenteActual);
            boolean restaurarConfiguracion = vigenteCreadaPorLaPrueba && publicacionesSinTerminar.isEmpty();
            Long politicaConservada = null;
            if (vigenteCreadaPorLaPrueba && !restaurarConfiguracion) {
                politicaConservada = idPoliticaVigenteActual;
                fallos.add("Se omitió restaurar la política vigente inicial porque hay publicaciones concurrentes "
                        + "pendientes que podrían mantener bloqueada la fila de configuración; la política "
                        + politicaConservada + " creada por la prueba y sus rangos quedan como datos residuales en "
                        + "la base dedicada");
            }
            if (restaurarConfiguracion) {
                // la conexión propia empieza con confirmación automática (valor por defecto de JDBC)
                ejecutarConId(connection,
                        "UPDATE configuracion_clasificacion_actual SET id_politica = ? WHERE id_configuracion = 1",
                        idPoliticaVigenteInicial, SEGUNDOS_LIMITE_DE_CONSULTA);
            }
            // Las políticas creadas no se borran fila a fila: son inmutables (HU11 CA11, también en la
            // base con V9). Quedan en el esquema temporal dedicado, que @AfterAll elimina entero.
            limpiezaConfirmada = true;
        } finally {
            cerrarConexionDeLimpieza(connection, limpiezaConfirmada, transaccionIncierta.get(), fallos);
        }
    }

    /**
     * Cierra la conexión propia sin lanzar excepciones, para no sustituir un error de limpieza que
     * se esté propagando. El aviso depende del resultado previo: limpieza confirmada, limpieza que
     * falló antes de confirmar o transacción en estado incierto. Nunca afirma que el cierre haya
     * revertido una transacción.
     */
    private void cerrarConexionDeLimpieza(Connection connection, boolean limpiezaConfirmada,
            boolean transaccionIncierta, List<String> fallos) {
        String falloDeCierre = null;
        try {
            connection.close();
        } catch (SQLException | RuntimeException ex) {
            falloDeCierre = ex instanceof SQLException falloJdbc
                    ? ProteccionBaseDedicada.describirErrorJdbc(falloJdbc)
                    : ex.getClass().getSimpleName();
        }
        if (transaccionIncierta) {
            fallos.add((falloDeCierre == null
                    ? "La conexión propia de limpieza se cerró"
                    : "Falló el cierre de la conexión propia de limpieza (" + falloDeCierre + ")")
                    + " con una transacción cuya reversión falló; no está verificado que la transacción se haya "
                    + "revertido, por lo que el borrado parcial de esa política podría haber quedado aplicado");
        } else if (falloDeCierre != null && limpiezaConfirmada) {
            fallos.add("Falló el cierre de la conexión propia de limpieza (" + falloDeCierre + "); los borrados ya "
                    + "se habían confirmado antes del cierre");
        } else if (falloDeCierre != null) {
            fallos.add("Falló el cierre de la conexión propia de limpieza (" + falloDeCierre + ") después de un "
                    + "error de limpieza anterior a la confirmación; se conserva ese error");
        }
    }

    /**
     * Abre una conexión fuera del pool con las propiedades spring.datasource.* efectivas (las mismas
     * que validó {@link ProteccionBaseDedicada}) y exige que apunte a la base verificada antes de
     * permitir cualquier borrado. Los mensajes no incluyen la URL ni las credenciales.
     */
    private Connection abrirConexionPropiaDeLimpieza(List<String> fallos) throws SQLException {
        ProteccionBaseDedicada.IdentidadBase esperada = ProteccionBaseDedicada.baseVerificada;
        if (esperada == null) {
            throw new IllegalStateException("No hay una base verificada; no se borra ningún dato");
        }
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("spring.datasource.url debe estar definida y apuntar a PostgreSQL para "
                    + "limpiar; no se borra ningún dato");
        }
        Connection connection = DriverManager.getConnection(url,
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"));
        boolean verificada;
        try {
            verificada = esperada.equals(ProteccionBaseDedicada.identificar(connection));
        } catch (SQLException | RuntimeException ex) {
            // el cierre no lanza: el error de lectura de la identidad se conserva como error principal
            cerrarConexionDescartada(connection, fallos);
            throw ex;
        }
        if (!verificada) {
            cerrarConexionDescartada(connection, fallos);
            throw new IllegalStateException("La conexión propia de limpieza no apunta a la base verificada; no se "
                    + "borra ningún dato");
        }
        return connection;
    }

    /**
     * Cierra una conexión rechazada sin lanzar excepciones, para no sustituir el error de
     * validación o de lectura que se va a propagar. Un fallo de cierre se informa como aviso seguro.
     */
    private void cerrarConexionDescartada(Connection connection, List<String> fallos) {
        try {
            connection.close();
        } catch (SQLException | RuntimeException ex) {
            String detalle = ex instanceof SQLException falloJdbc
                    ? ProteccionBaseDedicada.describirErrorJdbc(falloJdbc)
                    : ex.getClass().getSimpleName();
            fallos.add("No fue posible cerrar la conexión de limpieza descartada (" + detalle + "); no puede "
                    + "confirmarse que se haya cerrado");
        }
    }

    private Long idPoliticaVigente(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(SEGUNDOS_LIMITE_DE_CONSULTA);
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT id_politica FROM configuracion_clasificacion_actual WHERE id_configuracion = 1")) {
                return resultSet.next() ? resultSet.getObject(1, Long.class) : null;
            }
        }
    }

    @Test
    void flywayDebeHaberAplicadoV6YLaFilaSingletonDeConfiguracionDebeExistir() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(consultarEntero(connection,
                    "SELECT count(*) FROM flyway_schema_history WHERE version = '6' AND success")).isEqualTo(1);
            assertThat(consultarEntero(connection,
                    "SELECT count(*) FROM configuracion_clasificacion_actual")).isEqualTo(1);
            assertThat(consultarEntero(connection,
                    "SELECT count(*) FROM configuracion_clasificacion_actual WHERE id_configuracion = 1")).isEqualTo(1);
        }
        assertThat(politicaClasificacionRepositoryAdapter).isNotNull();
    }

    /**
     * HU11 CA14: V7 aplicada, columna publicado_por UUID que admite nulos (como la entidad JPA, que
     * Hibernate ya validó con ddl-auto=validate al iniciar el contexto) y restricción NOT VALID.
     */
    @Test
    void laMigracionV7DebeCorresponderAlModeloJpaYLaRestriccionDelAutorDebeQuedarSinValidar() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThat(consultarEntero(connection,
                    "SELECT count(*) FROM flyway_schema_history WHERE version = '7' AND success")).isEqualTo(1);
            try (ResultSet columna = statement.executeQuery(
                    "SELECT data_type, is_nullable FROM information_schema.columns "
                            + "WHERE table_schema = current_schema() AND table_name = 'politica_clasificacion' "
                            + "AND column_name = 'publicado_por'")) {
                assertThat(columna.next()).as("columna publicado_por").isTrue();
                assertThat(columna.getString(1)).isEqualTo("uuid");
                assertThat(columna.getString(2)).isEqualTo("YES");
            }
            try (ResultSet restriccion = statement.executeQuery(
                    "SELECT contype, convalidated, pg_get_constraintdef(oid) FROM pg_constraint "
                            + "WHERE conrelid = 'politica_clasificacion'::regclass AND conname = '"
                            + RESTRICCION_AUTOR + "'")) {
                assertThat(restriccion.next()).as(RESTRICCION_AUTOR).isTrue();
                assertThat(restriccion.getString(1)).isEqualTo("c");
                assertThat(restriccion.getBoolean(2)).as("la restricción no debe validar filas históricas").isFalse();
                assertThat(restriccion.getString(3)).contains("publicado_por IS NOT NULL").contains("NOT VALID");
            }
        }
    }

    @Test
    void laPublicacionDebePersistirYRecuperarSuAutor() throws SQLException {
        PoliticaClasificacionResponse publicada = publicar(solicitudValida());

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT publicado_por FROM politica_clasificacion WHERE id_politica = ?")) {
            statement.setLong(1, publicada.getIdPolitica());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getObject(1, UUID.class)).isEqualTo(AUTOR_PRUEBA);
            }
        }
        assertThat(politicaClasificacionRepositoryAdapter.buscarVigente())
                .hasValueSatisfying(vigente -> {
                    assertThat(vigente.getIdPolitica()).isEqualTo(publicada.getIdPolitica());
                    assertThat(vigente.getPublicadoPor()).isEqualTo(AUTOR_PRUEBA);
                });
    }

    @Test
    void laBaseDeDatosDebeRechazarUnaPoliticaNuevaSinAutor() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERTAR_POLITICA_SIN_AUTOR)) {
                Timestamp instante = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
                statement.setInt(1, consultarEntero(connection,
                        "SELECT COALESCE(MAX(version), 0) + 1 FROM politica_clasificacion"));
                statement.setTimestamp(2, instante);
                statement.setTimestamp(3, instante);
                assertThatThrownBy(statement::executeUpdate)
                        .isInstanceOfSatisfying(SQLException.class, ex -> {
                            assertThat(ex.getSQLState()).isEqualTo("23514");
                            assertThat(ex.getMessage()).contains(RESTRICCION_AUTOR);
                        });
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }

    @Test
    void debePublicarLaPoliticaSusTresRangosYActualizarLaPoliticaVigente() throws SQLException {
        int versionAnterior = versionMaxima();

        PoliticaClasificacionResponse publicada = publicar(solicitudValida());

        assertThat(publicada.getVersion()).isEqualTo(versionAnterior + 1);
        assertThat(idPoliticaVigente()).isEqualTo(publicada.getIdPolitica());
        assertThat(contarRangos(publicada.getIdPolitica())).isEqualTo(3);
        assertThat(publicada.getFechaCreacion()).isEqualTo(publicada.getFechaPublicacion());
        assertThat(fechaCreacionPersistida(publicada.getIdPolitica())).isEqualTo(publicada.getFechaCreacion());
        assertThat(fechaPublicacionPersistida(publicada.getIdPolitica())).isEqualTo(publicada.getFechaPublicacion());
    }

    @Test
    void publicarUnaNuevaVersionDebeConservarIntactaLaVersionAnterior() throws SQLException {
        PoliticaClasificacionResponse primera = publicar(solicitudValida());
        String primeraAntes = describirPoliticaPersistida(primera.getIdPolitica());

        PoliticaClasificacionResponse segunda = publicar(otraSolicitudValida());

        assertThat(segunda.getVersion()).isEqualTo(primera.getVersion() + 1);
        assertThat(describirPoliticaPersistida(primera.getIdPolitica())).isEqualTo(primeraAntes);
        assertThat(idPoliticaVigente()).isEqualTo(segunda.getIdPolitica());
    }

    @Test
    void modificarEnMemoriaLasEntidadesPublicadasNoDebeGenerarActualizaciones() throws SQLException {
        PoliticaClasificacionResponse publicada = publicar(solicitudValida());
        String antes = describirPoliticaPersistida(publicada.getIdPolitica());
        Long idRango = publicada.getRangos().get(0).getIdRango();

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            PoliticaClasificacionJpaEntity politica = politicaClasificacionJpaRepository
                    .findById(publicada.getIdPolitica()).orElseThrow();
            ReflectionTestUtils.setField(politica, "numeroVersion", 999_999);
            ReflectionTestUtils.setField(politica, "fechaPublicacion", politica.getFechaPublicacion().plusDays(1));
            RangoClasificacionJpaEntity rango = rangoClasificacionJpaRepository.findById(idRango).orElseThrow();
            ReflectionTestUtils.setField(rango, "limiteSuperior", rango.getLimiteSuperior() - 1);
            politicaClasificacionJpaRepository.flush();
        });

        assertThat(describirPoliticaPersistida(publicada.getIdPolitica())).isEqualTo(antes);
    }

    @Test
    void siFallaElGuardadoDeLosRangosNoDebeQuedarNingunaParteDeLaPublicacion() throws Exception {
        int versionAnterior = versionMaxima();
        doThrow(new IllegalStateException("fallo simulado al guardar rangos"))
                .when(rangoClasificacionJpaRepository).saveAll(any());

        assertThatThrownBy(() -> publicarPoliticaClasificacionUseCase.publicar(solicitudValida()))
                .hasMessageContaining("fallo simulado al guardar rangos");

        assertThat(versionMaxima()).isEqualTo(versionAnterior);
        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
        verificarQueElBloqueoQuedoLiberado();
    }

    @Test
    void siFallaLaActualizacionDeLaConfiguracionNoDebeQuedarNingunaParteDeLaPublicacion() throws Exception {
        int versionAnterior = versionMaxima();
        doThrow(new IllegalStateException("fallo simulado al actualizar la configuración"))
                .when(configuracionClasificacionActualJpaRepository).saveAndFlush(any());

        assertThatThrownBy(() -> publicarPoliticaClasificacionUseCase.publicar(solicitudValida()))
                .hasMessageContaining("fallo simulado al actualizar la configuración");

        assertThat(versionMaxima()).isEqualTo(versionAnterior);
        assertThat(contarPoliticasConVersionMayorQue(versionAnterior)).isZero();
        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
        verificarQueElBloqueoQuedoLiberado();
    }

    @Test
    void siLaTransaccionSeRevierteDespuesDePublicarNoDebeQuedarNingunDato() throws SQLException {
        int versionAnterior = versionMaxima();

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            politicaClasificacionRepositoryAdapter.publicar(PoliticaClasificacion.nueva(
                    LocalDateTime.now().truncatedTo(ChronoUnit.MICROS), UUID.randomUUID(), definicionesValidas()));
            throw new IllegalStateException("reversión forzada después de publicar");
        })).hasMessage("reversión forzada después de publicar");

        assertThat(versionMaxima()).isEqualTo(versionAnterior);
        assertThat(contarPoliticasConVersionMayorQue(versionAnterior)).isZero();
        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
    }

    @Test
    void laPrimeraPublicacionConcurrenteDebeSerializarseMedianteElBloqueoDeLaConfiguracion() throws Exception {
        assumeTrue(idPoliticaVigenteInicial == null,
                "Solo aplica cuando la base dedicada todavía no tiene política vigente");

        verificarQueLasPublicacionesSeSerializan();
    }

    @Test
    void unaPublicacionConcurrenteSobreUnaPoliticaVigenteDebeSerializarse() throws Exception {
        publicar(solicitudValida());

        verificarQueLasPublicacionesSeSerializan();
    }

    @Test
    void unaVersionInsertadaSinBloqueoDebeHacerFallarLaPublicacionConcurrenteSinDejarRastro() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<PoliticaClasificacionResponse> publicacion = null;
        Integer pidPublicacion = null;
        try (Connection sesionExterna = dataSource.getConnection()) {
            sesionExterna.setAutoCommit(false);
            Throwable falloPrincipal = null;
            try {
                PoliticaInsertada politicaExterna = insertarPoliticaCompletaPorSql(sesionExterna, false);
                int pidSesionExterna = obtenerPid(sesionExterna);

                publicacion = executor.submit(() -> publicarPoliticaClasificacionUseCase.publicar(solicitudValida()));
                pidPublicacion = esperarSesionBloqueadaPor(pidSesionExterna);
                assertThat(publicacion.isDone()).isFalse();

                sesionExterna.commit();
                politicasCreadas.add(politicaExterna.idPolitica());

                assertThat(causaDelFallo(publicacion)).isInstanceOf(PublicacionPoliticaConcurrenteException.class);
                assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
                assertThat(contarPoliticasConVersion(politicaExterna.version())).isEqualTo(1);
            } catch (Throwable ex) {
                falloPrincipal = ex;
                throw ex;
            } finally {
                revertirSesionExternaYFinalizar(sesionExterna, publicacion, pidPublicacion, falloPrincipal);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void laBaseDeDatosDebeRechazarDatosQueIncumplenLasRestriccionesDeV6() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Timestamp instante = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
                Timestamp otroInstante = Timestamp.valueOf(instante.toLocalDateTime().plusSeconds(1));
                int version = consultarEntero(connection,
                        "SELECT COALESCE(MAX(version), 0) + 1 FROM politica_clasificacion");
                long idPolitica = insertarPolitica(connection, version, instante);
                insertarRango(connection, idPolitica, "BAJO", -100, 29);

                // con autor, para que cada rechazo se deba a la restricción de V6 que se prueba y no a la de V7
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_POLITICA, 0, instante, instante, AUTOR_PRUEBA);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_POLITICA, version, instante, instante,
                        AUTOR_PRUEBA);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_POLITICA, version + 1, instante, otroInstante,
                        AUTOR_PRUEBA);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_RANGO, idPolitica, "OTRO", 30, 79);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_RANGO, idPolitica, "MEDIO", -101, 79);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_RANGO, idPolitica, "MEDIO", 30, 131);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_RANGO, idPolitica, "MEDIO", 79, 30);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_RANGO, idPolitica, "BAJO", 30, 79);
                assertRechazadoPorLaBaseDeDatos(connection, INSERTAR_RANGO, -1L, "MEDIO", 30, 79);
                assertRechazadoPorLaBaseDeDatos(connection,
                        "INSERT INTO configuracion_clasificacion_actual (id_configuracion, id_politica) VALUES (2, NULL)");
                assertRechazadoPorLaBaseDeDatos(connection,
                        "INSERT INTO configuracion_clasificacion_actual (id_configuracion, id_politica) VALUES (1, NULL)");
                assertRechazadoPorLaBaseDeDatos(connection,
                        "UPDATE configuracion_clasificacion_actual SET id_politica = -1 WHERE id_configuracion = 1");
                assertRechazadoPorLaBaseDeDatos(connection,
                        "DELETE FROM politica_clasificacion WHERE id_politica = ?", idPolitica);

                long idPoliticaReferenciada = insertarPolitica(connection, version + 2, instante);
                ejecutarConId(connection,
                        "UPDATE configuracion_clasificacion_actual SET id_politica = ? WHERE id_configuracion = 1",
                        idPoliticaReferenciada);
                assertRechazadoPorLaBaseDeDatos(connection,
                        "DELETE FROM politica_clasificacion WHERE id_politica = ?", idPoliticaReferenciada);
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * La ausencia de la fila singleton se simula borrándola dentro de una transacción marcada
     * para revertirse desde su inicio, en la que también se ejecuta la publicación: el borrado
     * nunca se confirma (PostgreSQL lo revierte aunque la prueba se interrumpa) y ninguna otra
     * sesión lo ve. Mientras la transacción está abierta, la fila queda bloqueada.
     */
    @Test
    void sinLaFilaDeConfiguracionLaPublicacionDebeFallarSinRecrearla() throws SQLException {
        int versionAnterior = versionMaxima();

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            estado.setRollbackOnly();
            entityManager.createNativeQuery(
                    "DELETE FROM configuracion_clasificacion_actual WHERE id_configuracion = 1").executeUpdate();

            assertThatThrownBy(() -> publicarPoliticaClasificacionUseCase.publicar(solicitudValida()))
                    .isInstanceOf(ConfiguracionClasificacionNoInicializadaException.class);

            Number filasDentroDeLaTransaccion = (Number) entityManager.createNativeQuery(
                    "SELECT count(*) FROM configuracion_clasificacion_actual").getSingleResult();
            assertThat(filasDentroDeLaTransaccion.intValue()).isZero();
        });

        try (Connection connection = dataSource.getConnection()) {
            assertThat(consultarEntero(connection,
                    "SELECT count(*) FROM configuracion_clasificacion_actual WHERE id_configuracion = 1")).isEqualTo(1);
        }
        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
        assertThat(versionMaxima()).isEqualTo(versionAnterior);
    }

    @Test
    void unaPoliticaConUnaCategoriaDuplicadaDebeRechazarseSinPersistirNada() throws SQLException {
        verificarRechazoSinPersistencia(new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 29),
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, 30, 79),
                new RangoClasificacionRequest(CategoriaRiesgo.ALTO, 80, 130))),
                "La categoría BAJO está duplicada", "Falta el rango de la categoría MEDIO");
    }

    @Test
    void unaPoliticaConUnaCategoriaFaltanteDebeRechazarseSinPersistirNada() throws SQLException {
        verificarRechazoSinPersistencia(new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 29),
                new RangoClasificacionRequest(CategoriaRiesgo.MEDIO, 30, 130))),
                "Falta el rango de la categoría ALTO");
    }

    private void verificarRechazoSinPersistencia(PublicarPoliticaClasificacionRequest request,
            String... erroresEsperados) throws SQLException {
        int versionAnterior = versionMaxima();
        int politicasAntes;
        int rangosAntes;
        try (Connection connection = dataSource.getConnection()) {
            politicasAntes = consultarEntero(connection, "SELECT count(*) FROM politica_clasificacion");
            rangosAntes = consultarEntero(connection, "SELECT count(*) FROM rango_clasificacion");
        }

        assertThatThrownBy(() -> publicar(request))
                .isInstanceOfSatisfying(PoliticaClasificacionInvalidaException.class,
                        ex -> assertThat(ex.getErrores()).contains(erroresEsperados));

        assertThat(versionMaxima()).isEqualTo(versionAnterior);
        assertThat(contarPoliticasConVersionMayorQue(versionAnterior)).isZero();
        try (Connection connection = dataSource.getConnection()) {
            assertThat(consultarEntero(connection, "SELECT count(*) FROM politica_clasificacion"))
                    .isEqualTo(politicasAntes);
            assertThat(consultarEntero(connection, "SELECT count(*) FROM rango_clasificacion"))
                    .isEqualTo(rangosAntes);
        }
        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
    }

    @Test
    void laConsultaDebeRetornarLaPoliticaVigenteRecienPublicadaConSusRangos() throws SQLException {
        PoliticaClasificacionResponse publicada = publicar(solicitudValida());

        PoliticaClasificacionResponse vigente = consultarPoliticaVigenteUseCase.consultarVigente();

        assertThat(vigente.getIdPolitica()).isEqualTo(publicada.getIdPolitica()).isEqualTo(idPoliticaVigente());
        assertThat(vigente.getVersion()).isEqualTo(publicada.getVersion());
        assertThat(vigente.getFechaCreacion()).isEqualTo(fechaCreacionPersistida(publicada.getIdPolitica()));
        assertThat(vigente.getFechaPublicacion()).isEqualTo(fechaPublicacionPersistida(publicada.getIdPolitica()));
        assertThat(vigente.getRangos()).extracting(RangoClasificacionResponse::getIdRango)
                .containsExactlyElementsOf(publicada.getRangos().stream().map(RangoClasificacionResponse::getIdRango)
                        .toList());
        assertThat(vigente.getRangos()).extracting(RangoClasificacionResponse::getCategoria)
                .containsExactly(CategoriaRiesgo.BAJO, CategoriaRiesgo.MEDIO, CategoriaRiesgo.ALTO);
        assertThat(vigente.getRangos()).extracting(RangoClasificacionResponse::getLimiteInferior)
                .containsExactly(-100, 30, 80);
        assertThat(vigente.getRangos()).extracting(RangoClasificacionResponse::getLimiteSuperior)
                .containsExactly(29, 79, 130);
    }

    @Test
    void trasPublicarUnaNuevaVersionLaConsultaDebeRetornarlaYLaAnteriorDebeConservarse() throws SQLException {
        PoliticaClasificacionResponse primera = publicar(solicitudValida());
        String primeraAntes = describirPoliticaPersistida(primera.getIdPolitica());

        PoliticaClasificacionResponse segunda = publicar(otraSolicitudValida());
        PoliticaClasificacionResponse vigente = consultarPoliticaVigenteUseCase.consultarVigente();

        assertThat(vigente.getIdPolitica()).isEqualTo(segunda.getIdPolitica());
        assertThat(vigente.getVersion()).isEqualTo(primera.getVersion() + 1);
        assertThat(vigente.getRangos()).extracting(RangoClasificacionResponse::getLimiteSuperior)
                .containsExactly(49, 99, 130);
        assertThat(describirPoliticaPersistida(primera.getIdPolitica())).isEqualTo(primeraAntes);
    }

    /**
     * La ausencia de política vigente se simula dejando la configuración sin política dentro de
     * una transacción marcada para revertirse desde su inicio, en la que también se ejecuta la
     * consulta: el cambio nunca se confirma y ninguna otra sesión lo ve.
     */
    @Test
    void sinPoliticaVigenteLaConsultaDebeInformarExplicitamenteSuAusencia() throws SQLException {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            estado.setRollbackOnly();
            entityManager.createNativeQuery(
                    "UPDATE configuracion_clasificacion_actual SET id_politica = NULL WHERE id_configuracion = 1")
                    .executeUpdate();

            assertThatThrownBy(() -> consultarPoliticaVigenteUseCase.consultarVigente())
                    .isInstanceOf(PoliticaVigenteNoDisponibleException.class);
        });

        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
    }

    /** Igual que la prueba anterior, borrando la fila singleton en una transacción que se revierte. */
    @Test
    void sinLaFilaDeConfiguracionLaConsultaDebeFallarSinRecrearla() throws SQLException {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            estado.setRollbackOnly();
            entityManager.createNativeQuery(
                    "DELETE FROM configuracion_clasificacion_actual WHERE id_configuracion = 1").executeUpdate();

            assertThatThrownBy(() -> consultarPoliticaVigenteUseCase.consultarVigente())
                    .isInstanceOf(ConfiguracionClasificacionNoInicializadaException.class);
        });

        try (Connection connection = dataSource.getConnection()) {
            assertThat(consultarEntero(connection,
                    "SELECT count(*) FROM configuracion_clasificacion_actual WHERE id_configuracion = 1")).isEqualTo(1);
        }
        assertThat(idPoliticaVigente()).isEqualTo(idPoliticaVigenteInicial);
    }

    /**
     * La sesión externa toma el mismo bloqueo que la publicación; la publicación real se lanza
     * en otro hilo y solo se libera la sesión externa después de comprobar, en pg_stat_activity,
     * que exactamente una sesión está esperando precisamente ese bloqueo.
     */
    private void verificarQueLasPublicacionesSeSerializan() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<PoliticaClasificacionResponse> publicacion = null;
        Integer pidPublicacion = null;
        try (Connection sesionExterna = dataSource.getConnection()) {
            sesionExterna.setAutoCommit(false);
            Throwable falloPrincipal = null;
            try {
                bloquearConfiguracion(sesionExterna);
                int pidSesionExterna = obtenerPid(sesionExterna);

                publicacion = executor.submit(() -> publicarPoliticaClasificacionUseCase.publicar(solicitudValida()));
                pidPublicacion = esperarSesionBloqueadaPor(pidSesionExterna);
                assertThat(publicacion.isDone()).isFalse();

                PoliticaInsertada politicaExterna = insertarPoliticaCompletaPorSql(sesionExterna, true);
                sesionExterna.commit();
                politicasCreadas.add(politicaExterna.idPolitica());

                PoliticaClasificacionResponse publicada = esperarPublicacion(publicacion);
                politicasCreadas.add(publicada.getIdPolitica());

                assertThat(publicada.getVersion()).isEqualTo(politicaExterna.version() + 1);
                assertThat(idPoliticaVigente()).isEqualTo(publicada.getIdPolitica());
                assertThat(contarRangos(politicaExterna.idPolitica())).isEqualTo(3);
                assertThat(contarRangos(publicada.getIdPolitica())).isEqualTo(3);
            } catch (Throwable ex) {
                falloPrincipal = ex;
                throw ex;
            } finally {
                revertirSesionExternaYFinalizar(sesionExterna, publicacion, pidPublicacion, falloPrincipal);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Revierte la sesión externa y, aunque esa reversión falle, espera o registra siempre la
     * publicación concurrente. Si la reversión falla, se informa con un mensaje seguro: como fallo
     * de la prueba si no había otro, o como excepción suprimida del fallo principal si lo había,
     * para que ninguno de los dos se pierda.
     */
    private void revertirSesionExternaYFinalizar(Connection sesionExterna,
            Future<PoliticaClasificacionResponse> publicacion, Integer pidPublicacion, Throwable falloPrincipal) {
        AssertionError falloDeReversion = null;
        try {
            sesionExterna.rollback();
        } catch (SQLException ex) {
            falloDeReversion = new AssertionError("No fue posible revertir la sesión externa ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + ")");
        } finally {
            finalizarPublicacionPendiente(publicacion, pidPublicacion);
        }
        if (falloDeReversion != null) {
            if (falloPrincipal != null) {
                falloPrincipal.addSuppressed(falloDeReversion);
            } else {
                throw falloDeReversion;
            }
        }
    }

    private void verificarQueElBloqueoQuedoLiberado() throws Exception {
        reset(rangoClasificacionJpaRepository, configuracionClasificacionActualJpaRepository);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<PoliticaClasificacionResponse> publicacion = null;
        try {
            publicacion = executor.submit(() -> publicarPoliticaClasificacionUseCase.publicar(solicitudValida()));
            PoliticaClasificacionResponse publicada = esperarPublicacion(publicacion);
            politicasCreadas.add(publicada.getIdPolitica());
            assertThat(idPoliticaVigente()).isEqualTo(publicada.getIdPolitica());
        } finally {
            finalizarPublicacionPendiente(publicacion, null);
            executor.shutdownNow();
        }
    }

    /**
     * Espera, con un plazo máximo, a que exactamente una sesión quede bloqueada por la sesión
     * indicada y devuelve su pid. Falla si hay más de una, para no confundir la publicación con
     * otra sesión ajena a la prueba.
     */
    private int esperarSesionBloqueadaPor(int pidBloqueante) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(SEGUNDOS_MAXIMOS_DE_ESPERA);
        List<Integer> bloqueadas = List.of();
        boolean interrumpida = false;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT pid FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid))")) {
            statement.setQueryTimeout(SEGUNDOS_LIMITE_DE_CONSULTA);
            statement.setInt(1, pidBloqueante);
            while (System.nanoTime() < limite) {
                bloqueadas = consultarPids(statement);
                if (!bloqueadas.isEmpty()) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    interrupcionPendiente = true;
                    interrumpida = true;
                    break;
                }
            }
        } catch (SQLException ex) {
            throw new AssertionError("No fue posible consultar las sesiones bloqueadas en pg_stat_activity ("
                    + ProteccionBaseDedicada.describirErrorJdbc(ex) + ")");
        }
        // los resultados se evalúan con la conexión ya cerrada, para que un error de cierre no
        // quede adjunto como excepción suprimida
        if (interrumpida) {
            throw new AssertionError("La espera del bloqueo fue interrumpida");
        }
        if (bloqueadas.size() > 1) {
            throw new AssertionError("Hay " + bloqueadas.size() + " sesiones bloqueadas por la sesión "
                    + pidBloqueante + "; la base dedicada no debe tener otras sesiones activas");
        }
        if (bloqueadas.size() == 1) {
            return bloqueadas.get(0);
        }
        throw new AssertionError("La publicación concurrente no quedó esperando el bloqueo de la sesión "
                + pidBloqueante);
    }

    private List<Integer> consultarPids(PreparedStatement statement) throws SQLException {
        List<Integer> pids = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                pids.add(resultSet.getInt(1));
            }
        }
        return pids;
    }

    private PoliticaClasificacionResponse esperarPublicacion(Future<PoliticaClasificacionResponse> publicacion)
            throws ExecutionException {
        try {
            return publicacion.get(SEGUNDOS_MAXIMOS_DE_ESPERA, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            interrupcionPendiente = true;
            throw new AssertionError("La espera de la publicación concurrente fue interrumpida", ex);
        } catch (TimeoutException ex) {
            throw new AssertionError("La publicación concurrente no terminó en " + SEGUNDOS_MAXIMOS_DE_ESPERA
                    + " segundos", ex);
        }
    }

    private Throwable causaDelFallo(Future<PoliticaClasificacionResponse> publicacion) {
        try {
            PoliticaClasificacionResponse inesperada = esperarPublicacion(publicacion);
            politicasCreadas.add(inesperada.getIdPolitica());
            throw new AssertionError("Se esperaba que la publicación concurrente fallara");
        } catch (ExecutionException ex) {
            return ex.getCause();
        }
    }

    /**
     * Garantiza que ninguna publicación lanzada por la prueba siga en curso al llegar a la
     * limpieza. Si no termina dentro del plazo y se conoce su pid, se solicita cancelar su consulta
     * en PostgreSQL con pg_cancel_backend (shutdownNow() no detiene una operación JDBC en curso).
     * Si aun así no termina, se registra para que @AfterEach haga fallar la prueba en lugar de
     * ocultar un posible dato residual.
     */
    private void finalizarPublicacionPendiente(Future<PoliticaClasificacionResponse> publicacion,
            Integer pidPublicacion) {
        if (publicacion == null) {
            return;
        }
        ResultadoCancelacion cancelacion = null;
        if (!esperarFinalizacion(publicacion) && pidPublicacion != null && !interrupcionPendiente) {
            cancelacion = cancelarConsultaEnCurso(pidPublicacion);
            esperarFinalizacion(publicacion);
        }
        if (!publicacion.isDone()) {
            // solo se comprueba si la tarea terminó: el envío de la señal no prueba que la
            // publicación se haya detenido
            publicacionesSinTerminar.add("la publicación " + (pidPublicacion == null
                    ? "sin sesión identificada" : "de la sesión " + pidPublicacion)
                    + " sigue en curso" + (cancelacion == null
                    ? " (no se intentó cancelar su consulta)" : " tras intentar cancelarla: " + cancelacion.descripcion()));
            return;
        }
        try {
            politicasCreadas.add(publicacion.get().getIdPolitica());
        } catch (ExecutionException ex) {
            // la publicación falló y su transacción se revirtió: no hay datos que limpiar
        } catch (InterruptedException ex) {
            interrupcionPendiente = true;
        }
    }

    private boolean esperarFinalizacion(Future<PoliticaClasificacionResponse> publicacion) {
        if (interrupcionPendiente) {
            return publicacion.isDone();
        }
        try {
            publicacion.get(SEGUNDOS_MAXIMOS_DE_ESPERA, TimeUnit.SECONDS);
            return true;
        } catch (ExecutionException ex) {
            return true;
        } catch (TimeoutException ex) {
            return false;
        } catch (InterruptedException ex) {
            interrupcionPendiente = true;
            return publicacion.isDone();
        }
    }

    /**
     * Solicita a PostgreSQL cancelar la sentencia en curso de la sesión indicada. El resultado
     * distingue si la señal se envió, si PostgreSQL no la envió o si hubo un error JDBC; en
     * ningún caso confirma que la publicación terminó. No lanza la excepción para no interrumpir
     * la espera final y la limpieza.
     */
    private ResultadoCancelacion cancelarConsultaEnCurso(int pid) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT pg_cancel_backend(?)")) {
            statement.setQueryTimeout(SEGUNDOS_LIMITE_DE_CONSULTA);
            statement.setInt(1, pid);
            boolean senalEnviada;
            try (ResultSet resultSet = statement.executeQuery()) {
                senalEnviada = resultSet.next() && resultSet.getBoolean(1);
            }
            return senalEnviada
                    ? new ResultadoCancelacion(EstadoCancelacion.SENAL_ENVIADA, null)
                    : new ResultadoCancelacion(EstadoCancelacion.SENAL_NO_ENVIADA, null);
        } catch (SQLException ex) {
            return new ResultadoCancelacion(EstadoCancelacion.ERROR_JDBC, ProteccionBaseDedicada.describirErrorJdbc(ex));
        }
    }

    private enum EstadoCancelacion {
        SENAL_ENVIADA,
        SENAL_NO_ENVIADA,
        ERROR_JDBC
    }

    private record ResultadoCancelacion(EstadoCancelacion estado, String errorJdbc) {

        String descripcion() {
            return switch (estado) {
                case SENAL_ENVIADA -> "PostgreSQL envió la señal de cancelación, pero la publicación no terminó";
                case SENAL_NO_ENVIADA -> "PostgreSQL no envió la señal de cancelación";
                case ERROR_JDBC -> "no fue posible solicitar la cancelación (" + errorJdbc + ")";
            };
        }
    }

    private PoliticaClasificacionResponse publicar(PublicarPoliticaClasificacionRequest request) {
        PoliticaClasificacionResponse publicada = publicarPoliticaClasificacionUseCase.publicar(request);
        politicasCreadas.add(publicada.getIdPolitica());
        return publicada;
    }

    private void bloquearConfiguracion(Connection connection) throws SQLException {
        boolean filaBloqueada;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(SEGUNDOS_LIMITE_DE_CONSULTA);
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT id_configuracion FROM configuracion_clasificacion_actual "
                            + "WHERE id_configuracion = 1 FOR UPDATE")) {
                filaBloqueada = resultSet.next();
            }
        }
        assertThat(filaBloqueada).as("La fila singleton de configuración debe existir para bloquearla").isTrue();
    }

    private PoliticaInsertada insertarPoliticaCompletaPorSql(Connection connection, boolean actualizarConfiguracion)
            throws SQLException {
        int version = consultarEntero(connection, "SELECT COALESCE(MAX(version), 0) + 1 FROM politica_clasificacion");
        long idPolitica = insertarPolitica(connection,
                version, Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)));
        insertarRango(connection, idPolitica, "BAJO", -100, 29);
        insertarRango(connection, idPolitica, "MEDIO", 30, 79);
        insertarRango(connection, idPolitica, "ALTO", 80, 130);
        if (actualizarConfiguracion) {
            ejecutarConId(connection,
                    "UPDATE configuracion_clasificacion_actual SET id_politica = ? WHERE id_configuracion = 1",
                    idPolitica);
        }
        return new PoliticaInsertada(idPolitica, version);
    }

    private long insertarPolitica(Connection connection, int version, Timestamp instante) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERTAR_POLITICA + " RETURNING id_politica")) {
            statement.setInt(1, version);
            statement.setTimestamp(2, instante);
            statement.setTimestamp(3, instante);
            statement.setObject(4, AUTOR_PRUEBA);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertarRango(Connection connection, long idPolitica, String categoria, int limiteInferior,
            int limiteSuperior) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERTAR_RANGO)) {
            statement.setLong(1, idPolitica);
            statement.setString(2, categoria);
            statement.setInt(3, limiteInferior);
            statement.setInt(4, limiteSuperior);
            statement.executeUpdate();
        }
    }

    private void assertRechazadoPorLaBaseDeDatos(Connection connection, String sql, Object... parametros)
            throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                statement.setObject(i + 1, parametros[i]);
            }
            assertThatThrownBy(statement::executeUpdate).as(sql).isInstanceOf(SQLException.class);
        } finally {
            connection.rollback(savepoint);
        }
    }

    private int obtenerPid(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(SEGUNDOS_LIMITE_DE_CONSULTA);
            try (ResultSet resultSet = statement.executeQuery("SELECT pg_backend_pid()")) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private Long idPoliticaVigente() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT id_politica FROM configuracion_clasificacion_actual WHERE id_configuracion = 1")) {
            return resultSet.next() ? resultSet.getObject(1, Long.class) : null;
        }
    }

    private int versionMaxima() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return consultarEntero(connection, "SELECT COALESCE(MAX(version), 0) FROM politica_clasificacion");
        }
    }

    private int contarPoliticasConVersion(int version) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return consultarEntero(connection, "SELECT count(*) FROM politica_clasificacion WHERE version = " + version);
        }
    }

    private int contarPoliticasConVersionMayorQue(int version) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return consultarEntero(connection, "SELECT count(*) FROM politica_clasificacion WHERE version > " + version);
        }
    }

    private int contarRangos(long idPolitica) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return consultarEntero(connection,
                    "SELECT count(*) FROM rango_clasificacion WHERE id_politica = " + idPolitica);
        }
    }

    private LocalDateTime fechaCreacionPersistida(long idPolitica) throws SQLException {
        return consultarFecha("SELECT fecha_creacion FROM politica_clasificacion WHERE id_politica = " + idPolitica);
    }

    private LocalDateTime fechaPublicacionPersistida(long idPolitica) throws SQLException {
        return consultarFecha("SELECT fecha_publicacion FROM politica_clasificacion WHERE id_politica = " + idPolitica);
    }

    private LocalDateTime consultarFecha(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getObject(1, LocalDateTime.class);
        }
    }

    private String describirPoliticaPersistida(long idPolitica) throws SQLException {
        StringBuilder descripcion = new StringBuilder();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet politica = statement.executeQuery(
                    "SELECT version, fecha_creacion, fecha_publicacion FROM politica_clasificacion "
                            + "WHERE id_politica = " + idPolitica)) {
                assertThat(politica.next()).isTrue();
                descripcion.append(politica.getInt(1)).append('|')
                        .append(politica.getObject(2, LocalDateTime.class)).append('|')
                        .append(politica.getObject(3, LocalDateTime.class));
            }
            try (ResultSet rangos = statement.executeQuery(
                    "SELECT categoria, limite_inferior, limite_superior FROM rango_clasificacion "
                            + "WHERE id_politica = " + idPolitica + " ORDER BY limite_inferior")) {
                while (rangos.next()) {
                    descripcion.append('|').append(rangos.getString(1))
                            .append(':').append(rangos.getInt(2))
                            .append(':').append(rangos.getInt(3));
                }
            }
        }
        return descripcion.toString();
    }

    private int consultarEntero(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void ejecutarConId(Connection connection, String sql, Long id) throws SQLException {
        ejecutarConId(connection, sql, id, null);
    }

    /** Igual que {@link #ejecutarConId(Connection, String, Long)}, con un tiempo límite opcional en segundos. */
    private void ejecutarConId(Connection connection, String sql, Long id, Integer segundosLimite)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (segundosLimite != null) {
                statement.setQueryTimeout(segundosLimite);
            }
            if (id == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, id);
            }
            statement.executeUpdate();
        }
    }

    private static PublicarPoliticaClasificacionRequest solicitudValida() {
        return new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 29),
                new RangoClasificacionRequest(CategoriaRiesgo.MEDIO, 30, 79),
                new RangoClasificacionRequest(CategoriaRiesgo.ALTO, 80, 130)));
    }

    private static PublicarPoliticaClasificacionRequest otraSolicitudValida() {
        return new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 49),
                new RangoClasificacionRequest(CategoriaRiesgo.MEDIO, 50, 99),
                new RangoClasificacionRequest(CategoriaRiesgo.ALTO, 100, 130)));
    }

    private static List<DefinicionRango> definicionesValidas() {
        return List.of(
                new DefinicionRango(CategoriaRiesgo.BAJO, -100, 29),
                new DefinicionRango(CategoriaRiesgo.MEDIO, 30, 79),
                new DefinicionRango(CategoriaRiesgo.ALTO, 80, 130));
    }

    private record PoliticaInsertada(long idPolitica, int version) {
    }

    /**
     * Protección contra la ejecución sobre una base no autorizada.
     *
     * Garantía principal: Spring invoca un ApplicationContextInitializer antes del refresh del
     * contexto, y todos los beans (DataSource, Flyway e Hibernate) se crean durante el refresh.
     * Por eso, si esta clase lanza una excepción, la carga del contexto se aborta antes de que
     * exista ningún bean capaz de migrar. Con el Environment ya resuelto (variables de entorno,
     * propiedades de JVM, argumentos y archivos de configuración, con su precedencia real):
     * 1. Exige la base autorizada de forma explícita con -Dhu11.it.base-dedicada.
     * 2. Aborta si alguna fuente define una conexión alternativa (Flyway, Hikari, otros pools,
     *    JNDI, XA o tipo de DataSource) o si existe una fuente de propiedades que no se puede
     *    inspeccionar. Para la configuración nativa de Flyway (variables FLYWAY_*, propiedades
     *    flyway.* y archivos flyway.conf / flyway.toml) no se comprueba si Spring Boot la lee:
     *    el proyecto solo usa spring.flyway.*, así que cualquier clave flyway.* o cualquier archivo
     *    en las ubicaciones por defecto de Flyway hace abortar la prueba. Por decisión deliberada
     *    de esta prueba (no es un requisito general de la aplicación), también se rechaza
     *    SPRING_APPLICATION_JSON / spring.application.json, para no depender de en qué momento
     *    Spring Boot incorpora su contenido al entorno.
     * 3. Se conecta con spring.datasource.url, username y password efectivos y exige que la base
     *    sea exactamente la autorizada y que las tablas de V1–V5 estén vacías.
     *
     * Defensas adicionales, que no sustituyen a la anterior: un BeanPostProcessor compara la
     * identidad de la base del DataSource real con la verificada, y @BeforeEach la vuelve a
     * comprobar con el DataSource que usan las pruebas.
     *
     * Los mensajes de error no incluyen valores de propiedades, URL, usuarios, hosts, rutas ni la
     * excepción JDBC original: solo el nombre de la propiedad, una descripción genérica de su
     * fuente y, para errores JDBC, el SQLState y el tipo de la excepción.
     *
     * Limitaciones: se asume, según el comportamiento documentado de Spring Boot, que sin las
     * propiedades rechazadas el DataSource y Flyway usan spring.datasource.*. La comprobación de
     * tablas vacías solo revisa las tablas de V1–V5 en el search_path y no prueba por sí sola que
     * la base sea desechable.
     */
    static class ProteccionBaseDedicada implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static final String PROPIEDAD_BASE_AUTORIZADA = "hu11.it.base-dedicada";
        static final String PROPIEDAD_ESQUEMA_DEDICADO = "hu11.it.esquema-dedicado";
        private static final java.util.regex.Pattern NOMBRE_ESQUEMA_DEDICADO =
                java.util.regex.Pattern.compile("^[a-z][a-z0-9_]*_it$");
        private static final Set<String> PROPIEDADES_DE_CONEXION_NO_ADMITIDAS = Set.of(
                "spring.flyway.url",
                "spring.flyway.user",
                "spring.flyway.password",
                "spring.flyway.driverclassname",
                "spring.datasource.jndiname",
                "spring.datasource.type");
        private static final List<String> PREFIJOS_DE_CONEXION_NO_ADMITIDOS = List.of(
                "spring.flyway.jdbcproperties",
                "spring.datasource.hikari",
                "spring.datasource.dbcp2",
                "spring.datasource.tomcat",
                "spring.datasource.oracleucp",
                "spring.datasource.xa");
        private static final String PREFIJO_CONFIGURACION_NATIVA_FLYWAY = "flyway.";
        private static final String PROPIEDAD_JSON_DE_APLICACION = "spring.application.json";
        private static final List<String> ARCHIVOS_DE_CONFIGURACION_DE_FLYWAY = List.of(
                "flyway.conf", "flyway.toml", "conf/flyway.conf", "conf/flyway.toml");
        private static final Set<String> FUENTES_NO_ENUMERABLES_ADMITIDAS = Set.of("random", "configurationProperties");
        private static final List<String> TABLAS_CON_DATOS_DE_TRABAJO = List.of(
                "solicitante", "riesgo", "regla_scoring", "evaluacion", "detalle_evaluacion");

        static volatile IdentidadBase baseVerificada;
        /** Esquema dedicado verificado como inexistente antes de iniciar el contexto; null si no se verificó. */
        static volatile String esquemaVerificado;
        static volatile String urlVerificada;
        static volatile String usuarioVerificado;
        static volatile String claveVerificada;

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            String baseAutorizada = exigirBaseAutorizada(environment);
            String esquema = exigirEsquemaDedicado(environment);
            rechazarFuentesAlternativasDeConexion(environment);
            rechazarArchivosDeConfiguracionDeFlyway();
            IdentidadBase identidad = verificarConexionConfigurada(environment, baseAutorizada, esquema);
            baseVerificada = identidad;
            urlVerificada = environment.getProperty("spring.datasource.url");
            usuarioVerificado = environment.getProperty("spring.datasource.username");
            claveVerificada = environment.getProperty("spring.datasource.password");
            esquemaVerificado = esquema;

            applicationContext.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource dataSourceCreado) {
                        IdentidadBase identidadDelBean;
                        try (Connection connection = dataSourceCreado.getConnection()) {
                            identidadDelBean = identificar(connection);
                        } catch (SQLException ex) {
                            throw new IllegalStateException("No fue posible verificar el DataSource '" + beanName
                                    + "' (" + describirErrorJdbc(ex) + ")");
                        }
                        // se evalúa con la conexión ya cerrada: un error de cierre no puede quedar
                        // adjunto como excepción suprimida a este rechazo
                        if (!identidad.equals(identidadDelBean)) {
                            throw new IllegalStateException("El DataSource '" + beanName
                                    + "' no apunta a la base verificada antes de iniciar el contexto");
                        }
                    }
                    return bean;
                }
            });
        }

        static IdentidadBase identificar(Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT current_database(), inet_server_port(), pg_postmaster_start_time()::text")) {
                resultSet.next();
                return new IdentidadBase(resultSet.getString(1), resultSet.getObject(2, Integer.class),
                        resultSet.getString(3));
            }
        }

        private static String exigirBaseAutorizada(ConfigurableEnvironment environment) {
            String baseAutorizada = environment.getProperty(PROPIEDAD_BASE_AUTORIZADA);
            if (baseAutorizada == null || baseAutorizada.isBlank()) {
                throw new IllegalStateException("Debe indicarse explícitamente la base dedicada autorizada con -D"
                        + PROPIEDAD_BASE_AUTORIZADA + "=<nombre>");
            }
            if (!baseAutorizada.endsWith(SUFIJO_BASE_DEDICADA)) {
                throw new IllegalStateException("La base autorizada debe terminar en '" + SUFIJO_BASE_DEDICADA
                        + "': " + baseAutorizada);
            }
            return baseAutorizada;
        }

        /**
         * La prueba solo se ejecuta en un esquema dedicado y temporal: nombre en minúsculas terminado
         * en "_it", fijado como currentSchema (único parámetro de la URL) y como spring.flyway.schemas.
         * Así las políticas que crea (inmutables con V9) nunca quedan en el esquema public.
         */
        private static String exigirEsquemaDedicado(ConfigurableEnvironment environment) {
            String esquema = environment.getProperty(PROPIEDAD_ESQUEMA_DEDICADO);
            if (esquema == null || !NOMBRE_ESQUEMA_DEDICADO.matcher(esquema).matches()) {
                throw new IllegalStateException("Debe indicarse un esquema dedicado con -D" + PROPIEDAD_ESQUEMA_DEDICADO
                        + "=<nombre en minúsculas terminado en '" + SUFIJO_BASE_DEDICADA + "'>");
            }
            String url = environment.getProperty("spring.datasource.url");
            if (url == null || !url.endsWith("?currentSchema=" + esquema) || url.indexOf('?') != url.lastIndexOf('?')) {
                throw new IllegalStateException("spring.datasource.url debe terminar en ?currentSchema=" + esquema
                        + " y no tener otros parámetros");
            }
            if (!esquema.equals(environment.getProperty("spring.flyway.schemas"))) {
                throw new IllegalStateException("spring.flyway.schemas debe ser exactamente " + esquema);
            }
            return esquema;
        }

        private static void rechazarFuentesAlternativasDeConexion(ConfigurableEnvironment environment) {
            for (PropertySource<?> fuente : environment.getPropertySources()) {
                if (PROPIEDAD_JSON_DE_APLICACION.equals(fuente.getName())) {
                    throw new IllegalStateException("La configuración proviene de SPRING_APPLICATION_JSON / "
                            + "spring.application.json, que esta prueba no admite");
                }
                if (fuente instanceof EnumerablePropertySource<?> enumerable) {
                    for (String nombre : enumerable.getPropertyNames()) {
                        String normalizado = normalizar(nombre);
                        if (PROPIEDAD_JSON_DE_APLICACION.equals(normalizado)) {
                            throw new IllegalStateException("La propiedad '" + nombre + "' (" + describirFuente(fuente)
                                    + ") no está admitida en esta prueba: SPRING_APPLICATION_JSON / "
                                    + "spring.application.json se rechaza por decisión deliberada de la prueba");
                        }
                        if (PROPIEDADES_DE_CONEXION_NO_ADMITIDAS.contains(normalizado)
                                || PREFIJOS_DE_CONEXION_NO_ADMITIDOS.stream().anyMatch(normalizado::startsWith)) {
                            throw new IllegalStateException("La propiedad '" + nombre + "' (" + describirFuente(fuente)
                                    + ") no está admitida en esta prueba: definiría una conexión distinta de "
                                    + "spring.datasource.url");
                        }
                        if (normalizado.startsWith(PREFIJO_CONFIGURACION_NATIVA_FLYWAY)) {
                            throw new IllegalStateException("La propiedad '" + nombre + "' (" + describirFuente(fuente)
                                    + ") es configuración nativa de Flyway y podría definir una conexión o cargar "
                                    + "archivos de configuración que esta prueba no puede verificar");
                        }
                    }
                } else if (!(fuente instanceof PropertySource.StubPropertySource)
                        && !FUENTES_NO_ENUMERABLES_ADMITIDAS.contains(fuente.getName())) {
                    throw new IllegalStateException("Una fuente de propiedades de tipo "
                            + fuente.getClass().getSimpleName()
                            + " no se puede inspeccionar; se aborta la prueba por seguridad");
                }
            }
        }

        /**
         * Descripción genérica de la fuente: el nombre real de una fuente de archivo incluye su
         * ruta, que puede contener el nombre del usuario del sistema.
         */
        private static String describirFuente(PropertySource<?> fuente) {
            return switch (fuente.getName()) {
                case "systemEnvironment" -> "variables de entorno";
                case "systemProperties" -> "propiedades de la JVM";
                default -> fuente.getName().startsWith("Config resource") ? "archivo de configuración"
                        : "otra fuente de configuración";
            };
        }

        /**
         * Solo el SQLState (código estándar de cinco caracteres) y el tipo de la excepción: el
         * mensaje JDBC original puede contener la URL, el host o el usuario de la conexión.
         * Visible en el paquete para que la prueba lo reutilice en los puntos de conexión.
         */
        static String describirErrorJdbc(SQLException ex) {
            String sqlState = ex.getSQLState() == null ? "desconocido" : ex.getSQLState();
            return "SQLState " + sqlState + ", " + ex.getClass().getSimpleName();
        }

        /**
         * Flyway puede cargar flyway.conf / flyway.toml del directorio personal y del directorio de
         * trabajo cuando se le pide leer sus archivos por defecto. No se comprueba si Spring Boot lo
         * hace: si alguno de esos archivos existe, se aborta. Los mensajes no incluyen la ruta
         * completa, que contiene el nombre del usuario del sistema.
         */
        private static void rechazarArchivosDeConfiguracionDeFlyway() {
            List<String[]> directorios = List.of(
                    new String[] {"directorio personal", System.getProperty("user.home")},
                    new String[] {"directorio de trabajo", System.getProperty("user.dir")});
            for (String[] directorio : directorios) {
                if (directorio[1] == null) {
                    continue;
                }
                for (String archivo : ARCHIVOS_DE_CONFIGURACION_DE_FLYWAY) {
                    if (Files.exists(Path.of(directorio[1], archivo))) {
                        throw new IllegalStateException("Existe el archivo de configuración de Flyway '" + archivo
                                + "' en el " + directorio[0] + "; podría definir una conexión que esta prueba no "
                                + "puede verificar");
                    }
                }
            }
        }

        private static String normalizar(String nombre) {
            return nombre.toLowerCase(Locale.ROOT).replace('_', '.').replace("-", "");
        }

        private static IdentidadBase verificarConexionConfigurada(ConfigurableEnvironment environment,
                String baseAutorizada, String esquema) {
            String url = environment.getProperty("spring.datasource.url");
            if (url == null || !url.startsWith("jdbc:postgresql:")) {
                throw new IllegalStateException("spring.datasource.url debe estar definida y apuntar a PostgreSQL");
            }
            IdentidadBase identidad;
            String tablaConDatos = null;
            boolean esquemaExiste = false;
            try (Connection connection = DriverManager.getConnection(url,
                    environment.getProperty("spring.datasource.username"),
                    environment.getProperty("spring.datasource.password"))) {
                identidad = identificar(connection);
                if (baseAutorizada.equals(identidad.nombre())) {
                    esquemaExiste = existeEsquema(connection, esquema);
                    tablaConDatos = primeraTablaConDatos(connection);
                }
            } catch (SQLException ex) {
                // incluye un error al cerrar la conexión: también hace fallar, con un mensaje seguro
                throw new IllegalStateException("No fue posible verificar la base configurada ("
                        + describirErrorJdbc(ex) + ")");
            }
            // los rechazos se evalúan con la conexión ya cerrada: un error de cierre no puede quedar
            // adjunto como excepción suprimida
            if (!baseAutorizada.equals(identidad.nombre())) {
                throw new IllegalStateException("La conexión configurada apunta a la base '" + identidad.nombre()
                        + "', distinta de la base dedicada autorizada '" + baseAutorizada + "'");
            }
            if (esquemaExiste) {
                throw new IllegalStateException("El esquema dedicado " + esquema + " ya existe; no se reutiliza ni "
                        + "se elimina");
            }
            if (tablaConDatos != null) {
                throw new IllegalStateException("La base '" + identidad.nombre() + "' contiene datos en la tabla "
                        + tablaConDatos + "; esta prueba solo admite una base desechable");
            }
            return identidad;
        }

        static boolean existeEsquema(Connection connection, String esquema) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)")) {
                statement.setString(1, esquema);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getBoolean(1);
                }
            }
        }

        private static String primeraTablaConDatos(Connection connection) throws SQLException {
            for (String tabla : TABLAS_CON_DATOS_DE_TRABAJO) {
                if (tablaTieneFilas(connection, tabla)) {
                    return tabla;
                }
            }
            return null;
        }

        private static boolean tablaTieneFilas(Connection connection, String tabla) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT to_regclass('" + tabla + "') IS NOT NULL")) {
                    resultSet.next();
                    if (!resultSet.getBoolean(1)) {
                        return false;
                    }
                }
                try (ResultSet resultSet = statement.executeQuery("SELECT EXISTS (SELECT 1 FROM " + tabla + ")")) {
                    resultSet.next();
                    return resultSet.getBoolean(1);
                }
            }
        }

        record IdentidadBase(String nombre, Integer puerto, String inicioDelServidor) {
        }
    }
}
