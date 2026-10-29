/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openpolicyfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.AbstractDSpaceTest;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderPublisherResponse;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for Open Policy Finder service
 * @author Kim Shepherd
 * @see MockOpenPolicyFinderService
 */
public class OpenPolicyFinderServiceTest extends AbstractDSpaceTest {

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    // Spring Open Policy Finder service. For testing purposes, this will use a mock service
    // that doesn't perform actual HTTP queries but does construct URIs
    // and returns a valid response (The Lancet) if no other errors are encountered
    protected OpenPolicyFinderService openPolicyFinderService = DSpaceServicesFactory.getInstance()
        .getServiceManager().getServiceByName(
            "org.dspace.app.openpolicyfinder.MockOpenPolicyFinderService",
            MockOpenPolicyFinderService.class);

    public OpenPolicyFinderServiceTest() {

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
     * Test searching by Journal ISSN directly against Open Policy Finder service
     */
    @Test
    public void testSearchByJournalISSN() {
        // Get a response with a single valid ISSN, using the mock service which will return a response based on
        // thelancet.json stored response in test resources
        String validISSN = "0140-6736";
        OpenPolicyFinderResponse opfResponse = openPolicyFinderService.searchByJournalISSN(validISSN);

        // This response should NOT contain an error (isError() should be false)
        assertFalse("Response contained an error flag / message: " + opfResponse.getMessage(),
            opfResponse.isError());

        // This response should contain a single journal called The Lancet
        String expectedTitle = "The Lancet";
        assertTrue("Response did not contain a journal with the expected title '" + expectedTitle + '"',
            expectedTitle.equals(opfResponse.getJournals().get(0).getTitles().get(0)));
    }

    /**
     * Test that the URIBuilder and sanitation procedures are producing expected URLs, comparing the results
     * to manually compiled strings. Note: the API key is now sent as an HTTP header, not as a query parameter.
     * @throws URISyntaxException
     */
    @Test
    public void testUriConstruction() throws URISyntaxException, UnsupportedEncodingException {
        // Get values for base URL (API key is now a header, not in the URL)
        String endpoint = configurationService.getProperty("openpolicyfinder.url",
            configurationService.getProperty("sherpa.romeo.url",
                "https://api.openpolicyfinder.jisc.ac.uk/retrieve"));

        // Compare expected outputs
        // Valid ISSN (The Lancet)
        String validISSN = "0140-6736";
        // Invalid ISSN that also contains characters we strip out in sanitisation
        String invalidISSN = "{TEST}";

        // Characters like { and } that conflict with JSON should be stripped from the filter query
        assertEquals("JSON filter query sanitisation not stripping special characters",
            "TEST", OpenPolicyFinderUtils.sanitiseQuery(invalidISSN));

        // The valid string should look like this (assuming default configuration)
        // https://api.openpolicyfinder.jisc.ac.uk/retrieve?item-type=publication&...&format=Json
        String validUrl = new URIBuilder(buildUrlString(validISSN, endpoint)).toString();
        assertEquals("Built and expected valid URLs differ", validUrl,
            openPolicyFinderService.constructHttpGet("publication", "issn", "equals", validISSN)
                .getURI().toASCIIString());

        // The invalid string should look like this (assuming default configuration)
        // https://api.openpolicyfinder.jisc.ac.uk/retrieve?item-type=publication&...&format=Json
        // Note - it should return 0 results from the API, but these services are not intended to validate the ISSN
        // query, though they do sanitise it for the JSON input type, hence expecting the braces to be stripped
        String invalidUrl = new URIBuilder(buildUrlString(invalidISSN, endpoint)).toString();
        assertEquals("Built and expected invalid URLs differ", invalidUrl,
            openPolicyFinderService.constructHttpGet("publication", "issn", "equals", invalidISSN)
                .getURI().toASCIIString());


        // The null query string should look like this (assuming default configuration)
        // https://api.openpolicyfinder.jisc.ac.uk/retrieve?item-type=publication&...&format=Json
        // Note - it should return 0 results from the API, but all we do is log a warning, this is not considered
        // a fatal URI syntax exception (the remote call does work, and returns 0 items as valid JSON)
        String nullUrl = new URIBuilder(buildUrlString(null, endpoint)).toString();
        assertEquals("Built and expected invalid URLs differ", nullUrl,
            openPolicyFinderService.constructHttpGet("publication", "issn", "equals", null)
                .getURI().toASCIIString());

    }

    /**
     * Test that {@link OpenPolicyFinderService#prepareQuery(String, String, String)} fails fast when the
     * query is {@code null} instead of building a filter containing the literal "null".
     */
    @Test
    public void testPrepareQueryFailsFastOnNullQuery() {
        String endpoint = configurationService.getProperty("openpolicyfinder.url",
            configurationService.getProperty("sherpa.romeo.url",
                "https://api.openpolicyfinder.jisc.ac.uk/retrieve"));
        try {
            openPolicyFinderService.prepareQuery(null, endpoint, null);
            fail("Expected a URISyntaxException when the query is null");
        } catch (URISyntaxException e) {
            // expected: no filter containing the literal "null" should be built
            assertTrue("Unexpected reason: " + e.getReason(),
                e.getReason().contains("No ISSN supplied"));
        }
    }

    /**
     * Test that {@link OpenPolicyFinderService#prepareQuery(String, String, String)} fails fast when the
     * query is blank.
     */
    @Test
    public void testPrepareQueryFailsFastOnBlankQuery() {
        String endpoint = configurationService.getProperty("openpolicyfinder.url",
            configurationService.getProperty("sherpa.romeo.url",
                "https://api.openpolicyfinder.jisc.ac.uk/retrieve"));
        try {
            openPolicyFinderService.prepareQuery("   ", endpoint, null);
            fail("Expected a URISyntaxException when the query is blank");
        } catch (URISyntaxException e) {
            assertTrue("Unexpected reason: " + e.getReason(),
                e.getReason().contains("No ISSN supplied"));
        }
    }

    /**
     * Test that the grand total {@code totalHits} is parsed from the API response and that the items
     * array is parsed alongside it (page 1 of the real "test" search).
     */
    @Test
    public void testTotalHitsParsedFromResponse() {
        OpenPolicyFinderResponse response = openPolicyFinderService.performRequest(
            "publication", "title", "contains-word", "test_page1", 0, 3);

        assertFalse("Response was flagged as 'isError'", response.isError());
        assertEquals("totalHits was not parsed from the response", 4, response.getTotalHits());
        assertTrue("Expected three parsed journals on page 1",
            CollectionUtils.isNotEmpty(response.getJournals()) && response.getJournals().size() == 3);
        assertEquals("First journal title did not match", "TEST",
            response.getJournals().get(0).getTitles().get(0));
        assertEquals("First journal ISSN did not match", "1133-0686",
            response.getJournals().get(0).getIssns().get(0));
    }

    /**
     * Test that {@code next_search_after} is parsed: present on page 1 (more pages) and null on the
     * last page, while {@code totalHits} stays constant across pages.
     */
    @Test
    public void testNextSearchAfterParsed() {
        OpenPolicyFinderResponse page1 = openPolicyFinderService.performRequest(
            "publication", "title", "contains-word", "test_page1", 0, 3);
        assertEquals("next_search_after cursor did not match on page 1", "[37137]",
            page1.getNextSearchAfter());

        OpenPolicyFinderResponse page2 = openPolicyFinderService.performRequest(
            "publication", "title", "contains-word", "test_page2", 0, 3);
        assertTrue("next_search_after should be null on the last page",
            page2.getNextSearchAfter() == null);
        assertEquals("totalHits should be constant across pages", 4, page2.getTotalHits());
        assertTrue("Expected a single journal on page 2",
            CollectionUtils.isNotEmpty(page2.getJournals()) && page2.getJournals().size() == 1);
        assertEquals("Page 2 journal title did not match", "Journal Test Record 1",
            page2.getJournals().get(0).getTitles().get(0));
    }

    /**
     * Test that {@code totalHits} defaults to the parsed item count when the API response does not
     * include the field (protects legacy fixtures).
     */
    @Test
    public void testTotalHitsDefaultsToItemCountWhenMissing() {
        // thelancet.json now carries totalHits=1 which equals its item count; assert consistency
        OpenPolicyFinderResponse response = openPolicyFinderService.searchByJournalISSN("0140-6736");
        assertFalse("Response was flagged as 'isError'", response.isError());
        assertEquals("totalHits should equal the parsed item count",
            response.getJournals().size(), response.getTotalHits());
    }

    /**
     * Test that the optional {@code search_after} cursor is appended to the constructed URI, and that
     * the default (no-cursor) contract is unchanged.
     * @throws URISyntaxException if the URL build fails
     */
    @Test
    public void testSearchAfterAppendedToUri() throws URISyntaxException {
        String cursorUri = openPolicyFinderService
            .constructHttpGet("publication", "issn", "equals", "0140-6736", 0, 1, "[37137]")
            .getURI().toASCIIString();
        assertTrue("search_after cursor was not appended to the URI",
            cursorUri.contains("search_after=%5B37137%5D"));

        // Default path (blank cursor) must not add a search_after parameter
        String defaultUri = openPolicyFinderService
            .constructHttpGet("publication", "issn", "equals", "0140-6736", 0, 1)
            .getURI().toASCIIString();
        assertFalse("Default URI must not contain a search_after parameter",
            defaultUri.contains("search_after"));
        assertTrue("Default URI contract changed unexpectedly",
            defaultUri.endsWith("&format=Json&offset=0&limit=1"));
    }

    /**
     * Thorough test of returned OpenPolicyFinderResponse object to ensure all expected fields are there and valid
     */
    @Test
    public void testJournalResponse() {
        // Valid ISSN (The Lancet)
        String validISSN = "0140-6736";
        OpenPolicyFinderResponse response = openPolicyFinderService.searchByJournalISSN(validISSN);

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
        String expectedSystemMetadataUri = "https://openpolicyfinder.jisc.ac.uk/id/publication/23803";
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
     * Thorough test of returned OpenPolicyFinderPublisherResponse object to ensure all expected fields are there
     * and valid
     */
    @Test
    public void testPublisherResponse() {
        // Set up basic query and query the (mock) service
        String publisherName = "Public Library of Science";
        OpenPolicyFinderPublisherResponse response = openPolicyFinderService.performPublisherRequest(
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
     * Build URL manually with string builder to compare to URIBuilder usage in actual service.
     * Note: the API key is now sent as an HTTP header, not as a query parameter.
     * @param query
     * @param endpoint
     * @return
     */
    public static String buildUrlString(String query, String endpoint) {
        query = OpenPolicyFinderUtils.sanitiseQuery(query);
        StringBuilder expected = new StringBuilder();
        String filter = "[[\"issn\",\"equals\",\"" + query + "\"]]";
        expected.append(endpoint).append("?")
            .append("item-type=publication&filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8))
            .append("&format=Json&offset=0&limit=1");
        return expected.toString();
    }

}
