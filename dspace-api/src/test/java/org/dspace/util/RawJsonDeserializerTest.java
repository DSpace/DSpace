/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.Test;

/**
 * Unit tests for {@link RawJsonDeserializer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RawJsonDeserializerTest {

    private String json = ""
        + "{"
        + "    \"attribute\": {"
        + "         \"firstField\":\"value\","
        + "         \"secondField\": 1"
        + "    }"
        + "}";

    @Test
    public void testDeserialization() throws JsonMappingException, JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();

        DeserializationTestClass object = mapper.readValue(json, DeserializationTestClass.class);
        assertThat(object, notNullValue());
        assertThat(object.getAttribute(), is("{\"firstField\":\"value\",\"secondField\":1}"));

    }

    private static class DeserializationTestClass {

        @JsonDeserialize(using = RawJsonDeserializer.class)
        private String attribute;

        public String getAttribute() {
            return attribute;
        }

    }

}
