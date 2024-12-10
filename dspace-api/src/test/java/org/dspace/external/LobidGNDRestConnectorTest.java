/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

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

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests specifically related to the REST connector that should accept search and get methods with parameters
 * and return valid JSON results. This test class uses a mock REST connector by default, that loads real examples
 * of LOBID results from disk to cut down on network usage or false negatives due to remote service issues.
 *
 * @author Kim Shepherd
 */
public class LobidGNDRestConnectorTest extends AbstractDSpaceTest {

    /**
     * LOBID GND REST connector
     */
    LobidGNDRestConnector gndRestConnector;
    /**
     * Configuration service
     */
    ConfigurationService configurationService;

    /**
     * Initialise REST connector and services
     */
    @Before
    public void init() {
        gndRestConnector = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("lobidGNDRestConnector", LobidGNDRestConnector.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    /**
     * The connector is present and non-null
     */
    @Test
    public void testValidRestConnector() {
        assertNotNull(gndRestConnector);
    }


    /**
     * The connector must correctly format GND DNB identifiers, given input in various (partial) formats
     * TODO: does this belong instaed in GNDUtilsTest?
     * @throws Exception
     */
    @Test
    public void testFormatIdentifier() throws Exception {
        // Define a partial identifier ("ID")
        String partialIdentifier = "4074335-4";
        // Define the URI by which we retrieve an object from the LOBID API
        String objectUri = "https://lobid.org/gnd/" + partialIdentifier + ".json";
        // Define the official DNB URI for a GND record
        String uri = "https://d-nb.info/gnd/" + partialIdentifier;
        // Our expected value is https://d-nb.info/gnd/4074335-4
        String expected = configurationService.getProperty("gnd.uri.prefix", "https://d-nb.info/gnd/")
                + partialIdentifier;

        // 1. Test identifier formatting from partial ID
        String identifier = GNDUtils.formatURI(partialIdentifier);
        assertEquals(expected, identifier);

        // 2. Test identifier formatting from object ID (https://lobid.org/gnd/4074335-4.json)
        identifier = GNDUtils.formatURI(objectUri);
        assertEquals(expected, identifier);

        // 3. Test identifier formatting from official URI (ie no change) (https://d-nb.info/gnd/4074335-4)
        identifier = GNDUtils.formatURI(uri);
        assertEquals(expected, identifier);

        // 4. Test a failed formatting attempt from a random URL
        identifier = GNDUtils.formatURI("https://lib-co.de/fail/4074335-4");
        assertNotEquals(expected, identifier);

        // 5. Test a null identifier. In this case, we should see an IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> GNDUtils.formatURI(null));
    }

    /**
     * The connector must correctly format LOBID API object URIs given input in various (partial) formats
     * TODO: does this belong instead in GNDUtilsTest?
     * @throws Exception
     */
    @Test
    public void testFormatObjectUri() throws Exception {
        // Define a partial identifier ("ID")
        String partialIdentifier = "4074335-4";
        // Define the URI by which we retrieve an object from the LOBID API
        String objectUri = "https://lobid.org/gnd/" + partialIdentifier + ".json";
        // Define the official DNB URI for a GND record
        String uri = "https://d-nb.info/gnd/" + partialIdentifier;
        // Our expected value is https://lobid.org/gnd/4074335-4.json
        String expected = configurationService.getProperty("gnd.api.url", "https://lobid.org/gnd/")
                + partialIdentifier + ".json";

        // 1. Test identifier formatting from partial ID
        String identifier = GNDUtils.formatObjectURI("4074335-4");
        assertEquals(expected, identifier);

        // 2. Test identifier formatting from object ID (https://lobid.org/gnd/4074335-4.json)
        identifier = GNDUtils.formatObjectURI(objectUri);
        assertEquals(expected, identifier);

        // 3. Test identifier formatting from DNB URI (https://d-nb.info/gnd/4074335-4)
        identifier = GNDUtils.formatObjectURI(uri);
        assertEquals(expected, identifier);

        // 4. Test a failed formatting attempt from a random URL
        identifier = GNDUtils.formatURI("https://lib-co.de/gnd/4074335-4");
        assertNotEquals(expected, identifier);

        // 5. Test a null identifier. In this case, we should see an IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> GNDUtils.formatObjectURI(null));
    }

    /**
     * The connector must return a valid JSON object for a GND ID, if it exists
     * TODO: Link to a specification / story / functional requirement?!
     * XXX. The LOBID REST connector must successfully retrieve and parse a JSON result given an identifier
     */
    @Test
    public void testGetById() throws Exception {
        // The London mock record is "gnd_4074335-4.json" with a length of 0
        String id = "https://d-nb.info/gnd/4074335-4";
        String anotherId = "https://d-nb.info/gnd/118540238";
        String nonexistantId = "https://d-nb.info/gnd/000000000000";
        // Set up the mock HTTP client to intercept and return a response from disk
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        gndRestConnector.setHttpClient(httpClient);
        CloseableHttpResponse response = mockResponse("gnd_" +
                GNDUtils.extractIdentifier(id) + ".json", 200, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

        // Get a mock 'by id' result from the mock connector
        String result = gndRestConnector.getById(id);
        // 1. The input stream should be non-null, and valid JSON
        assertNotNull("InputStream should not be null", result);
        DocumentContext jsonContext = JsonPath.parse(result);
        assertNotNull("parsed DocumentContext should not be null", jsonContext);
        // 2. The JSON should be parseable by jayway JSONPath. We do not test or compare a full model here
        //    as that is not the concern of the REST connector and just results in comparing mocked objects against
        //    each other. For testing of the actual record parsing, see GNDDataProviderTest
        assertNotNull("root JSON path should not be null", jsonContext.read("$"));
        // 3. The id in the JSON should match our expected DNB URI
        assertEquals(id, jsonContext.read("$.id"));
        // 4. The id in the JSON should NOT match a different DNB URI
        assertNotEquals(anotherId, jsonContext.read("$.id"));

        // Attempt to get a non-existant ID. HTTP response testing is not in scope for this test
        // but we do want to test what happens when we get responses for bad IDs
        CloseableHttpResponse badResponse = mockResponse("gnd_" +
                GNDUtils.extractIdentifier(nonexistantId) + ".json", 404, "NOT FOUND");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(badResponse);

        result = gndRestConnector.getById(nonexistantId);
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
        // The London mock record is "gnd_4074335-4.json" with a length of 0
        String id = "https://d-nb.info/gnd/4074335-4";
        String anotherId = "https://d-nb.info/gnd/118540238";
        String nonexistantId = "https://d-nb.info/gnd/000000000000";
        // Get a mock 'by id' result from the mock connector
        String result = gndRestConnector.getById(id);
        // 1. The input stream should be non-null, and valid JSON
        assertNotNull("InputStream should not be null", result);
        DocumentContext jsonContext = JsonPath.parse(result);
        assertNotNull("parsed DocumentContext should not be null", jsonContext);
        // 2. The JSON should be parseable by jayway JSONPath. We do not test or compare a full model here
        //    as that is not the concern of the REST connector and just results in comparing mocked objects against
        //    each other. For testing of the actual record parsing, see GNDDataProviderTest
        assertNotNull("root JSON path should not be null", jsonContext.read("$"));
        // 3. The id in the JSON should match our expected DNB URI
        assertEquals(id, jsonContext.read("$.id"));
        // 4. The id in the JSON should NOT match a different DNB URI
        assertNotEquals(anotherId, jsonContext.read("$.id"));

        // Attempt to get a non-existant ID. HTTP response testing is not in scope for this test
        // but we do want to test what happens when we get responses for bad IDs
        result = gndRestConnector.getById(nonexistantId);
        // 5. The input stream should be null
        assertNull("InputStream should be null", result);
        // 6. JsonPath.parse() should throw an illegal argument exception for a null input stream
        // Copy to new final for use in lambda below
        String finalResult = result;
        assertThrows(IllegalArgumentException.class, () -> JsonPath.parse(finalResult));

    }

    /**
     * The connector must return a valid input stream containing JSON
     * TODO: Link to a specification / story / functional requirement
     */
    @Test
    public void testSearch() throws Exception {
        // The London mock data is gnd_search_london.json with length of 0
        String expectedId = "https://lobid.org/gnd/search?q=london&from=0&size=2&format=json";
        String query = "london";
        // Set up the mock HTTP client to intercept and return a response from disk
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        gndRestConnector.setHttpClient(httpClient);
        CloseableHttpResponse response = mockResponse("gnd_search_" + query + ".json", 200, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
        String result = gndRestConnector.search("london", 0, 2, null, null, "json");
        // 1. The input stream should be valid JSON
        DocumentContext jsonContext = JsonPath.parse(result);
        assertNotNull("Parsed JSON context should not be null", jsonContext);
        // 2. The JSON should be parseable by jayway JSONPath. We do not test or compare a full model here
        //    as that is not the concern of the REST connector and just results in comparing mocked objects against
        //    each other. For testing of the actual record parsing, see GNDDataProviderTest
        assertEquals(expectedId, jsonContext.read("$.id"));
        // 3. There should be 0 results
        assertEquals(Integer.valueOf(81249), jsonContext.read("$.totalItems"));

        // Attempt to search for a query that finds no results.
        String expectedEmptyId = "https://lobid.org/gnd/search?q=badsearch&from=0&size=2&format=json";
        CloseableHttpResponse badResponse = mockResponse("gnd_search_empty.json", 200, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(badResponse);

        result = gndRestConnector.getById("badsearch");
        // 4. The input stream should NOT be null
        assertNotNull("InputStream should not be null", result);
        jsonContext = JsonPath.parse(result);
        // 5. The parsed content should not be null
        assertNotNull("Parsed JSON context should not be null", jsonContext);
        // 6. The id should match the expected string
        assertEquals(expectedEmptyId, jsonContext.read("$.id"));
        // 7. There should be 0 results
        assertEquals(Integer.valueOf(0), jsonContext.read("$.totalItems"));

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
