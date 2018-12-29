/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MetadataPatchSuite {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonNode suite;

    public MetadataPatchSuite() throws Exception {
        suite = objectMapper.readTree(getClass().getResourceAsStream("metadata-patch-suite.json"));
    }

    public void runWith(MockMvc client, String url, int expectedStatus) {
        for (JsonNode testNode: suite.get("tests")) {
            String requestBody = testNode.get("patch").toString();
            String expectedMetadata = testNode.get("expect").toString();
            try {
                System.out.println("Running patch test: " + testNode.get("name") + "\nRequest: " + requestBody);
                checkResponse("PATCH", client, patch(url).content(requestBody), expectedMetadata, expectedStatus);
                if (expectedStatus >= 200 && expectedStatus < 300) {
                    checkResponse("GET", client, get(url), expectedMetadata, expectedStatus);
                }
            } catch (Throwable t) {
                Assert.fail("Metadata patch test '" + testNode.get("name") + "' failed.\n" + "Request body: "
                        + requestBody + "\n" + "Error: " + (t instanceof AssertionError ? "" : t.getClass().getName())
                        + t.getMessage());
            }
        }
    }

    private void checkResponse(String verb, MockMvc client, MockHttpServletRequestBuilder requestBuilder,
                               String expectedMetadata, int expectedStatus) throws Exception {
        ResultActions resultActions = client.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus >= 200 && expectedStatus < 300) {
          String responseBody = resultActions.andReturn().getResponse().getContentAsString();
          JsonNode responseJson =  objectMapper.readTree(responseBody);
          String responseMetadata = responseJson.get("metadata").toString();
          if (!responseMetadata.equals(expectedMetadata)) {
              Assert.fail("Expected metadata in " + verb + " response: " + expectedMetadata
                      + "\nGot metadata in " + verb + " response: " + responseMetadata);
          }
        }
    }
}
