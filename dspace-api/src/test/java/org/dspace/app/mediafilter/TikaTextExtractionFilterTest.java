/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * Test the TikaTextExtractionFilter using test files for all major formats.
 * The test files used below are all located at [dspace-api]/src/test/resources/org/dspace/app/mediafilter/
 *
 * @author mwood
 * @author Tim Donohue
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
        assertTrue(tempFileExtractedText.contains("quick brown fox"), "Known content was not found in .pdf");

        // Now, extract text from same file using default, in-memory
        configurationService.setProperty("textextractor.use-temp-file", "false");
        source = getClass().getResourceAsStream("test.pdf");
        result = instance.getDestinationStream(null, source, false);
        String inMemoryExtractedText = readAll(result);

        // Verify the two results are equal
        assertEquals(inMemoryExtractedText, tempFileExtractedText, "Extracted text via temp file is the same as in-memory.");
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
        assertTrue(extractedText.contains("This is a text."), "Known beginning content was found");
        assertFalse(extractedText.contains("Emergency Broadcast System"), "Known ending content was not found");
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

        InputStream source = getClass().getResourceAsStream("test.doc");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .doc");
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

        InputStream source = getClass().getResourceAsStream("test.docx");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .docx");
    }

    /**
     * Test of getDestinationStream method using an ODT document
     * Read a constant .odt document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithODT()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.odt");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .odt");
    }

    /**
     * Test of getDestinationStream method using an RTF document
     * Read a constant .rtf document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithRTF()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.rtf");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .rtf");
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
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .pdf");
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
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .html");
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
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .txt");
    }

    /**
     * Test of getDestinationStream method using a CSV document
     * Read a constant .csv document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithCsv()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.csv");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("data3,3"), "Known content was not found in .csv");
    }

    /**
     * Test of getDestinationStream method using an XLS document
     * Read a constant .xls document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithXLS()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.xls");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("data3,3"), "Known content was not found in .xls");
    }

    /**
     * Test of getDestinationStream method using an XLSX document
     * Read a constant .xlsx document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithXLSX()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.xlsx");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("data3,3"), "Known content was not found in .xlsx");
    }

    /**
     * Test of getDestinationStream method using an ODS document
     * Read a constant .ods document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithODS()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.ods");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("Data on the second sheet"), "Known content was not found in .ods");
    }

    /**
     * Test of getDestinationStream method using an PPT document
     * Read a constant .ppt document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithPPT()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.ppt");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .ppt");
    }

    /**
     * Test of getDestinationStream method using an PPTX document
     * Read a constant .pptx document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithPPTX()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.pptx");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .pptx");
    }

    /**
     * Test of getDestinationStream method using an ODP document
     * Read a constant .odp document and examine the extracted text.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetDestinationStreamWithODP()
        throws Exception {
        TikaTextExtractionFilter instance = new TikaTextExtractionFilter();

        InputStream source = getClass().getResourceAsStream("test.odp");
        InputStream result = instance.getDestinationStream(null, source, false);
        assertTrue(readAll(result).contains("quick brown fox"), "Known content was not found in .odp");
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
