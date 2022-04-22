/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Test;

/**
 *
 */
public class NotificationTest {

    public static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    @Test
    public void testNotificationFromDataverse() throws IllegalArgumentException, IOException {
        Notification notification = read("src/test/resources/mocks/fromDataverse.json");
        String[] context = notification.getC();
        assertEquals(2, context.length);
        assertEquals("https://purl.org/coar/notify", context[0]);
        assertEquals("https://www.w3.org/ns/activitystreams", context[1]);
    }

    @Test
    public void testNotificationToDataverse() throws IllegalArgumentException, IOException {
        Notification notification = read("src/test/resources/mocks/toDataverse.json");
        String[] context = notification.getC();
        assertEquals(2, context.length);
        assertEquals("https://purl.org/coar/notify", context[0]);
        assertEquals("https://www.w3.org/ns/activitystreams", context[1]);
    }

    private Notification read(String relativeToRootFilepath) throws IllegalArgumentException, IOException {
        try (FileInputStream fis = new FileInputStream(new File(relativeToRootFilepath))) {
            return objectMapper.convertValue(JsonUtils.fromInputStream(fis), Notification.class);
        }
    }

}
