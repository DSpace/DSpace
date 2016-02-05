/*
 */
package org.datadryad.rest.providers;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONFormatProvider implements ContextResolver<ObjectMapper> {
    private ObjectMapper objectMapper;

    public JSONFormatProvider() throws Exception {
        this.objectMapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        this.objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .setDateFormat(df);
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return objectMapper;
    }
}