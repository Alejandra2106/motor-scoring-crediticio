package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.domain.ConfiguracionClasificacionNoInicializadaException;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import com.crediticio.scoring.domain.PublicacionPoliticaConcurrenteException;
import com.crediticio.scoring.domain.RangoClasificacion;
import com.crediticio.scoring.ports.output.PoliticaClasificacionConsultaPort;
import com.crediticio.scoring.ports.output.PoliticaClasificacionRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * La publicación debe invocarse dentro de una transacción (la del caso de uso de publicación):
 * el bloqueo PESSIMISTIC_WRITE sobre la fila singleton de configuración serializa las
 * publicaciones concurrentes y se mantiene hasta el commit o el rollback. UNIQUE(version) actúa
 * como protección adicional si alguna inserción se hiciera sin ese bloqueo.
 *
 * La consulta de la política vigente lee la fila de configuración sin bloqueo: como las
 * políticas publicadas son inmutables, la política referenciada y sus rangos no cambian después
 * de leer su identificador.
 */
@Component
public class PoliticaClasificacionRepositoryAdapter
        implements PoliticaClasificacionRepositoryPort, PoliticaClasificacionConsultaPort {

    static final Short ID_CONFIGURACION_ACTUAL = 1;
    private static final String RESTRICCION_VERSION_UNICA = "uq_politica_clasificacion_version";

    private final PoliticaClasificacionJpaRepository politicaClasificacionJpaRepository;
    private final RangoClasificacionJpaRepository rangoClasificacionJpaRepository;
    private final ConfiguracionClasificacionActualJpaRepository configuracionClasificacionActualJpaRepository;

    public PoliticaClasificacionRepositoryAdapter(PoliticaClasificacionJpaRepository politicaClasificacionJpaRepository,
            RangoClasificacionJpaRepository rangoClasificacionJpaRepository,
            ConfiguracionClasificacionActualJpaRepository configuracionClasificacionActualJpaRepository) {
        this.politicaClasificacionJpaRepository = politicaClasificacionJpaRepository;
        this.rangoClasificacionJpaRepository = rangoClasificacionJpaRepository;
        this.configuracionClasificacionActualJpaRepository = configuracionClasificacionActualJpaRepository;
    }

    @Override
    public PoliticaClasificacion publicar(PoliticaClasificacion politica) {
        ConfiguracionClasificacionActualJpaEntity configuracion = configuracionClasificacionActualJpaRepository
                .findByIdConfiguracion(ID_CONFIGURACION_ACTUAL)
                .orElseThrow(ConfiguracionClasificacionNoInicializadaException::new);
        int siguienteVersion = politicaClasificacionJpaRepository.buscarVersionMaxima().orElse(0) + 1;

        try {
            PoliticaClasificacionJpaEntity politicaGuardada = politicaClasificacionJpaRepository.save(
                    new PoliticaClasificacionJpaEntity(siguienteVersion, politica.getFechaCreacion(),
                            politica.getFechaPublicacion(), politica.getPublicadoPor()));

            List<RangoClasificacionJpaEntity> rangosGuardados = rangoClasificacionJpaRepository.saveAll(
                    politica.getRangos().stream()
                            .map(rango -> new RangoClasificacionJpaEntity(
                                    politicaGuardada.getIdPolitica(),
                                    rango.getCategoria(),
                                    rango.getLimiteInferior(),
                                    rango.getLimiteSuperior()))
                            .toList());

            configuracion.setIdPolitica(politicaGuardada.getIdPolitica());
            configuracionClasificacionActualJpaRepository.saveAndFlush(configuracion);

            return aDominio(politicaGuardada, rangosGuardados);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionDeVersionUnica(ex)) {
                throw new PublicacionPoliticaConcurrenteException();
            }
            throw ex;
        }
    }

    @Override
    public Optional<PoliticaClasificacion> buscarVigente() {
        ConfiguracionClasificacionActualJpaEntity configuracion = configuracionClasificacionActualJpaRepository
                .findById(ID_CONFIGURACION_ACTUAL)
                .orElseThrow(ConfiguracionClasificacionNoInicializadaException::new);
        if (configuracion.getIdPolitica() == null) {
            return Optional.empty();
        }
        // la clave foránea de la configuración garantiza que la política referenciada existe
        PoliticaClasificacionJpaEntity politica = politicaClasificacionJpaRepository
                .findById(configuracion.getIdPolitica())
                .orElseThrow(() -> new IllegalStateException(
                        "La configuración referencia una política de clasificación inexistente"));
        return Optional.of(aDominio(politica,
                rangoClasificacionJpaRepository.findByIdPoliticaOrderByLimiteInferior(politica.getIdPolitica())));
    }

    private boolean esViolacionDeVersionUnica(DataIntegrityViolationException ex) {
        String mensaje = ex.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.contains(RESTRICCION_VERSION_UNICA);
    }

    private PoliticaClasificacion aDominio(PoliticaClasificacionJpaEntity politica,
            List<RangoClasificacionJpaEntity> rangos) {
        return PoliticaClasificacion.reconstruir(
                politica.getIdPolitica(),
                politica.getNumeroVersion(),
                politica.getFechaCreacion(),
                politica.getFechaPublicacion(),
                politica.getPublicadoPor(),
                rangos.stream()
                        .map(rango -> RangoClasificacion.reconstruir(
                                rango.getIdRango(),
                                rango.getCategoria(),
                                rango.getLimiteInferior(),
                                rango.getLimiteSuperior()))
                        .toList());
    }
}
