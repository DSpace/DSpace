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

import org.dspace.app.rest.authorization.impl.CreateCollectionFeature;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test deals with testing the feature {@link CreateCollectionFeature}
 */
public class CanCreateCollectionsIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    private Community communityA;
    private Community communityB;
    private Community communityC;
    private Collection collectionA;

    private AuthorizationFeature canCreateCollectionsFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        communityA = CommunityBuilder.createCommunity(context).withName("Community A").build();
        collectionA = CollectionBuilder.createCollection(context, communityA).withName("Collection A").build();
        communityB = CommunityBuilder.createCommunity(context).withName("Community B").build();
        communityC = CommunityBuilder.createSubCommunity(context, communityB).withName("Community C").build();
        context.restoreAuthSystemState();

        canCreateCollectionsFeature = authorizationFeatureService.find(CreateCollectionFeature.NAME);
    }

    @Test
    public void canCreateCollectionsOnCommunityAsAdminTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        CommunityRest comRest = communityConverter.convert(communityA, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", comUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void canCreateCollectionsOnSiteAsAdminTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", siteUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnCollectionAsAdminTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        CollectionRest collectionRest = collectionConverter.convert(collectionA, DefaultProjection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", collectionUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnCommunityAsAnonymousTest() throws Exception {
        CommunityRest comRest = communityConverter.convert(communityA, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", comUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnSiteAsAnonymousTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", siteUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnCollectionAsAnonymousTest() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, DefaultProjection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                .param("uri", collectionUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnCommunityAsEPersonWithoutRightsTest() throws Exception {
        authorizeService.addPolicy(context, communityB, Constants.ADMIN, eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        CommunityRest comRest = communityConverter.convert(communityA, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", comUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnSiteAsEPersonWithoutRightsTest() throws Exception {
        authorizeService.addPolicy(context, communityB, Constants.ADMIN, eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", siteUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnCollectionAsEPersonWithoutRightsTest() throws Exception {
        authorizeService.addPolicy(context, communityB, Constants.ADMIN, eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        CollectionRest collectionRest = collectionConverter.convert(collectionA, DefaultProjection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", collectionUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void canCreateCollectionsOnCommunityAsEPersonWithRightsTest() throws Exception {
        authorizeService.addPolicy(context, communityB, Constants.ADMIN, eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        CommunityRest comRest = communityConverter.convert(communityB, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", comUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void canCreateCollectionsOnSubCommunityAsEPersonWithRightsOnParentCommunityTest() throws Exception {
        authorizeService.addPolicy(context, communityB, Constants.ADMIN, eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        CommunityRest comRest = communityConverter.convert(communityC, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", comUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    public void canCreateCollectionsOnSubCommunityAsEPersonWithoutRightsOnParentCommunityTest() throws Exception {
        authorizeService.addPolicy(context, communityA, Constants.ADMIN, eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        CommunityRest comRest = communityConverter.convert(communityC, DefaultProjection.DEFAULT);
        String comUri = utils.linkToSingleResource(comRest, "self").getHref();

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", comUri)
                .param("feature", canCreateCollectionsFeature.getName())
                .param("embed", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

}
