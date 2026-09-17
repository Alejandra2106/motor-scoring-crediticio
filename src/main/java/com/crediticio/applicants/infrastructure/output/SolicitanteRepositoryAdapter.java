package com.crediticio.applicants.infrastructure.output;

import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SolicitanteRepositoryAdapter implements SolicitanteRepositoryPort {

    private final SolicitanteJpaRepository solicitanteJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SolicitanteRepositoryAdapter(SolicitanteJpaRepository solicitanteJpaRepository) {
        this.solicitanteJpaRepository = solicitanteJpaRepository;
    }

    @Override
    public Solicitante guardar(Solicitante solicitante) {
        SolicitanteJpaEntity entity = aJpaEntity(solicitante);
        try {
            SolicitanteJpaEntity entityGuardada = solicitanteJpaRepository.save(entity);
            entityManager.refresh(entityGuardada);
            return aDominio(entityGuardada);
        } catch (DataIntegrityViolationException ex) {
            throw new DocumentoDuplicadoException();
        }
    }

    @Override
    public boolean existePorNumeroDocumento(String numeroDocumento) {
        return solicitanteJpaRepository.existsByNumeroDocumento(numeroDocumento);
    }

    @Override
    public Optional<Solicitante> buscarPorNumeroDocumento(String numeroDocumento) {
        return solicitanteJpaRepository.findByNumeroDocumento(numeroDocumento)
                .map(this::aDominio);
    }

    private SolicitanteJpaEntity aJpaEntity(Solicitante solicitante) {
        return new SolicitanteJpaEntity(
                solicitante.getNombreCompleto(),
                solicitante.getNumeroDocumento(),
                solicitante.getIngresosMensuales(),
                solicitante.getDeudasMensuales(),
                solicitante.getNumeroMoras(),
                solicitante.getHistorialCrediticio(),
                solicitante.getAntiguedadLaboral());
    }

    private Solicitante aDominio(SolicitanteJpaEntity entity) {
        return Solicitante.reconstruir(
                entity.getIdSolicitante(),
                entity.getNombreCompleto(),
                entity.getNumeroDocumento(),
                entity.getIngresosMensuales(),
                entity.getDeudasMensuales(),
                entity.getNumeroMoras(),
                entity.getHistorialCrediticio(),
                entity.getAntiguedadLaboral(),
                entity.getFechaRegistro());
    }
}
