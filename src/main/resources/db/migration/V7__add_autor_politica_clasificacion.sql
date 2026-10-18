-- HU11 CA14: autor de cada publicacion de la politica de clasificacion (claim sub del JWT de
-- auth-api, UUID). Las versiones publicadas antes de este cambio no tienen autor y conservan NULL.
ALTER TABLE politica_clasificacion
    ADD COLUMN publicado_por UUID;

-- NOT VALID: PostgreSQL no comprueba las filas existentes (las versiones historicas sin autor siguen
-- siendo validas), pero exige la condicion en toda fila insertada o actualizada desde ahora.
-- No ejecutar VALIDATE CONSTRAINT mientras existan versiones historicas sin autor.
ALTER TABLE politica_clasificacion
    ADD CONSTRAINT ck_politica_clasificacion_publicado_por
        CHECK (publicado_por IS NOT NULL) NOT VALID;
