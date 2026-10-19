/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class InvertedIndexProcessorTest {

    private InvertedIndexProcessor processor;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        processor = new InvertedIndexProcessor();
        processor.setPath("/index");
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGetStringValue_NullNode_ReturnsEmptyString() {
        assertEquals("", processor.getStringValue(null));
    }

    @Test
    public void testGetStringValue_EmptyNode_ReturnsEmptyString() throws IOException {
        JsonNode emptyNode = objectMapper.readTree("{}");
        assertEquals("", processor.getStringValue(emptyNode));
    }

    @Test
    public void testGetStringValue_MissingPath_ReturnsEmptyString() throws IOException {
        JsonNode jsonNode = objectMapper.readTree("{\"wrongPath\": { \"1\": \"word\" }}");
        assertEquals("", processor.getStringValue(jsonNode));
    }

    @Test
    public void testGetStringValue_InvalidStructure_ReturnsEmptyString() throws IOException {
        JsonNode jsonNode = objectMapper.readTree("{\"index\": \"not an object or array\"}");
        assertEquals("", processor.getStringValue(jsonNode));
    }

    @Test
    public void testGetStringValue_ValidJson_ReturnsOrderedString() throws IOException {
        String json = "{ \"index\": { \"word1\": [2, 5], \"word2\": [1], \"word3\": [4] } }";
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("word2 word1 word3 word1", processor.getStringValue(jsonNode));
    }

    @Test
    public void testGetStringValue_UnorderedJson_ReturnsCorrectlyOrderedString() throws IOException {
        String json = "{ \"index\": { \"apple\": [3], \"banana\": [1], \"cherry\": [2] } }";
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("banana cherry apple", processor.getStringValue(jsonNode));
    }

}
