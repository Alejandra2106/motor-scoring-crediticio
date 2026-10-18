package com.crediticio.scoring.domain;

public class PublicacionPoliticaConcurrenteException extends RuntimeException {

    public PublicacionPoliticaConcurrenteException() {
        super("Otra publicación de política de clasificación obtuvo la misma versión; intente publicar nuevamente");
    }
}
