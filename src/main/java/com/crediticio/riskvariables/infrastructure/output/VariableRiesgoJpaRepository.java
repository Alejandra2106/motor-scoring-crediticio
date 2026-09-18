package com.crediticio.riskvariables.infrastructure.output;

import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableRiesgoJpaRepository extends JpaRepository<VariableRiesgoJpaEntity, Long> {

    boolean existsByVariable(NombreVariableRiesgo variable);
}
