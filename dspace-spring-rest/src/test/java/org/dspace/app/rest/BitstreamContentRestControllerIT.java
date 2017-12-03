/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
import org.dspace.solr.MockSolrServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test to test the /api/core/bitstreams/[id]/content endpoint
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
public class BitstreamContentRestControllerIT extends AbstractControllerIntegrationTest {

    private MockSolrServer mockSolrServer;

    @Before
    public void setup() throws Exception {
        super.setUp();
        mockSolrServer = new MockSolrServer("statistics");
        mockSolrServer.getSolrServer().deleteByQuery("*:*");
        mockSolrServer.getSolrServer().commit();
    }

    @After
    public void destroy() throws Exception {
        super.destroy();
        mockSolrServer.destroy();
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

        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

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

        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

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

        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

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
        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

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

        } finally {
            //** CLEANUP **
            GroupBuilder.cleaner().delete(internalGroup);
        }
    }

    private void checkNumberOfStatsRecords(Bitstream bitstream, int expectedNumberOfStatsRecords) throws SolrServerException, IOException {
        mockSolrServer.getSolrServer().commit();

        SolrQuery query = new SolrQuery("id:\"" + bitstream.getID() + "\"")
                .setRows(0)
                .setStart(0);
        QueryResponse queryResponse = mockSolrServer.getSolrServer().query(query);
        assertEquals(expectedNumberOfStatsRecords, queryResponse.getResults().getNumFound());
    }

}