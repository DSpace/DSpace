/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;
import java.time.LocalDate;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.core.JacksonException;

/**
 * This is a custom date deserializer for jackson that make use of our
 * {@link MultiFormatDateParser}
 *
 * Dates are parsed as being in the UTC zone.
 *
 */
public class MultiFormatDateDeserializer extends StdDeserializer<LocalDate> {

    public MultiFormatDateDeserializer() {
        this(null);
    }

    public MultiFormatDateDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonparser, DeserializationContext context)
            throws JacksonException {
        String date = jsonparser.getString();
        return MultiFormatDateParser.parse(date).toLocalDate();
    }
}