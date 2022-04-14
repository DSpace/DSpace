/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.AccessStatusRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for the access status endpoint
 */
public class AccessStatusRestControllerIT extends AbstractControllerIntegrationTest {

    @Before
    public void setup() throws Exception {
        super.setUp();
    }

    @Test
    public void testValidUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Owning Collection")
                                                       .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
                               .withTitle("Test item")
                               .build();
        context.restoreAuthSystemState();
        MvcResult result = getClient().perform(get("/api/accessStatus/find?uuid={uuid}", item.getID()))
                                      .andExpect(status().isOk())
                                      .andReturn();
        String content = result.getResponse().getContentAsString();
        // We can read AccessStatusRest directly from the response as it's a customizable Enum for now.
        AccessStatusRest statusRest = new ObjectMapper().readValue(content, AccessStatusRest.class);
        String status = statusRest.getStatus();
        // The status should not be null
        assertNotNull(status);
    }

    @Test
    public void testUnexistentUUID() throws Exception {
        UUID fakeUUID = UUID.randomUUID();
        getClient().perform(get("/api/accessStatus/find?uuid={uuid}", fakeUUID))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testMissingUUIDParameter() throws Exception {
        getClient().perform(get("/api/accessStatus/find"))
            .andExpect(status().isNotFound());
    }
}
