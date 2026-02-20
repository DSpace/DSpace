/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.submit.extraction.GrobidImportMetadataSourceServiceImpl;
import org.dspace.submit.extraction.grobid.TEI;
import org.dspace.submit.extraction.grobid.client.ConsolidateHeaderEnum;
import org.dspace.submit.extraction.grobid.client.GrobidClient;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Test suite for the WorkspaceItem endpoint
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class GrobidMetadataExtractionIT extends AbstractControllerIntegrationTest {

    @Autowired
    private GrobidImportMetadataSourceServiceImpl grobidImportMetadataSourceService;

    private GrobidClient grobidClient;
    private GrobidClient grobidClientMock;

    private static <T> @NonNull ResultMatcher grobidMetadataMatcherList(
        String metadata, Matcher<Iterable<? extends T>> matcher
    ) {
        return jsonPath(
            "$._embedded.workspaceitems[0].sections.grobidmetadata['" + metadata + "'][*].value",
            matcher
        );
    }

    private static @NonNull ResultMatcher grobidMetadataMatcher(
        String metadata, Matcher<String> matcher
    ) {
        return jsonPath(
            "$._embedded.workspaceitems[0].sections.grobidmetadata['" + metadata + "'][0].value",
            matcher
        );
    }


    private static @NonNull ResultMatcher uploadMatcher(
        String metadata, Matcher<String> matcher
    ) {
        return jsonPath(
            "$._embedded.workspaceitems[0].sections.upload.files[0].metadata['" + metadata + "'][0].value",
            matcher
        );
    }

    private TEI loadSimpleTei() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(
            "/org/dspace/app/rest/simple-article.pdf.tei.xml")) {
            assertNotNull("Test TEI XML resource should exist", is);
            JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (TEI) unmarshaller.unmarshal(is);
        }
    }

    private TEI loadFullTei() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(
            "/org/dspace/app/rest/full-example.pdf.tei.xml")) {
            assertNotNull("Test TEI XML resource should exist", is);
            JAXBContext jaxbContext = JAXBContext.newInstance(TEI.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (TEI) unmarshaller.unmarshal(is);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();
        Community child1 =
            CommunityBuilder.createSubCommunity(context, parentCommunity)
                            .withName("Sub Community")
                            .build();

        CollectionBuilder.createCollection(context, child1, "123456789/grobid-extraction")
                         .withName("Collection 1")
                         .withSubmitterGroup(eperson)
                         .build();

        context.restoreAuthSystemState();

        grobidClient = grobidImportMetadataSourceService.getGrobidClient();
        grobidClientMock = mock(GrobidClient.class);
        grobidImportMetadataSourceService.setGrobidClient(grobidClientMock);
    }

    @After
    public void cleanupGrobidMock() {
        grobidImportMetadataSourceService.setGrobidClient(grobidClient);
    }

    @Test
    public void createWorkspaceItemFromSimplePDFFileTest() throws Exception {

        TEI tei = loadSimpleTei();
        when(grobidClientMock.processHeaderDocument(any(InputStream.class), any(ConsolidateHeaderEnum.class)))
            .thenReturn(tei);

        String authToken = getAuthToken(eperson.getEmail(), password);

        try (InputStream pdf = getClass().getResourceAsStream("/org/dspace/app/rest/simple-article.pdf")) {
            final MockMultipartFile pdfFile =
                new MockMultipartFile("file", "/local/path/simple-article.pdf", "application/pdf", pdf);

            // bulk create a workspaceitem
            getClient(authToken).perform(multipart("/api/submission/workspaceitems").file(pdfFile))
                                .andExpect(status().isOk())
                                // testing grobid extraction
                                .andExpect(
                                    grobidMetadataMatcher("dc.title", is("This is a simple test file"))
                                )
                                .andExpect(grobidMetadataMatcher("dc.contributor.author", is("Bollini, Andrea")))
                                // we can just check that the pdf is stored in the item
                                .andExpect(
                                    uploadMatcher(
                                        "dc.title", is("simple-article.pdf")
                                    )
                                )
                                .andExpect(
                                    uploadMatcher(
                                        "dc.source", is("/local/path/simple-article.pdf")
                                    )
                                );
        }
    }


    @Test
    public void createWorkspaceItemFromFullPDFFileTest() throws Exception {

        TEI tei = loadFullTei();
        when(grobidClientMock.processHeaderDocument(any(InputStream.class), any(ConsolidateHeaderEnum.class)))
            .thenReturn(tei);

        String authToken = getAuthToken(eperson.getEmail(), password);

        try (InputStream pdf = IOUtils.toInputStream("FULL PDF", CharEncoding.UTF_8)) {
            final MockMultipartFile pdfFile =
                new MockMultipartFile("file", "/local/path/full-example.pdf", "application/pdf", pdf);

            // bulk create a workspaceitem
            getClient(authToken).perform(multipart("/api/submission/workspaceitems").file(pdfFile))
                                .andExpect(status().isOk())
                                // testing grobid extraction
                                .andExpect(
                                    grobidMetadataMatcher("dc.title", is("This is a simple test file"))
                                )
                                .andExpect(grobidMetadataMatcher("dc.contributor.author", is("Bollini, Andrea")))
                                .andExpect(grobidMetadataMatcher("dc.date.issued", is("2018")))
                                .andExpect(
                                    grobidMetadataMatcher(
                                        "dc.identifier.issn",
                                        is("1234-5678")
                                    )
                                )
                                .andExpect(
                                    grobidMetadataMatcher(
                                        "dc.identifier.isbn",
                                        is("978-3-16-148410-0")
                                    )
                                )
                                .andExpect(
                                    grobidMetadataMatcher(
                                        "dc.identifier.doi",
                                        is("10.1234/example.2026.0123")
                                    )
                                )
                                .andExpect(
                                    grobidMetadataMatcher(
                                        "dc.description.abstract", is("This is the abstract of our PDF file")
                                    )
                                )
                                .andExpect(
                                    grobidMetadataMatcher(
                                        "dc.source", is("GROBID-JOURNAL")
                                    )
                                )
                                .andExpect(
                                    grobidMetadataMatcherList(
                                        "dc.subject",
                                        containsInAnyOrder(
                                            is("PDF"),
                                            is("GROBID"),
                                            is("Metadata Extraction")
                                        )
                                    )
                                )
                                // we can just check that the pdf is stored in the item
                                .andExpect(
                                    uploadMatcher(
                                        "dc.title", is("full-example.pdf")
                                    )
                                )
                                .andExpect(
                                    uploadMatcher(
                                        "dc.source", is("/local/path/full-example.pdf")
                                    )
                                );
        }
    }


}
