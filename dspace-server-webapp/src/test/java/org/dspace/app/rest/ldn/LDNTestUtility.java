/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.ldn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jsonldjava.utils.JsonUtils;
import org.dspace.app.ldn.model.Notification;

public class LDNTestUtility {

    public static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    private LDNTestUtility() {

    }

    public static String toJson(Notification notification) throws IOException {
        return JsonUtils.toString(notification);
    }

    public static String loadJson(String relativeToRootFilepath) throws IllegalArgumentException, IOException {
        Notification notification = load(relativeToRootFilepath);
        return JsonUtils.toString(notification);
    }

    public static Notification load(String relativeToRootFilepath) throws IllegalArgumentException, IOException {
        try (FileInputStream fis = new FileInputStream(new File(relativeToRootFilepath))) {
            return objectMapper.convertValue(JsonUtils.fromInputStream(fis), Notification.class);
        }
    }

}
