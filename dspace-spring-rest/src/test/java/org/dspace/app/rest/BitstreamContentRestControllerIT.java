/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.disseminate.CitationDocumentServiceImpl;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/core/bitstreams/[id]/content endpoint
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
public class BitstreamContentRestControllerIT extends AbstractControllerIntegrationTest {

    protected SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    private static final Logger log = LogManager
        .getLogger(BitstreamContentRestControllerIT.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CitationDocumentServiceImpl citationDocumentService;

    @BeforeClass
    public static void clearStatistics() throws Exception {
        // To ensure these tests start "fresh", clear out any existing statistics data.
        // NOTE: this is committed immediately in removeIndex()
        StatisticsServiceFactory.getInstance().getSolrLoggerService().removeIndex("*:*");
    }

    @Before
    public void setup() throws Exception {
        super.setUp();

        configurationService.setProperty("citation-page.enable_globally", false);
    }

    @Test
    public void retrieveFullBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                          .build();

            Bitstream bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test bitstream")
                .withDescription("This is a bitstream to test range requests")
                .withMimeType("text/plain")
                .build();

            //** WHEN **
            //We download the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                       //** THEN **
                       .andExpect(status().isOk())

                       //The Content Length must match the full length
                       .andExpect(header().longValue("Content-Length", bitstreamContent.getBytes().length))
                       //The server should indicate we support Range requests
                       .andExpect(header().string("Accept-Ranges", "bytes"))
                       //The ETag has to be based on the checksum
                       .andExpect(header().string("ETag", bitstream.getChecksum()))
                       //We expect the content type to match the bitstream mime type
                       .andExpect(content().contentType("text/plain"))
                       //THe bytes of the content must match the original content
                       .andExpect(content().bytes(bitstreamContent.getBytes()));

            //A If-None-Match HEAD request on the ETag must tell is the bitstream is not modified
            getClient().perform(head("/api/core/bitstreams/" + bitstream.getID() + "/content")
                                    .header("If-None-Match", bitstream.getChecksum()))
                       .andExpect(status().isNotModified());

            //The download and head request should also be logged as a statistics record
            checkNumberOfStatsRecords(bitstream, 2);
        }
    }

    @Test
    public void retrieveRangeBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                          .build();

            Bitstream bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test bitstream")
                .withDescription("This is a bitstream to test range requests")
                .withMimeType("text/plain")
                .build();

            //** WHEN **
            //We download only a specific byte range of the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")
                                    .header("Range", "bytes=1-3"))

                       //** THEN **
                       .andExpect(status().is(206))

                       //The Content Length must match the requested range
                       .andExpect(header().longValue("Content-Length", 3))
                       //The server should indicate we support Range requests
                       .andExpect(header().string("Accept-Ranges", "bytes"))
                       //The ETag has to be based on the checksum
                       .andExpect(header().string("ETag", bitstream.getChecksum()))
                       //The response should give us details about the range
                       .andExpect(header().string("Content-Range", "bytes 1-3/10"))
                       //We expect the content type to match the bitstream mime type
                       .andExpect(content().contentType("text/plain"))
                       //We only expect the bytes 1, 2 and 3
                       .andExpect(content().bytes("123".getBytes()));

            //** WHEN **
            //We download the rest of the range
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")
                                    .header("Range", "bytes=4-"))

                       //** THEN **
                       .andExpect(status().is(206))

                       //The Content Length must match the requested range
                       .andExpect(header().longValue("Content-Length", 6))
                       //The server should indicate we support Range requests
                       .andExpect(header().string("Accept-Ranges", "bytes"))
                       //The ETag has to be based on the checksum
                       .andExpect(header().string("ETag", bitstream.getChecksum()))
                       //The response should give us details about the range
                       .andExpect(header().string("Content-Range", "bytes 4-9/10"))
                       //We expect the content type to match the bitstream mime type
                       .andExpect(content().contentType("text/plain"))
                       //We all remaining bytes, starting at byte 4
                       .andExpect(content().bytes("456789".getBytes()));

            //Check that NO statistics record was logged for the Range requests
            checkNumberOfStatsRecords(bitstream, 0);
        }
    }

    @Test
    public void testBitstreamNotFound() throws Exception {
        getClient().perform(get("/api/core/bitstreams/" + UUID.randomUUID() + "/content"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void testEmbargoedBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                          .build();

            Bitstream bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("6 months")
                .build();

            //** WHEN **
            //We download the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                       //** THEN **
                       .andExpect(status().isUnauthorized());

            //An unauthorized request should not log statistics
            checkNumberOfStatsRecords(bitstream, 0);
        }
    }

    @Test
    public void testPrivateBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a private bitstream
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .build();

        Group internalGroup = GroupBuilder.createGroup(context)
                                          .withName("Internal Group")
                                          .build();

        String bitstreamContent = "Private!";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Bitstream bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withReaderGroup(internalGroup)
                .build();

            //** WHEN **
            //We download the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                       //** THEN **
                       .andExpect(status().isUnauthorized());

            //An unauthorized request should not log statistics
            checkNumberOfStatsRecords(bitstream, 0);

        }
    }

    // Verify number of hits/views of Bitstream is as expected
    private void checkNumberOfStatsRecords(Bitstream bitstream, int expectedNumberOfStatsRecords)
        throws SolrServerException, IOException {
        // Use the SolrLoggerServiceImpl.ResultProcessor inner class to force a Solr commit to occur.
        // This is required because statistics hits will not be committed to Solr until autoCommit next runs.
        SolrLoggerServiceImpl.ResultProcessor rs = ((SolrLoggerServiceImpl) solrLoggerService).new ResultProcessor();
        rs.commit();

        // Find all hits/views of bitstream
        ObjectCount objectCount = solrLoggerService.queryTotal("type:" + Constants.BITSTREAM +
                                                               " AND id:" + bitstream.getID(), null);
        assertEquals(expectedNumberOfStatsRecords, objectCount.getCount());
    }

    @Test
    public void retrieveCitationCoverpageOfBitstream() throws Exception {
        configurationService.setProperty("citation-page.enable_globally", true);
        citationDocumentService.afterPropertiesSet();
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        File originalPdf = new File(testProps.getProperty("test.bitstream"));


        try (InputStream is = new FileInputStream(originalPdf)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                    .withTitle("Public item citation cover page test 1")
                    .withIssueDate("2017-10-17")
                    .withAuthor("Smith, Donald").withAuthor("Doe, John")
                    .build();

            Bitstream bitstream = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Test bitstream")
                    .withDescription("This is a bitstream to test the citation cover page.")
                    .withMimeType("application/pdf")
                    .build();

            //** WHEN **
            //We download the bitstream
            byte[] content = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                    //** THEN **
                    .andExpect(status().isOk())

                    //The Content Length must match the full length
                    .andExpect(header().string("Content-Length", not(nullValue())))
                    //The server should indicate we support Range requests
                    .andExpect(header().string("Accept-Ranges", "bytes"))
                    //The ETag has to be based on the checksum
                    .andExpect(header().string("ETag", bitstream.getChecksum()))
                    //We expect the content type to match the bitstream mime type
                    .andExpect(content().contentType("application/pdf"))
                    //THe bytes of the content must match the original content
                    .andReturn().getResponse().getContentAsByteArray();

            // The citation cover page contains the item title.
            // We will now verify that the pdf text contains this title.
            String pdfText = extractPDFText(content);
            System.out.println(pdfText);
            assertTrue(StringUtils.contains(pdfText,"Public item citation cover page test 1"));

            // The dspace-api/src/test/data/dspaceFolder/assetstore/ConstitutionofIreland.pdf file contains 64 pages,
            // manually counted + 1 citation cover page
            assertEquals(65,getNumberOfPdfPages(content));

            //A If-None-Match HEAD request on the ETag must tell is the bitstream is not modified
            getClient().perform(head("/api/core/bitstreams/" + bitstream.getID() + "/content")
                    .header("If-None-Match", bitstream.getChecksum()))
                    .andExpect(status().isNotModified());

            //The download and head request should also be logged as a statistics record
            checkNumberOfStatsRecords(bitstream, 2);
        }
    }

    private String extractPDFText(byte[] content) throws IOException {
        PDFTextStripper pts = new PDFTextStripper();
        pts.setSortByPosition(true);

        try (ByteArrayInputStream source = new ByteArrayInputStream(content);
             Writer writer = new StringWriter();
             PDDocument pdfDoc = PDDocument.load(source)) {

            pts.writeText(pdfDoc, writer);
            return writer.toString();
        }
    }

    private int getNumberOfPdfPages(byte[] content) throws IOException {
        try (ByteArrayInputStream source = new ByteArrayInputStream(content);
             PDDocument pdfDoc = PDDocument.load(source)) {
            return pdfDoc.getNumberOfPages();
        }
    }


}
