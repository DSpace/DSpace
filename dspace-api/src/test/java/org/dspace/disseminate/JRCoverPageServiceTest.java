/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.DefaultResourceLoader;

@RunWith(MockitoJUnitRunner.class)
public class JRCoverPageServiceTest {

    @Mock
    Context context;

    @Mock
    Item item;

    Map<String, Object> params = new HashMap<>();

    private JRCoverPageService sut;

    @Before
    public void setUp() throws Exception {
        sut = new JRCoverPageService("classpath:cover-template.jrxml", new DefaultResourceLoader()) {
            @Override
            protected Map<String, Object> prepareParams(Item item) {
                return params;
            }
        };
    }

    @Test
    public void canRenderCoverPage() throws Exception {

        params.put("dc_title", "My title");
        params.put("dc_contributor_author", "My author");

        try (var coverPage = sut.renderCoverDocument(context, item)) {
            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);

            assertThat(text, containsString("dspace.lyrasis.org"));
            assertThat(text, containsString("My title"));
            assertThat(text, containsString("My author"));
        }
    }

    @Test
    public void canRenderCoverPageWithMissingMetadata() throws Exception {

        params.put("dc_title", "My title");

        try (var coverPage = sut.renderCoverDocument(context, item)) {
            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);

            assertThat(text, containsString("dspace.lyrasis.org"));
            assertThat(text, containsString("My title"));
            assertThat(text, containsString("Unknown"));
        }
    }
}