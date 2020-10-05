/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageTemplateItem authorization feature
 */
public class ManageTemplateItemFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    SiteService siteService;

    private Site site;
    private Community communityA;
    private Collection collectionA;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        site = siteService.findSite(context);
        communityA = CommunityBuilder.createCommunity(context)
            .withName("communityA")
            .build();
        collectionA = CollectionBuilder.createCollection(context, communityA)
            .withName("collectionA")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void adminTests() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        final String feature = "canManageTemplateItem";

        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", "http://localhost/api/core/collections/" + collectionA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());

        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void anonymousTests() throws Exception {
        final String feature = "canManageTemplateItem";

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", "http://localhost/api/core/collections/" + collectionA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void collectionAdminTests() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicy removePermission = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .withUser(eperson)
            .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(eperson.getEmail(), password);
        final String feature = "canManageTemplateItem";

        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", "http://localhost/api/core/collections/" + collectionA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());

        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", "http://localhost/api/core/communities/" + communityA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
