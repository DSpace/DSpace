/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.ClarinBitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/clarin/import/logo/* endpoints
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinLogoImportControllerIT extends AbstractEntityIntegrationTest {
    private Bitstream bitstream;

    @Autowired
    private CommunityService communityService;
    @Autowired
    private CollectionService collectionService;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        bitstream = ClarinBitstreamBuilder.createBitstream(context, toInputStream("test", UTF_8)).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void addCommunityLogoTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                post("/api/clarin/import/logo/community")
                        .contentType(contentType)
                        .param("bitstream_id", bitstream.getID().toString())
                        .param("community_id", community.getID().toString()))
        .andExpect(status().isOk());

        community = communityService.find(context, community.getID());
        assertEquals(community.getLogo().getID(), bitstream.getID());
    }

    @Test
    public void addCollectionLogoTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                        post("/api/clarin/import/logo/collection")
                                .contentType(contentType)
                                .param("bitstream_id", bitstream.getID().toString())
                                .param("collection_id", collection.getID().toString()))
                .andExpect(status().isOk());

        collection = collectionService.find(context, collection.getID());
        assertEquals(collection.getLogo().getID(), bitstream.getID());
    }
}
