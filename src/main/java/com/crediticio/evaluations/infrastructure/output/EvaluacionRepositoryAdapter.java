package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.ports.output.EvaluacionRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvaluacionRepositoryAdapter implements EvaluacionRepositoryPort {

    private final EvaluacionJpaRepository evaluacionJpaRepository;
    private final DetalleEvaluacionJpaRepository detalleEvaluacionJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EvaluacionRepositoryAdapter(EvaluacionJpaRepository evaluacionJpaRepository,
            DetalleEvaluacionJpaRepository detalleEvaluacionJpaRepository) {
        this.evaluacionJpaRepository = evaluacionJpaRepository;
        this.detalleEvaluacionJpaRepository = detalleEvaluacionJpaRepository;
    }

    @Override
    public Evaluacion guardar(Evaluacion evaluacion) {
        EvaluacionJpaEntity evaluacionEntity = evaluacionJpaRepository.save(
                new EvaluacionJpaEntity(evaluacion.getIdSolicitante()));
        entityManager.flush();
        entityManager.refresh(evaluacionEntity);

        List<DetalleEvaluacionJpaEntity> detalleEntities = evaluacion.getDetalles().stream()
                .map(detalle -> aJpaEntity(evaluacionEntity.getIdEvaluacion(), detalle))
                .toList();
        List<DetalleEvaluacionJpaEntity> detallesGuardados = detalleEvaluacionJpaRepository.saveAll(detalleEntities);
        entityManager.flush();

        return aDominio(evaluacionEntity, detallesGuardados);
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

    private Evaluacion aDominio(EvaluacionJpaEntity entity, List<DetalleEvaluacionJpaEntity> detalles) {
        return Evaluacion.reconstruir(
                entity.getIdEvaluacion(),
                entity.getIdSolicitante(),
                entity.getFechaEvaluacion(),
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
