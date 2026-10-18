package com.crediticio.shared.util;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Jackson por defecto trunca un literal JSON decimal (p. ej. 3.7) hacia un campo {@code Integer}
 * en vez de rechazarlo. Este deserializador solo acepta el token JSON de tipo entero; cualquier
 * otro valor termina en HTTP 400 mediante el manejo global de errores.
 */
public class EnteroEstrictoDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }
        throw InvalidFormatException.from(parser,
                "el valor debe ser un número entero, sin notación decimal", parser.getText(), Integer.class);
    }
}
