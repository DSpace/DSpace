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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.SiteMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.builder.SiteBuilder;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SiteRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Test
    public void findAll() throws Exception {


        context.turnOffAuthorisationSystem();
        //This will always return just one site, DSpace doesn't allow for more to be created
        Site site = SiteBuilder.createSite(context).build();

        context.restoreAuthSystemState();

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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/sites/" + site.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", SiteMatcher.matchEntry(site)))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/sites")));

    }


    @Test
    public void findOneWrongUUID() throws Exception {
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

    @Test
    public void patchReplaceMultipleDescriptionSite() throws Exception {
        context.turnOffAuthorisationSystem();

        List<String> siteDescriptions = List.of(
            "FIRST",
            "SECOND",
            "THIRD"
        );

        Site site = SiteBuilder.createSite(context).build();

        this.siteService
            .addMetadata(
                context, site,
                MetadataSchemaEnum.DC.getName(), "description", null,
                Item.ANY, siteDescriptions
            );

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token)
            .perform(get("/api/core/sites/" + site.getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.metadata",
                    Matchers.allOf(
                        MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(0), 0),
                        MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(1), 1),
                        MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(2), 2)
                    )
                )
            );

        List<Operation> ops = List.of(
            new ReplaceOperation("/metadata/dc.description/0", siteDescriptions.get(2)),
            new ReplaceOperation("/metadata/dc.description/1", siteDescriptions.get(0)),
            new ReplaceOperation("/metadata/dc.description/2", siteDescriptions.get(1))
        );
        String requestBody = getPatchContent(ops);
        getClient(token)
            .perform(patch("/api/core/sites/" + site.getID())
            .content(requestBody)
            .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(
                 jsonPath("$.metadata",
                     Matchers.allOf(
                         MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(2), 0),
                         MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(0), 1),
                         MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(1), 2)
                     )
                 )
             );
        getClient(token)
            .perform(get("/api/core/sites/" + site.getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.metadata",
                    Matchers.allOf(
                        MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(2), 0),
                        MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(0), 1),
                        MetadataMatcher.matchMetadata("dc.description", siteDescriptions.get(1), 2)
                    )
                )
            );
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = SiteBuilder.createSite(context).build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/sites/" + site.getID(), expectedStatus);
    }
}
