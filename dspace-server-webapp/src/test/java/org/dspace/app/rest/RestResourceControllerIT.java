/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration test for the {@link RestResourceController}
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class RestResourceControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void undefinedRepository() throws Exception {
        // When we call the root endpoint
        getClient().perform(get("/api/undefined/undefined"))
                // The status has to be 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    public void undefinedSubResource() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        // When we call the root endpoint
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/undefined"))
                // The status has to be 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    public void selfRelIsNotASubResource() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        // When we call the root endpoint
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/self"))
                // The status has to be 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    public void notExistentResourceValidSubPath() throws Exception {
        // When we call the root endpoint
        getClient().perform(get("/api/core/communities/" + UUID.randomUUID().toString() + "/collections"))
                // The status has to be 404 Not Found
                .andExpect(status().isNotFound());
    }

}