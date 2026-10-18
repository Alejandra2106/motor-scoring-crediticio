package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import com.crediticio.scoring.domain.PublicacionSinAutorException;
import com.crediticio.scoring.ports.input.PublicarPoliticaClasificacionUseCase;
import com.crediticio.scoring.ports.output.ActorActualPort;
import com.crediticio.scoring.ports.output.PoliticaClasificacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
public class PublicarPoliticaClasificacionService implements PublicarPoliticaClasificacionUseCase {

    private final PoliticaClasificacionRepositoryPort politicaClasificacionRepositoryPort;
    private final ActorActualPort actorActualPort;
    private final PoliticaClasificacionMapper politicaClasificacionMapper;

    public PublicarPoliticaClasificacionService(PoliticaClasificacionRepositoryPort politicaClasificacionRepositoryPort,
            ActorActualPort actorActualPort, PoliticaClasificacionMapper politicaClasificacionMapper) {
        this.politicaClasificacionRepositoryPort = politicaClasificacionRepositoryPort;
        this.actorActualPort = actorActualPort;
        this.politicaClasificacionMapper = politicaClasificacionMapper;
    }

    @Override
    @Transactional
    public PoliticaClasificacionResponse publicar(PublicarPoliticaClasificacionRequest request) {
        Objects.requireNonNull(request, "request es obligatorio");
        // CA14: el autor es el usuario autenticado (claim sub); sin él la publicación se rechaza
        UUID publicadoPor = actorActualPort.idUsuarioAutenticado().orElseThrow(PublicacionSinAutorException::new);
        // Un único instante para fecha_creacion y fecha_publicacion, truncado a microsegundos
        // porque esa es la precisión de TIMESTAMP en PostgreSQL.
        LocalDateTime instantePublicacion = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(
                instantePublicacion, publicadoPor, politicaClasificacionMapper.aDefiniciones(request));
        PoliticaClasificacion publicada = politicaClasificacionRepositoryPort.publicar(politica);
        return politicaClasificacionMapper.aResponse(publicada);
    }
}
