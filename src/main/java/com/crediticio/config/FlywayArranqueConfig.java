package com.crediticio.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Protección contra migraciones accidentales al arrancar. Por defecto Flyway solo valida: si la base tiene
 * migraciones pendientes (por ejemplo, motor_scoring en V9 frente a V10 y V11), la validación falla, la
 * aplicación no arranca y no se aplica nada. Las migraciones se ejecutan solo de forma controlada, con
 * SCORING_FLYWAY_MIGRAR_AL_ARRANCAR=true, después del respaldo verificado y de la autorización
 * (docs/operacion/despliegue-v10-v11.md).
 */
@Configuration
public class FlywayArranqueConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayArranqueConfig.class);

    @Bean
    public FlywayMigrationStrategy estrategiaMigracion(
            @Value("${scoring.flyway.migrar-al-arrancar:false}") boolean migrarAlArrancar) {
        return flyway -> ejecutar(flyway, migrarAlArrancar);
    }

    static void ejecutar(Flyway flyway, boolean migrarAlArrancar) {
        if (migrarAlArrancar) {
            log.warn("Flyway aplicará las migraciones pendientes (scoring.flyway.migrar-al-arrancar=true)");
            flyway.migrate();
            return;
        }
        try {
            flyway.validate();
        } catch (RuntimeException ex) {
            log.error("La base no coincide con las migraciones de esta versión (puede haber migraciones pendientes). "
                    + "No se aplicó ninguna. Siga el procedimiento de despliegue controlado.");
            throw ex;
        }
    }
}
