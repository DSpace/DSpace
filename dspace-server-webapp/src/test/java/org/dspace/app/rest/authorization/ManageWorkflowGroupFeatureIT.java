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

import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageWorkflowGroup authorization feature
 */
public class ManageWorkflowGroupFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private CollectionConverter collectionConverter;

    private Community communityA;
    private CommunityRest communityARest;
    private Collection collectionA;
    private CollectionRest collectionARest;

    final String feature = "canManageWorkflowGroup";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        communityA = CommunityBuilder.createCommunity(context)
            .withName("communityA")
            .build();
        collectionA = CollectionBuilder.createCollection(context, communityA)
            .withName("collectionA")
            .build();

        context.restoreAuthSystemState();

        communityARest = communityConverter.convert(communityA, Projection.DEFAULT);
        collectionARest = collectionConverter.convert(collectionA, Projection.DEFAULT);
    }

    @Test
    public void adminCollectionTestSuccess() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminCommunityTestNotFound() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void anonymousCollectionTestNotFound() throws Exception {
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void anonymousCommunityTestNotFound() throws Exception {
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void collectionAdminCollectionTestSuccess() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .withUser(eperson)
            .build();
        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void collectionAdminCommunityTestNotFound() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .withUser(eperson)
            .build();
        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
