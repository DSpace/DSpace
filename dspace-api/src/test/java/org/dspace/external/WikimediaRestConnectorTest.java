/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.AbstractDSpaceTest;
import org.dspace.external.model.WikiImageResource;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests specifically related to the Wikimedia REST connector that should accept search and get methods with parameters
 * and return valid JSON results. This test class uses a mock REST connector by default, that loads real examples
 * of Wikimedia results from disk to cut down on network usage or false negatives due to remote service issues.
 *
 * @author Kim Shepherd
 */
public class WikimediaRestConnectorTest extends AbstractDSpaceTest {

    /**
     * Wikimedia REST connector
     */
    WikimediaRestConnector wikimediaRestConnector;
    /**
     * Configuration service
     */
    ConfigurationService configurationService;

    /**
     * Initialise REST connector and services
     */
    @Before
    public void init() {
        wikimediaRestConnector = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("wikimediaRestConnector", WikimediaRestConnector.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    /**
     * The connector is present and non-null
     */
    @Test
    public void testValidRestConnector() {
        assertNotNull(wikimediaRestConnector);
    }

    /**
     * The connector must return a valid JSON object for an image title, if it exists
     */
    @Test
    public void testGetByImageTitle() throws Exception {
        // Example title
        String title = "MarkTwain.LOC.jpg";
        // Expected license
        String license = "Public domain";
        // Expected attribution snippet
        String attributionSnippet = "Unknown author<span style=\"display: none;\">Unknown author</span> | " +
                "<a target='_blank' href='https://commons.wikimedia.org/wiki/File:MarkTwain.LOC.jpg'>" +
                "Wikimedia Commons</a> | Public domain";
        // Bad title
        String badTitle = "NOPE.jpg";

        // Set up the mock HTTP client to intercept and return a response from disk
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        wikimediaRestConnector.setHttpClient(httpClient);
        CloseableHttpResponse response = mockResponse("wikimedia_" + title + ".json", 200, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

        // Get a mock 'by id' result from the mock connector
        WikiImageResource resource =
                WikiImageResource.parseWikimediaResponse(wikimediaRestConnector.getByImageTitle(title));
        // 1. The resulting resource should not be null
        assertNotNull("WikiImageResource should not be null", resource);
        // 2. The resource should have the expected license short name
        assertEquals("License short name should be Public domain", license, resource.getLicenseShortName());
        // 3. The resource should have the expected attribution snippet
        assertEquals("Attribution snippet should be as expected",
                attributionSnippet, resource.getAttributionSnippet());

        // Attempt to get a non-existant ID. HTTP response testing is not in scope for this test
        // but we do want to test what happens when we get responses for bad IDs
        CloseableHttpResponse badResponse = mockResponse("wikimedia_" + badTitle + ".json",
                404, "NOT FOUND");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(badResponse);

        // 4. The attempt to get a non-existant title should result in an IOException
        assertThrows(IOException.class, () -> wikimediaRestConnector.getByImageTitle(badTitle));

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
