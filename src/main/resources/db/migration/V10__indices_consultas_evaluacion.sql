-- HU13 y HU15: índices para las consultas de evaluation-query-api y para el trigger A de V8.
-- detalle_evaluacion(id_evaluacion): la explicación de una evaluación (HU13) y la suma de detalles que el
-- trigger A de V8 calcula en cada inserción leían toda la tabla; PostgreSQL no indexa las claves foráneas.
-- evaluacion(id_solicitante, fecha_evaluacion DESC, id_evaluacion DESC): el historial de un solicitante
-- (HU15) en el orden exigido, sin ordenar en memoria.
CREATE INDEX ix_detalle_evaluacion_evaluacion ON detalle_evaluacion (id_evaluacion);

CREATE INDEX ix_evaluacion_solicitante_fecha
    ON evaluacion (id_solicitante, fecha_evaluacion DESC, id_evaluacion DESC);
