package com.crediticio.shared.util;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Igual que {@link EnteroEstrictoDeserializer}, para campos {@code Long} (p. ej. identificadores):
 * un decimal como 3.7 se rechaza en vez de truncarse a 3 y apuntar a otro registro.
 */
public class EnteroLargoEstrictoDeserializer extends ValueDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getLongValue();
        }
        throw InvalidFormatException.from(parser,
                "el valor debe ser un número entero, sin notación decimal", parser.getText(), Long.class);
    }
}
