/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;

/**
 * Test the TikaServerTextExtractionFilter using test files for all major formats.
 * The test files used below are all located at [dspace-api]/src/test/resources/org/dspace/app/mediafilter/
 *
 * @author mwood
 * @author Tim Donohue
 */

public class TikaServerTextExtractionFilterTest extends AbstractUnitTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    private MockServerClient mockServerClient;

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Test of getDestinationStream method when max characters is less than file size
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithMaxChars()
        throws Exception {
        TikaServerTextExtractionFilter instance = new TikaServerTextExtractionFilter();

        // Set up mock Tika Server
        char[] bupher = new char[100];
        try (
            Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("test.txt"));) {
            reader.read(bupher);
        }
        mockServerClient
                .when(request().withMethod("POST").withPath("/tika"))
                .respond(response().withStatusCode(200).withBody(new String(bupher)));

        // Set "max-chars" to a small value of 100 chars, which is less than the text size of the file.
        configurationService.setProperty("textextractor.max-chars", "100");

        // Set Tika server URL.
        mockServerClient.remoteAddress();
        mockServerClient.getPort();
        configurationService.setProperty("textextractor.tika.server.url",
                mockServerClient.remoteAddress().toString());

        // Test!
        InputStream source = this.getClass().getResourceAsStream("test.txt");
        InputStream result = instance.getDestinationStream(null, source, false);
        String extractedText = readAll(result);

        // Verify we have exactly the first 100 characters
        assertEquals(100, extractedText.length());
        // Verify it has some text at the beginning of the file, but NOT text near the end
        assertTrue("Known beginning content was found", extractedText.contains("This is a text."));
        assertFalse("Known ending content was not found", extractedText.contains("Emergency Broadcast System"));
    }

    /**
     * Test of getDestinationStream method using a PDF document
     * Read a constant .pdf document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithPDF()
        throws Exception {
        TikaServerTextExtractionFilter instance = new TikaServerTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.pdf");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue("Known content was not found in .pdf", readAll(result).contains("quick brown fox"));
    }

    /**
     * Read the entire content of a stream into a String.
     *
     * @param stream a stream of UTF-8 characters.
     * @return complete content of stream as a String
     * @throws IOException
     */
    private static String readAll(InputStream stream)
        throws IOException {
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }
}
