package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.solr.MockSolrServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test to test the /api/core/bitstreams/[id]/content endpoint
 */
public class BitstreamContentRestControllerIT extends AbstractControllerIntegrationTest {

    private MockSolrServer mockSolrServer;

    @Before
    public void setup() throws Exception {
        super.setUp();
        mockSolrServer = new MockSolrServer("statistics");
        mockSolrServer.getSolrServer().deleteByQuery("*:*");
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
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = new CollectionBuilder().createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = new ItemBuilder().createItem(context, col1)
                    .withTitle("Public item 1")
                    .withIssueDate("2017-10-17")
                    .withAuthor("Smith, Donald").withAuthor("Doe, John")
                    .build();

            Bitstream bitstream = new BitstreamBuilder()
                    .createBitstream(context, publicItem1, is)
                    .withName("Test bitstream")
                    .withDescription("This is a bitstream to test range requests")
                    .withMimeType("text/plain")
                    .build();

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andExpect(status().isOk())

                    //We expect the content type to be "application/hal+json;charset=UTF-8"
                    .andExpect(header().longValue("Content-Length", bitstreamContent.getBytes().length))
                    .andExpect(header().string("Accept-Ranges", "bytes"))
                    .andExpect(header().string("ETag", bitstream.getChecksum()))
                    .andExpect(content().contentType("text/plain"))
                    .andExpect(content().bytes(bitstreamContent.getBytes()));

            //Check that a statistics record was logged.
            mockSolrServer.getSolrServer().commit();

            SolrQuery query = new SolrQuery("id:\"" + bitstream.getID() + "\"")
                    .setRows(0)
                    .setStart(0);
            QueryResponse queryResponse = mockSolrServer.getSolrServer().query(query);
            assertEquals( 1, queryResponse.getResults().getNumFound());
        }
    }

    @Test
    public void retrieveRangeBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = new CollectionBuilder().createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try(InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = new ItemBuilder().createItem(context, col1)
                    .withTitle("Public item 1")
                    .withIssueDate("2017-10-17")
                    .withAuthor("Smith, Donald").withAuthor("Doe, John")
                    .build();

            Bitstream bitstream = new BitstreamBuilder()
                    .createBitstream(context, publicItem1, is)
                    .withName("Test bitstream")
                    .withDescription("This is a bitstream to test range requests")
                    .withMimeType("text/plain")
                    .build();

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")
                    .header("Range", "bytes=1-3"))
                    .andExpect(status().is(206))

                    //We expect the content type to be "application/hal+json;charset=UTF-8"
                    .andExpect(header().longValue("Content-Length", 3))
                    .andExpect(header().string("Accept-Ranges", "bytes"))
                    .andExpect(header().string("ETag", bitstream.getChecksum()))
                    .andExpect(header().string("Content-Range", "bytes 1-3/10"))
                    .andExpect(content().contentType("text/plain"))
                    .andExpect(content().bytes("123".getBytes()));

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")
                    .header("Range", "bytes=4-"))
                    .andExpect(status().is(206))

                    //We expect the content type to be "application/hal+json;charset=UTF-8"
                    .andExpect(header().longValue("Content-Length", 6))
                    .andExpect(header().string("Accept-Ranges", "bytes"))
                    .andExpect(header().string("ETag", bitstream.getChecksum()))
                    .andExpect(header().string("Content-Range", "bytes 4-9/10"))
                    .andExpect(content().contentType("text/plain"))
                    .andExpect(content().bytes("456789".getBytes()));

            //Check that NO statistics record was logged.
            mockSolrServer.getSolrServer().commit();

            SolrQuery query = new SolrQuery("id:\"" + bitstream.getID() + "\"")
                    .setRows(0)
                    .setStart(0);
            QueryResponse queryResponse = mockSolrServer.getSolrServer().query(query);
            assertEquals(0, queryResponse.getResults().getNumFound());
        }
    }

}