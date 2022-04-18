/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.AbstractDSpaceTest;
import org.dspace.app.sherpa.v2.SHERPAPublisherResponse;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.app.sherpa.v2.SHERPAUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for SHERPA service
 * @author Kim Shepherd
 * @see MockSHERPAService
 */
public class SHERPAServiceTest extends AbstractDSpaceTest {

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    // Spring SHERPA service. For testing purposes, this will use a mock service
    // that doesn't perform actual HTTP queries but does construct URIs
    // and returns a valid response (The Lancet) if no other errors are encountered
    protected SHERPAService sherpaService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName("org.dspace.app.sherpa.MockSHERPAService", MockSHERPAService.class);

    public SHERPAServiceTest() {

    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test searching by Journal ISSN directly against SHERPA service
     */
    @Test
    public void testSearchByJournalISSN() {
        // Get a response with a single valid ISSN, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        String validISSN = "0140-6736";
        SHERPAResponse sherpaResponse = sherpaService.searchByJournalISSN(validISSN);

        // This response should NOT contain an error (isError() should be false)
        assertFalse("Response contained an error flag / message: " + sherpaResponse.getMessage(),
            sherpaResponse.isError());

        // This response should contain a single journal called The Lancet
        String expectedTitle = "The Lancet";
        assertTrue("Response did not contain a journal with the expected title '" + expectedTitle + '"',
            expectedTitle.equals(sherpaResponse.getJournals().get(0).getTitles().get(0)));
    }

    /**
     * Test that the URIBuilder and sanitation procedures are producing expected URLs, comparing the results
     * to manually compiled strings
     * @throws URISyntaxException
     */
    @Test
    public void testUriConstruction() throws URISyntaxException, UnsupportedEncodingException {
        // Get values for base URL and api key parameter
        String endpoint = configurationService.getProperty("sherpa.romeo.url",
            "https://v2.sherpa.ac.uk/cgi/retrieve");
        String apiKey = configurationService.getProperty("sherpa.romeo.apikey", null);

        // Compare expected outputs
        // Valid ISSN (The Lancet)
        String validISSN = "0140-6736";
        // Invalid ISSN that also contains characters we strip out in sanitisation
        String invalidISSN = "{TEST}";

        // Characters like { and } that conflict with JSON should be stripped from the filter query
        assertEquals("JSON filter query sanitisation not stripping special characters",
            "TEST", SHERPAUtils.sanitiseQuery(invalidISSN));

        // The valid string should look like this (assuming default configuration)
        // https://v2.sherpa.ac.uk/cgi/retrieve?item-type=publication&filter=[["issn","equals","0140-6736"]]&format=Json
        String validUrl = new URIBuilder(buildUrlString(validISSN, endpoint, apiKey)).toString();
        assertEquals("Built and expected valid URLs differ", validUrl,
            sherpaService.constructHttpGet("publication", "issn", "equals", validISSN)
                .getURI().toASCIIString());

        // The invalid string should look like this (assuming default configuration)
        // https://v2.sherpa.ac.uk/cgi/retrieve?item-type=publication&filter=[["issn","equals","TEST"]]&format=Json
        // Note - it should return 0 results from the API, but these services are not intended to validate the ISSN
        // query, though they do sanitise it for the JSON input type, hence expecting the braces to be stripped
        String invalidUrl = new URIBuilder(buildUrlString(invalidISSN, endpoint, apiKey)).toString();
        assertEquals("Built and expected invalid URLs differ", invalidUrl,
            sherpaService.constructHttpGet("publication", "issn", "equals", invalidISSN)
                .getURI().toASCIIString());


        // The null query string should look like this (assuming default configuration)
        // https://v2.sherpa.ac.uk/cgi/retrieve?item-type=publication&filter=[["issn","equals",""]]&format=Json
        // Note - it should return 0 results from the API, but all we do is log a warning, this is not considered
        // a fatal URI syntax exception (the remote call does work, and returns 0 items as valid JSON)
        String nullUrl = new URIBuilder(buildUrlString(null, endpoint, apiKey)).toString();
        assertEquals("Built and expected invalid URLs differ", nullUrl,
            sherpaService.constructHttpGet("publication", "issn", "equals", null)
                .getURI().toASCIIString());

    }

    /**
     * Thorough test of returned SHERPAResponse object to ensure all expected fields are there and valid
     */
    @Test
    public void testJournalResponse() {
        // Valid ISSN (The Lancet)
        String validISSN = "0140-6736";
        SHERPAResponse response = sherpaService.searchByJournalISSN(validISSN);

        // Assert response is not error, or fail with message
        assertFalse("Response was flagged as 'isError'", response.isError());

        // Assert response has at least one journal result, or fail with message
        assertTrue("List of journals did not contain at least one parsed journal",
            CollectionUtils.isNotEmpty(response.getJournals()));

        // Assert response has a journal with title "The Lancet", or fail with message
        String expectedTitle = "The Lancet";
        assertTrue("Journal title did not match expected '" + expectedTitle + "' value",
            CollectionUtils.isNotEmpty(response.getJournals().get(0).getTitles())
                && expectedTitle.equals(response.getJournals().get(0).getTitles().get(0)));

        // Assert response has expected publication (metadata) URI
        String expectedSystemMetadataUri = "http://v2.sherpa.ac.uk/id/publication/23803";
        assertTrue("Response metadata URI did not match expected '" + expectedSystemMetadataUri
            + "' value", expectedSystemMetadataUri.equals(response.getMetadata().getUri()));

        // Assert response has at least one policy
        assertTrue("Response did not contain at least one archiving policy",
            CollectionUtils.isNotEmpty(response.getJournals().get(0).getPolicies()));

        // Assert response has at least one permitted version
        assertTrue("Response did not contain at least one permitted version",
            CollectionUtils.isNotEmpty(response.getJournals().get(0).getPolicies().get(0).getPermittedVersions()));

        // Assert journal has at least one publisher
        assertTrue("Response did not contain at least one publisher",
            CollectionUtils.isNotEmpty(response.getJournals().get(0).getPublishers()));

        // Assert first publisher has name 'Elsevier'
        String expectedPublisherName = "Elsevier";
        assertTrue("Response did not contain expected publisher name '" + expectedPublisherName + "'",
            expectedPublisherName.equals(response.getJournals().get(0).getPublisher().getName()));
    }

    /**
     * Thorough test of returned SHERPAPublisherResponse object to ensure all expected fields are there and valid
     */
    @Test
    public void testPublisherResponse() {
        // Set up basic query and query the (mock) service
        String publisherName = "Public Library of Science";
        SHERPAPublisherResponse response = sherpaService.performPublisherRequest(
            "publisher", "name", "equals", publisherName, 0, 1);

        // Assert response is not error, or fail with message
        assertFalse("Response was flagged as 'isError'", response.isError());

        // Assert response has at least one publisher result, or fail with message
        assertTrue("List of publishers did not contain at least one parsed publisher",
            CollectionUtils.isNotEmpty(response.getPublishers()));

        // Assert response has a publisher with name "Public Library of Science", or fail with message
        String expectedName = "Public Library of Science";
        assertEquals("Publisher name did not match expected '" + expectedName + "' value",
            expectedName, response.getPublishers().get(0).getName());

        // Assert response has expected publisher URL
        String expectedUrl = "http://www.plos.org/";
        assertEquals("Response metadata URI did not match expected '" + expectedUrl
            + "' value", expectedUrl, response.getPublishers().get(0).getUri());

        // Assert response has at expected publisher ID
        String expectedId = "112";
        assertEquals("Response publisher ID did not match expected ID " + expectedId,
            expectedId, response.getPublishers().get(0).getIdentifier());
    }

    /**
     * Build URL manually with string builder to compare to URIBuilder usage in actual service
     * @param query
     * @param endpoint
     * @param apiKey
     * @return
     */
    public static String buildUrlString(String query, String endpoint, String apiKey) {
        query = SHERPAUtils.sanitiseQuery(query);
        StringBuilder expected = new StringBuilder();
        String filter = "[[\"issn\",\"equals\",\"" + query + "\"]]";
        expected.append(endpoint).append("?")
            .append("item-type=publication&filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8))
            .append("&format=Json&offset=0&limit=1");
        if (StringUtils.isNotBlank(apiKey)) {
            expected.append("&api-key=").append(apiKey);
        }
        return expected.toString();
    }

}
