/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.builder.SiteBuilder;
import org.dspace.app.rest.matcher.SiteMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.content.Site;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;

public class SiteRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception {


        context.turnOffAuthorisationSystem();
        //This will always return just one site, DSpace doesn't allow for more to be created
        Site site = SiteBuilder.createSite(context).build();


        getClient().perform(get("/api/core/sites"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.sites[0]", SiteMatcher.matchEntry(site)))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/sites")))
                   .andExpect(jsonPath("$.page.size", is(20)));

    }

    @Test
    public void findOne() throws Exception {


        context.turnOffAuthorisationSystem();
        //This will always return just one site, DSpace doesn't allow for more to be created
        Site site = SiteBuilder.createSite(context).build();


        getClient().perform(get("/api/core/sites/" + site.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", SiteMatcher.matchEntry(site)))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/sites")));

    }


    @Test
    public void findOneWrongUUID() throws Exception {


        context.turnOffAuthorisationSystem();

        getClient().perform(get("/api/core/sites/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound());

    }

    @Test
    public void patchSiteMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchSiteMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = SiteBuilder.createSite(context).build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/sites/" + site.getID(), expectedStatus);
    }
}
