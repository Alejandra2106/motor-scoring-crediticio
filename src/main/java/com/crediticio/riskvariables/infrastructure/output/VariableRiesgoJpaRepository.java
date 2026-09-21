package com.crediticio.riskvariables.infrastructure.output;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariableRiesgoJpaRepository extends JpaRepository<VariableRiesgoJpaEntity, Long> {

    boolean existsByVariable(NombreVariableRiesgo variable);

    List<VariableRiesgoJpaEntity> findByEstado(EstadoVariableRiesgo estado);
}
