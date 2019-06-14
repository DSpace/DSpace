/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test for the identifier resolver
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class IdentifierRestControllerIT extends AbstractControllerIntegrationTest {

    @Before
    public void setup() throws Exception {
        super.setUp();
   }

    @Test
    public void testValidIdentifier() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a top community to receive an identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        context.restoreAuthSystemState();

        String handle = parentCommunity.getHandle();
        String communityDetail = REST_SERVER_URL + "core/communities/" + parentCommunity.getID();

        getClient().perform(get("/api/pid/find?id={handle}",handle))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", communityDetail));
    }

    @Test
    public void testUnexistentIdentifier() throws Exception {
        getClient().perform(get("/api/pid/find?id={id}","fakeIdentifier"))
                        .andExpect(status().isNotFound());
    }

    @Test
    @Ignore
    /**
     * This test will check the return status code when no id is supplied. It currently fails as our
     * RestResourceController take the precedence over the pid controller returning a 404 Repository not found
     *
     * @throws Exception
     */
    public void testMissingIdentifierParameter() throws Exception {
        getClient().perform(get("/api/pid/find"))
                        .andExpect(status().isUnprocessableEntity());
    }
}
