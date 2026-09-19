ALTER TABLE riesgo
    DROP CONSTRAINT ck_riesgo_estado;

ALTER TABLE riesgo
    ADD CONSTRAINT ck_riesgo_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'));

ALTER TABLE riesgo
    ADD COLUMN fecha_modificacion TIMESTAMP NULL;
