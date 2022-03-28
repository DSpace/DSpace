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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Drive the POI-based MS Word filter.
 *
 * @author mwood
 */
public class TikaTextExtractionFilterTest extends AbstractUnitTest {

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Test of getDestinationStream method using temp file for text extraction
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithUseTempFile()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        // Extract text from file with "use-temp-file=true"
        configurationService.setProperty("textextractor.use-temp-file", "true");
        InputStream source = getClass().getResourceAsStream("test.pdf");
        InputStream result = instance.getDestinationStream(null, source, false);
        String tempFileExtractedText = readAll(result);

        // Verify text extracted successfully
        assertTrue("Known content was not found in .pdf", tempFileExtractedText.contains("quick brown fox"));

        // Now, extract text from same file using default, in-memory
        configurationService.setProperty("textextractor.use-temp-file", "false");
        source = getClass().getResourceAsStream("test.pdf");
        result = instance.getDestinationStream(null, source, false);
        String inMemoryExtractedText = readAll(result);

        // Verify the two results are equal
        assertEquals("Extracted text via temp file is the same as in-memory.",
                     inMemoryExtractedText, tempFileExtractedText);
    }

    /**
     * Test of getDestinationStream method when max characters is less than file size
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithMaxChars()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        // Set "max-chars" to a small value of 100 chars, which is less than the text size of the file.
        configurationService.setProperty("textextractor.max-chars", "100");
        InputStream source = getClass().getResourceAsStream("test.pdf");
        InputStream result = instance.getDestinationStream(null, source, false);
        String extractedText = readAll(result);

        // Verify we have exactly the first 100 characters
        assertEquals(100, extractedText.length());
        // Verify it has some text at the beginning of the file, but NOT text near the end
        assertTrue("Known beginning content was found", extractedText.contains("This is a text."));
        assertFalse("Known ending content was not found", extractedText.contains("Emergency Broadcast System"));
    }

    /**
     * Test of getDestinationStream method using older Microsoft Word document.
     * Read a constant .doc document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithDoc()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("wordtest.doc");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue("Known content was not found in .doc", readAll(result).contains("quick brown fox"));
    }

    /**
     * Test of getDestinationStream method using newer Microsoft Word document.
     * Read a constant .docx document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithDocx()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("wordtest.docx");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue("Known content was not found in .docx", readAll(result).contains("quick brown fox"));
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
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.pdf");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue("Known content was not found in .pdf", readAll(result).contains("quick brown fox"));
    }

    /**
     * Test of getDestinationStream method using an HTML document
     * Read a constant .html document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithHTML()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.html");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue("Known content was not found in .html", readAll(result).contains("quick brown fox"));
    }

    /**
     * Test of getDestinationStream method using a TXT document
     * Read a constant .txt document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithTxt()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.txt");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue("Known content was not found in .txt", readAll(result).contains("quick brown fox"));
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
