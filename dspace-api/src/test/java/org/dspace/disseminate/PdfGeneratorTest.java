/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PdfGenerator}.
 */
public class PdfGeneratorTest {

    private PdfGenerator pdfGenerator;
    private Map<String, String> variables;
    private File testOutputFile;

    @BeforeEach
    public void setUp() {
        pdfGenerator = new PdfGenerator();
        variables = new HashMap<>();
        variables.put("metadata_title", "Test Title");
        variables.put("metadata_author", "Test Author");
        variables.put("metadata_editor", "Test Editor");
        testOutputFile = new File("target/testCoverpage.pdf");
    }

    @AfterEach
    public void tearDown() {
        if (testOutputFile.exists()) {
            testOutputFile.delete();
        }
    }

    @Test
    public void testParseTemplate() {
        String html = pdfGenerator.parseTemplate("dspace_coverpage", variables);
        assertNotNull(html, "HTML output should not be null");
        assertTrue(html.contains("Test Title"), "HTML should contain title");
        assertTrue(html.contains("Test Author"), "HTML should contain author");
        assertTrue(html.contains("<html") || html.contains("<!DOCTYPE html>"),
                   "HTML should contain standard tags");
    }

    @Test
    public void testGeneratePDDocument() throws IOException {
        String html = pdfGenerator.parseTemplate("dspace_coverpage", variables);
        try (PDDocument document = pdfGenerator.generate(html)) {
            assertNotNull(document, "Generated PDDocument should not be null");
            assertEquals(1, document.getNumberOfPages(), "Generated PDF should have 1 page");
        }
    }

    @Test
    public void testGenerateToFile() {
        String html = pdfGenerator.parseTemplate("dspace_coverpage", variables);
        pdfGenerator.generateToFile(html, testOutputFile);

        assertTrue(testOutputFile.exists(), "Output file should exist");
        assertTrue(testOutputFile.length() > 0, "Output file should not be empty");
    }

    @Test
    public void testGenerateWithInvalidHtml() {
        assertThrows(RuntimeException.class, () -> pdfGenerator.generate("<unclosed>tag"));
    }

    @Test
    public void testBlockXXE() {
        String xxePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE foo [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>" +
                "<html><body>&xxe;</body></html>";

        try {
            pdfGenerator.generate(xxePayload);
            throw new AssertionError("Should have blocked XXE/DOCTYPE");
        } catch (RuntimeException e) {
            // Expected
        }
    }
}
