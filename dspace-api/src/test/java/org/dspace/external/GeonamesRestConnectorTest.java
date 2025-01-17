/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.UnsupportedEncodingException;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests specifically related to the Geonames REST connector that should accept search and get methods with parameters
 * and return valid JSON results. This test class uses a mock REST connector by default, that loads real examples
 * of Geonames results from disk to cut down on network usage or false negatives due to remote service issues.
 *
 * @author Kim Shepherd
 */
public class GeonamesRestConnectorTest extends AbstractDSpaceTest {

    /**
     * Geonames REST connector
     */
    GeonamesRestConnector geonamesRestConnector;
    /**
     * Configuration service
     */
    ConfigurationService configurationService;

    /**
     * Initialise REST connector and services
     */
    @Before
    public void init() {
        geonamesRestConnector = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("geonamesRestConnector", GeonamesRestConnector.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    /**
     * The connector is present and non-null
     */
    @Test
    public void testValidRestConnector() {
        assertNotNull(geonamesRestConnector);
    }

    /**
     * The connector must return a valid JSON object for a Geonames ID, if it exists
     */
    @Test
    public void testGetById() throws Exception {
        // URLs look like http://api.geonames.org/get?geonameId=6547539&username=<username>
        // Define a partial identifier ("ID")
        String identifier = "6547539";
        int id = Integer.parseInt(identifier);
        String nonexistantIdentifier = "0000000000";

        // Set up the mock HTTP client to intercept and return a response from disk
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        geonamesRestConnector.setHttpClient(httpClient);
        CloseableHttpResponse response = mockResponse("geonames_" + identifier + ".json",
                200, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

        // Get a mock 'by id' result from the mock connector
        String result = geonamesRestConnector.getById(identifier);
        // 1. The input stream should be non-null, and valid JSON
        assertNotNull("InputStream should not be null", result);
        DocumentContext jsonContext = JsonPath.parse(result);
        assertNotNull("parsed DocumentContext should not be null", jsonContext);
        // 2. The JSON should be parseable by jayway JSONPath. We do not test or compare a full model here
        //    as that is not the concern of the REST connector and just results in comparing mocked objects against
        //    each other. For testing of the actual record parsing, see GNDDataProviderTest
        assertNotNull("root JSON path should not be null", jsonContext.read("$"));
        // 3. The id in the JSON should match our expected geonames ID
        assertEquals(identifier, jsonContext.read("$.geonameId").toString());
        // 4. The id in the JSON should NOT match a different geonames ID
        assertNotEquals(nonexistantIdentifier, jsonContext.read("$.geonameId").toString());

        // Attempt to get a non-existant ID. HTTP response testing is not in scope for this test
        // but we do want to test what happens when we get responses for bad IDs
        CloseableHttpResponse badResponse = mockResponse("geonames_" + nonexistantIdentifier + ".json",
                404, "NOT FOUND");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(badResponse);

        result = geonamesRestConnector.getById(nonexistantIdentifier);
        // 5. The input stream should be null
        assertNull("InputStream should be null", result);
        // 6. JsonPath.parse() should throw an illegal argument exception for a null input stream
        // Copy to final for use in below lambda
        String finalResult = result;
        assertThrows(IllegalArgumentException.class, () -> JsonPath.parse(finalResult));

    }

    @Ignore
    @Test
    public void testGetByIdLive() throws Exception {
        // URLs look like http://api.geonames.org/get?geonameId=6547539&username=<username>
        // Define a partial identifier ("ID")
        String identifier = "6547539";
        String nonexistantIdentifier = "0000000000";
        // Get a mock 'by id' result from the mock connector
        String result = geonamesRestConnector.getById(identifier);
        // 1. The input stream should be non-null, and valid JSON
        assertNotNull("InputStream should not be null", result);
        DocumentContext jsonContext = JsonPath.parse(result);
        assertNotNull("parsed DocumentContext should not be null", jsonContext);
        // 2. The JSON should be parseable by jayway JSONPath. We do not test or compare a full model here
        //    as that is not the concern of the REST connector and just results in comparing mocked objects against
        //    each other. For testing of the actual record parsing, see GNDDataProviderTest
        assertNotNull("root JSON path should not be null", jsonContext.read("$"));
        // 3. The id in the JSON should match our expected geonames ID
        assertEquals(identifier, jsonContext.read("$.geonameId").toString());
        // 4. The id in the JSON should NOT match a different geonames ID
        assertNotEquals(nonexistantIdentifier, jsonContext.read("$.geonameId").toString());

        result = geonamesRestConnector.getById(nonexistantIdentifier);
        // 5. The input stream should be null
        assertNull("InputStream should be null", result);
        // 6. JsonPath.parse() should throw an illegal argument exception for a null input stream
        // Copy to final for use in below lambda
        String finalResult = result;
        assertThrows(IllegalArgumentException.class, () -> JsonPath.parse(finalResult));

    }

    /**
     * Mock an HTTP response based on filename so that we can test connector http and response handling when
     * offline and without relying on consistency from the remote system
     *
     * @param filename  resource file (json) to read instead of performing true http get
     * @param statusCode status code to return in status line
     * @param reason reason text to return in status line
     * @return mock HTTP response object
     * @throws UnsupportedEncodingException
     */
    protected CloseableHttpResponse mockResponse(String filename, int statusCode, String reason)
            throws UnsupportedEncodingException {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(this.getClass().getResourceAsStream(filename));
        // Wrap response in mock(), this allows us to intercept some method calls and inject our mocked stuff instead
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine(statusCode, reason));
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

    /**
     * Construct a status line given a status code and reason text
     *
     * @param statusCode status code to return in status line
     * @param reason reason text to return in status line
     * @return status line
     */
    protected StatusLine statusLine(int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

}
