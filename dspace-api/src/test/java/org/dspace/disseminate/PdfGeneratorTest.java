/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link PdfGenerator}.
 */
public class PdfGeneratorTest {

    private PdfGenerator pdfGenerator;
    private Map<String, String> variables;
    private File testOutputFile;

    @Before
    public void setUp() {
        pdfGenerator = new PdfGenerator();
        variables = new HashMap<>();
        variables.put("metadata_title", "Test Title");
        variables.put("metadata_author", "Test Author");
        variables.put("metadata_editor", "Test Editor");
        testOutputFile = new File("target/testCoverpage.pdf");
    }

    @After
    public void tearDown() {
        if (testOutputFile.exists()) {
            testOutputFile.delete();
        }
    }

    @Test
    public void testParseTemplate() {
        String html = pdfGenerator.parseTemplate("dspace_coverpage", variables);
        assertNotNull("HTML output should not be null", html);
        assertTrue("HTML should contain title", html.contains("Test Title"));
        assertTrue("HTML should contain author", html.contains("Test Author"));
        assertTrue("HTML should contain standard tags", html.contains("<html") || html.contains("<!DOCTYPE html>"));
    }

    @Test
    public void testGeneratePDDocument() throws IOException {
        String html = pdfGenerator.parseTemplate("dspace_coverpage", variables);
        try (PDDocument document = pdfGenerator.generate(html)) {
            assertNotNull("Generated PDDocument should not be null", document);
            assertEquals("Generated PDF should have 1 page", 1, document.getNumberOfPages());
        }
    }

    @Test
    public void testGenerateToFile() {
        String html = pdfGenerator.parseTemplate("dspace_coverpage", variables);
        pdfGenerator.generateToFile(html, testOutputFile);

        assertTrue("Output file should exist", testOutputFile.exists());
        assertTrue("Output file should not be empty", testOutputFile.length() > 0);
    }

    @Test(expected = RuntimeException.class)
    public void testGenerateWithInvalidHtml() {
        pdfGenerator.generate("<unclosed>tag");
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
