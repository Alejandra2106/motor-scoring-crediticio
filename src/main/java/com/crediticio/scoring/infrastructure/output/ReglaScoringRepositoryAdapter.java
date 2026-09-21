package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.ports.output.ReglaScoringRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ReglaScoringRepositoryAdapter implements ReglaScoringRepositoryPort {

    private final ReglaScoringJpaRepository reglaScoringJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ReglaScoringRepositoryAdapter(ReglaScoringJpaRepository reglaScoringJpaRepository) {
        this.reglaScoringJpaRepository = reglaScoringJpaRepository;
    }

    @Override
    public ReglaScoring guardar(ReglaScoring reglaScoring) {
        try {
            ReglaScoringJpaEntity entityGuardada = reglaScoringJpaRepository.save(aJpaEntity(reglaScoring));
            entityManager.flush();
            entityManager.refresh(entityGuardada);
            return aDominio(entityGuardada);
        } catch (DataIntegrityViolationException ex) {
            throw new ReglaScoringDuplicadaException();
        }
    }

    @Override
    public boolean existeCombinacion(Long idRiesgo, OperadorScoring operador, String valorCondicion) {
        return reglaScoringJpaRepository.existsByIdRiesgoAndOperadorAndValorCondicion(
                idRiesgo, operador.getSimbolo(), valorCondicion);
    }

    private ReglaScoringJpaEntity aJpaEntity(ReglaScoring reglaScoring) {
        return new ReglaScoringJpaEntity(
                reglaScoring.getIdRiesgo(),
                reglaScoring.getOperador().getSimbolo(),
                reglaScoring.getValorCondicion(),
                reglaScoring.getPuntaje(),
                reglaScoring.getEstado());
    }

    private ReglaScoring aDominio(ReglaScoringJpaEntity entity) {
        return ReglaScoring.reconstruir(
                entity.getIdRegla(),
                entity.getIdRiesgo(),
                OperadorScoring.desdeSimbolo(entity.getOperador()),
                entity.getValorCondicion(),
                entity.getPuntaje(),
                entity.getEstado(),
                entity.getFechaCreacion());
    }
}
