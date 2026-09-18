package com.crediticio.riskvariables.infrastructure.output;

import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class VariableRiesgoRepositoryAdapter implements VariableRiesgoRepositoryPort {

    private final VariableRiesgoJpaRepository variableRiesgoJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public VariableRiesgoRepositoryAdapter(VariableRiesgoJpaRepository variableRiesgoJpaRepository) {
        this.variableRiesgoJpaRepository = variableRiesgoJpaRepository;
    }

    @Override
    public VariableRiesgo guardar(VariableRiesgo variableRiesgo) {
        VariableRiesgoJpaEntity entity = aJpaEntity(variableRiesgo);
        try {
            VariableRiesgoJpaEntity entityGuardada = variableRiesgoJpaRepository.save(entity);
            entityManager.refresh(entityGuardada);
            return aDominio(entityGuardada);
        } catch (DataIntegrityViolationException ex) {
            throw new VariableRiesgoDuplicadaException();
        }
    }

    @Override
    public boolean existePorVariable(NombreVariableRiesgo variable) {
        return variableRiesgoJpaRepository.existsByVariable(variable);
    }

    private VariableRiesgoJpaEntity aJpaEntity(VariableRiesgo variableRiesgo) {
        return new VariableRiesgoJpaEntity(
                variableRiesgo.getVariable(),
                variableRiesgo.getDescripcion(),
                variableRiesgo.getEstado());
    }

    private VariableRiesgo aDominio(VariableRiesgoJpaEntity entity) {
        return VariableRiesgo.reconstruir(
                entity.getIdRiesgo(),
                entity.getVariable(),
                entity.getDescripcion(),
                entity.getEstado(),
                entity.getFechaCreacion());
    }
}
