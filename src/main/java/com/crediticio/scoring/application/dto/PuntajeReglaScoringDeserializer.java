package com.crediticio.scoring.application.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Jackson por defecto trunca un literal JSON decimal (p. ej. 20.5 o 20.0) hacia un campo
 * {@code Integer} en vez de rechazarlo, lo que viola RF09 de HU05 ("puntaje" debe ser
 * estrictamente entero). Este deserializador solo acepta el token JSON de tipo entero.
 */
class PuntajeReglaScoringDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }
        throw InvalidFormatException.from(parser,
                "puntaje debe ser un número entero, sin notación decimal", parser.getText(), Integer.class);
    }
}
