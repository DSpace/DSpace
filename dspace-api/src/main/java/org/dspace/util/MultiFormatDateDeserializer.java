/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * This is a custom date deserializer for jackson that make use of our
 * {@link MultiFormatDateParser}
 *
 * Dates are parsed as being in the UTC zone.
 *
 */
public class MultiFormatDateDeserializer extends StdDeserializer<Date> {

    public MultiFormatDateDeserializer() {
        this(null);
    }

    public MultiFormatDateDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Date deserialize(JsonParser jsonparser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        String date = jsonparser.getText();
        return MultiFormatDateParser.parse(date);
    }
}