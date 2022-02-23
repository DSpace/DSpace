/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.converter;

import static org.dspace.app.ldn.RdfMediaType.APPLICATION_JSON_LD;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jsonldjava.utils.JsonUtils;
import org.dspace.app.ldn.model.Notification;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JsonLdHttpMessageConverter extends AbstractHttpMessageConverter<Notification> {

    private final ObjectMapper objectMapper;

    public JsonLdHttpMessageConverter() {
        super(APPLICATION_JSON_LD);
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Notification.class.isAssignableFrom(clazz);
    }

    @Override
    protected Notification readInternal(Class<? extends Notification> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (InputStream in = inputMessage.getBody()) {
            return objectMapper.convertValue(JsonUtils.fromInputStream(in), Notification.class);
        }
    }

    @Override
    protected void writeInternal(Notification notification, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try (OutputStreamWriter out = new OutputStreamWriter(outputMessage.getBody())) {
            JsonUtils.write(out, notification);
        }
    }

    public String getRdfType() {
        return "JSON-LD";
    }

}
