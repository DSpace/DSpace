/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PdfGeneratorTest {

    @Test
    public void canRenderPdf() throws Exception {

        var pdf = new PdfGenerator();

        Map<String, String> variables = new HashMap<>();

        variables.put("metadata_title", "This is my test title");
        variables.put("metadata_author", "Author a; Author b; Author c; etc.");
        variables.put("metadata_editor", "Editor 1; Editor 2; Editor 3; etc.");

        var html = pdf.parseTemplate("dspace_coverpage", variables);

        try (var page = pdf.generate(html)) {
            assertThat(page.getNumberOfPages(), equalTo(1));
        }

        // PDF is written to the filesystem so you can view it.

        // use e.g. entr view the PDF and restart the viewer when there are changes (https://eradman.com/entrproject/)
        // ls testCoverpage.pdf | entr -r open testCoverpage.pdf (replace open with your pdf viewer)

        pdf.generateToFile(html, new File("target/testCoverpage.pdf"));
    }
}
