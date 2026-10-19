package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.domain.ClasificacionEvaluacion;
import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.EstadoEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.ports.output.EvaluacionConsultaPort;
import com.crediticio.evaluations.ports.output.EvaluacionRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EvaluacionRepositoryAdapter implements EvaluacionRepositoryPort, EvaluacionConsultaPort {

    private final EvaluacionJpaRepository evaluacionJpaRepository;
    private final DetalleEvaluacionJpaRepository detalleEvaluacionJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EvaluacionRepositoryAdapter(EvaluacionJpaRepository evaluacionJpaRepository,
            DetalleEvaluacionJpaRepository detalleEvaluacionJpaRepository) {
        this.evaluacionJpaRepository = evaluacionJpaRepository;
        this.detalleEvaluacionJpaRepository = detalleEvaluacionJpaRepository;
    }

    /**
     * HU12: el puntaje total y la clasificación se escriben junto con la evaluación, en la misma
     * transacción que sus detalles (CA05/CA08).
     */
    @Override
    public Evaluacion guardar(Evaluacion evaluacion) {
        ClasificacionEvaluacion clasificacion = evaluacion.getClasificacion();
        EvaluacionJpaEntity evaluacionEntity = evaluacionJpaRepository.save(new EvaluacionJpaEntity(
                evaluacion.getIdSolicitante(),
                evaluacion.getScoreTotal(),
                clasificacion.categoria(),
                clasificacion.idPolitica(),
                evaluacion.getEstado() == null ? null : evaluacion.getEstado().name()));
        entityManager.flush();
        entityManager.refresh(evaluacionEntity);

        List<DetalleEvaluacionJpaEntity> detalleEntities = evaluacion.getDetalles().stream()
                .map(detalle -> aJpaEntity(evaluacionEntity.getIdEvaluacion(), detalle))
                .toList();
        List<DetalleEvaluacionJpaEntity> detallesGuardados = detalleEvaluacionJpaRepository.saveAll(detalleEntities);
        entityManager.flush();

        return aDominio(evaluacionEntity, clasificacion, detallesGuardados);
    }

    @Override
    public Optional<Evaluacion> buscarPorId(Long idEvaluacion) {
        return evaluacionJpaRepository.findById(idEvaluacion)
                .map(entity -> aDominio(entity, clasificacionPersistida(entity),
                        detalleEvaluacionJpaRepository.findByIdEvaluacionOrderByIdDetalleEvaluacion(idEvaluacion)));
    }

    /** Null para evaluaciones anteriores a HU12, que no se reclasifican. */
    private ClasificacionEvaluacion clasificacionPersistida(EvaluacionJpaEntity entity) {
        if (entity.getIdPolitica() == null) {
            return null;
        }
        Integer version = evaluacionJpaRepository.buscarVersionPolitica(entity.getIdPolitica())
                .orElseThrow(() -> new IllegalStateException("La evaluación " + entity.getIdEvaluacion()
                        + " referencia una política inexistente"));
        return new ClasificacionEvaluacion(entity.getCategoria(), entity.getIdPolitica(), version);
    }

    private DetalleEvaluacionJpaEntity aJpaEntity(Long idEvaluacion, DetalleEvaluacion detalle) {
        return new DetalleEvaluacionJpaEntity(
                idEvaluacion,
                detalle.getIdRegla(),
                detalle.getOperadorAplicado(),
                detalle.getValorCondicionAplicado(),
                detalle.getPuntajeReglaAplicado(),
                detalle.isCondicionCumplida(),
                detalle.getPuntajeObtenido());
    }

    private Evaluacion aDominio(EvaluacionJpaEntity entity, ClasificacionEvaluacion clasificacion,
            List<DetalleEvaluacionJpaEntity> detalles) {
        return Evaluacion.reconstruir(
                entity.getIdEvaluacion(),
                entity.getIdSolicitante(),
                entity.getFechaEvaluacion(),
                entity.getScoreTotal(),
                clasificacion,
                entity.getEstado() == null ? null : EstadoEvaluacion.valueOf(entity.getEstado()),
                detalles.stream().map(this::aDominioDetalle).toList());
    }

    private DetalleEvaluacion aDominioDetalle(DetalleEvaluacionJpaEntity entity) {
        return DetalleEvaluacion.reconstruir(
                entity.getIdDetalleEvaluacion(),
                entity.getIdRegla(),
                entity.getOperadorAplicado(),
                entity.getValorCondicionAplicado(),
                entity.getPuntajeReglaAplicado(),
                entity.getCondicionCumplida(),
                entity.getPuntajeObtenido());
    }
}
