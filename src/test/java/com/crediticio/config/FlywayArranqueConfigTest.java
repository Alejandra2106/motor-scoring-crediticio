package com.crediticio.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Un arranque normal solo valida; únicamente el arranque controlado migra. */
class FlywayArranqueConfigTest {

    @Test
    void porDefectoSoloValidaYNuncaMigra() {
        Flyway flyway = mock(Flyway.class);

        FlywayArranqueConfig.ejecutar(flyway, false);

        verify(flyway).validate();
        verify(flyway, never()).migrate();
    }

    @Test
    void conMigracionesPendientesElArranqueFallaSinMigrar() {
        Flyway flyway = mock(Flyway.class);
        doThrow(new FlywayException("Detected resolved migration not applied to database: 10")).when(flyway).validate();

        assertThatThrownBy(() -> FlywayArranqueConfig.ejecutar(flyway, false)).isInstanceOf(FlywayException.class);
        verify(flyway, never()).migrate();
    }

    @Test
    void elArranqueControladoMigra() {
        Flyway flyway = mock(Flyway.class);

        FlywayArranqueConfig.ejecutar(flyway, true);

        verify(flyway).migrate();
        verify(flyway, never()).validate();
    }
}
