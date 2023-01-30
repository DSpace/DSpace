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

import java.util.concurrent.Callable;

import org.dspace.app.rest.authorization.impl.SubmitFeature;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

public class SubmitFeatureIT extends AbstractControllerIntegrationTest {
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private CollectionConverter collectionConverter;
    @Autowired
    private SiteConverter siteConverter;
    @Autowired
    private Utils utils;

    private Group group;
    private String siteUri;
    private String epersonToken;
    private AuthorizationFeature submitFeature;
    private Community communityA;

    private Collection collectionA1;
    private Collection collectionA2;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        withSuppressedAuthorization(() -> {
            communityA = CommunityBuilder.createCommunity(context).withName("Community A").build();
            collectionA1 = CollectionBuilder.createCollection(context, communityA).withName("Collection A1").build();
            collectionA2 = CollectionBuilder.createCollection(context, communityA).withName("Collection A2").build();
            group = GroupBuilder.createGroup(context)
                .withName("group")
                .addMember(eperson)
                .build();
            return null;
        });
        submitFeature = authorizationFeatureService.find(SubmitFeature.NAME);

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        siteUri = utils.linkToSingleResource(siteRest, "self").getHref();
    }

    @Test
    public void testNoRights() throws Exception {
        expectZeroResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testDirectEPersonAddPolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
                .withUser(eperson)
                .withDspaceObject(collectionA1)
                .withAction(Constants.ADD)
                .build();
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testDirectGroupAddPolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withGroup(group)
            .withDspaceObject(collectionA1)
            .withAction(Constants.ADD)
            .build();
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testDirectEPersonAdminPolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withUser(eperson)
            .withDspaceObject(collectionA1)
            .withAction(Constants.ADMIN)
            .build();
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testDirectGroupAdminPolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withGroup(group)
            .withDspaceObject(collectionA1)
            .withAction(Constants.ADMIN)
            .build();
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testCollectionAdmin() throws Exception {
        withSuppressedAuthorization(() -> {
            Collection col = CollectionBuilder
                .createCollection(context, communityA)
                .withName("this is another test collection")
                .withAdminGroup(eperson)
                .build();
            return null;
        });
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testCommunityWithoutCollectionsAdmin() throws Exception {
        withSuppressedAuthorization(() -> {
            Community comm = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains no collections")
                .withAdminGroup(eperson)
                .build();
            return null;
        });
        expectZeroResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testCommunityWithCollectionsAdmin() throws Exception {
        withSuppressedAuthorization(() -> {
            Community comm = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains a collection")
                .withAdminGroup(eperson)
                .build();
            Collection coll = CollectionBuilder
                .createCollection(context, comm)
                .withName("Contained collection")
                .build();
            return null;
        });
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testCommunityWithSubCommunityWithCollectionsAdmin() throws Exception {
        withSuppressedAuthorization(() -> {
            Community parent = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains no collections")
                .withAdminGroup(eperson)
                .build();
            Community child = CommunityBuilder
                .createSubCommunity(context, parent)
                .withName("This community contains a collection")
                .build();
            Collection coll = CollectionBuilder
                .createCollection(context, child)
                .withName("Contained collection")
                .build();
            return null;
        });
        expectSomeResults(requestSitewideSubmitFeature());
    }

    @Test
    public void testNoRightsOnCollection() throws Exception {
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA1)));
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA2)));
    }

    @Test
    public void testDirectEPersonAddPolicyOnCollection() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
                .withUser(eperson)
                .withDspaceObject(collectionA1)
                .withAction(Constants.ADD)
                .build();
        expectSomeResults(requestSubmitFeature(getCollectionUri(collectionA1)));
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA2)));
    }

    @Test
    public void testDirectGroupAddPolicyOnCollection() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withGroup(group)
            .withDspaceObject(collectionA1)
            .withAction(Constants.ADD)
            .build();
        expectSomeResults(requestSubmitFeature(getCollectionUri(collectionA1)));
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA2)));
    }

    @Test
    public void testDirectEPersonAdminPolicyOnCollection() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withUser(eperson)
            .withDspaceObject(collectionA1)
            .withAction(Constants.ADMIN)
            .build();
        expectSomeResults(requestSubmitFeature(getCollectionUri(collectionA1)));
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA2)));
    }

    @Test
    public void testDirectGroupAdminPolicyOnCollection() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withGroup(group)
            .withDspaceObject(collectionA1)
            .withAction(Constants.ADMIN)
            .build();
        expectSomeResults(requestSubmitFeature(getCollectionUri(collectionA1)));
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA2)));
    }

    @Test
    public void testCollectionAdminOnCollection() throws Exception {
        Collection col = withSuppressedAuthorization(() -> {
            return CollectionBuilder
                .createCollection(context, communityA)
                .withName("this is another test collection")
                .withAdminGroup(eperson)
                .build();
        });
        expectSomeResults(requestSubmitFeature(getCollectionUri(col)));
        expectZeroResults(requestSubmitFeature(getCollectionUri(collectionA1)));
    }

    @Test
    public void testCommunityWithCollectionsAdminOnCollection() throws Exception {
        Collection coll = withSuppressedAuthorization(() -> {
            Community comm = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains a collection")
                .withAdminGroup(eperson)
                .build();
            return CollectionBuilder
                .createCollection(context, comm)
                .withName("Contained collection")
                .build();
        });
        expectSomeResults(requestSubmitFeature(getCollectionUri(coll)));
    }

    @Test
    public void testCommunityWithSubCommunityWithCollectionsAdminOnCollection() throws Exception {
        Collection coll = withSuppressedAuthorization(() -> {
            Community parent = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains no collections")
                .withAdminGroup(eperson)
                .build();
            Community child = CommunityBuilder
                .createSubCommunity(context, parent)
                .withName("This community contains a collection")
                .build();
            return CollectionBuilder
                .createCollection(context, child)
                .withName("Contained collection")
                .build();
        });
        expectSomeResults(requestSubmitFeature(getCollectionUri(coll)));
    }

    private ResultActions requestSitewideSubmitFeature() throws Exception {
        return requestSubmitFeature(siteUri);
    }

    private ResultActions requestSubmitFeature(String uri) throws Exception {
        epersonToken = getAuthToken(eperson.getEmail(), password);
        return getClient(epersonToken).perform(get("/api/authz/authorizations/search/object?")
            .param("uri", uri)
            .param("feature", submitFeature.getName())
            .param("embed", "feature"));
    }

    private ResultActions expectSomeResults(ResultActions actions) throws Exception {
        return actions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    private ResultActions expectZeroResults(ResultActions actions) throws Exception {
        return actions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    private <T> T withSuppressedAuthorization(Callable<T> fn) throws Exception {
        context.turnOffAuthorisationSystem();
        T result = fn.call();
        context.restoreAuthSystemState();
        return result;
    }

    private String getCollectionUri(Collection collection) {
        CollectionRest collectionRest = collectionConverter.convert(collection, DefaultProjection.DEFAULT);
        return utils.linkToSingleResource(collectionRest, "self").getHref();
    }
}
