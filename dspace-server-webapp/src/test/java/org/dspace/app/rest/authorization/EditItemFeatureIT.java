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

import org.dspace.app.rest.authorization.impl.EditItemFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

public class EditItemFeatureIT extends AbstractControllerIntegrationTest {
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private SiteConverter siteConverter;
    @Autowired
    private Utils utils;

    private Group group;
    private String siteUri;
    private String epersonToken;
    private AuthorizationFeature editItemFeature;
    private Community communityA;

    private Collection collectionA1;
    private Collection collectionA2;

    private Item itemA1X;
    private Item itemA2X;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        withSuppressedAuthorization(() -> {
            communityA = CommunityBuilder.createCommunity(context).withName("Community A").build();
            collectionA1 = CollectionBuilder.createCollection(context, communityA).withName("Collection A1").build();
            collectionA2 = CollectionBuilder.createCollection(context, communityA).withName("Collection A2").build();
            itemA1X = ItemBuilder.createItem(context, collectionA1).withTitle("Item A1X").build();
            itemA2X = ItemBuilder.createItem(context, collectionA2).withTitle("Item A2X").build();
            group = GroupBuilder.createGroup(context)
                .withName("group")
                .addMember(eperson)
                .build();
            return null;
        });
        editItemFeature = authorizationFeatureService.find(EditItemFeature.NAME);

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);
        siteUri = utils.linkToSingleResource(siteRest, "self").getHref();
    }

    @Test
    public void testNoRights() throws Exception {
        expectZeroResults(requestSitewideEditItemFeature());
    }

    @Test
    public void testDirectEPersonWritePolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withUser(eperson)
            .withDspaceObject(itemA1X)
            .withAction(Constants.WRITE)
            .build();
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(itemA1X));
        expectZeroResults(requestEditItemFeature(itemA2X));
    }

    @Test
    public void testDirectGroupWritePolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withGroup(group)
            .withDspaceObject(itemA1X)
            .withAction(Constants.WRITE)
            .build();
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(itemA1X));
        expectZeroResults(requestEditItemFeature(itemA2X));
    }

    @Test
    public void testDirectEPersonAdminPolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withUser(eperson)
            .withDspaceObject(itemA1X)
            .withAction(Constants.ADMIN)
            .build();
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(itemA1X));
        expectZeroResults(requestEditItemFeature(itemA2X));
    }

    @Test
    public void testDirectGroupAdminPolicy() throws Exception {
        ResourcePolicy rp = ResourcePolicyBuilder.createResourcePolicy(context)
            .withGroup(group)
            .withDspaceObject(itemA1X)
            .withAction(Constants.ADMIN)
            .build();
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(itemA1X));
        expectZeroResults(requestEditItemFeature(itemA2X));
    }

    @Test
    public void testNonemptyCollectionAdmin() throws Exception {
        Item item = withSuppressedAuthorization(() -> {
            Collection col = CollectionBuilder
                .createCollection(context, communityA)
                .withName("nonempty collection")
                .withAdminGroup(eperson)
                .build();
            return ItemBuilder
                .createItem(context, col)
                .withTitle("item in nonempty collection")
                .build();
        });
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(item));
        expectZeroResults(requestEditItemFeature(itemA1X));
        expectZeroResults(requestEditItemFeature(itemA2X));
    }

    @Test
    public void testEmptyCollectionAdmin() throws Exception {
        withSuppressedAuthorization(() -> {
            Collection col = CollectionBuilder
                .createCollection(context, communityA)
                .withName("nonempty collection")
                .withAdminGroup(eperson)
                .build();
            return null;
        });
        expectZeroResults(requestSitewideEditItemFeature());
    }

    @Test
    public void testCommunityWithEmptyCollectionAdmin() throws Exception {
        withSuppressedAuthorization(() -> {
            Community comm = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains a collection")
                .withAdminGroup(eperson)
                .build();
            Collection coll = CollectionBuilder
                .createCollection(context, comm)
                .withName("This collection contains no items")
                .build();
            return null;
        });
        expectZeroResults(requestSitewideEditItemFeature());
    }

    @Test
    public void testCommunityWithNonemptyCollectionAdmin() throws Exception {
        Item item = withSuppressedAuthorization(() -> {
            Community comm = CommunityBuilder
                .createCommunity(context)
                .withName("This community contains a collection")
                .withAdminGroup(eperson)
                .build();
            Collection coll = CollectionBuilder
                .createCollection(context, comm)
                .withName("This collection contains an item")
                .build();
            return ItemBuilder
                .createItem(context, coll)
                .withTitle("This is an item")
                .build();
        });
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(item));
    }

    @Test
    public void testNestedCommunitiesWithNonemptyCollectionAdmin() throws Exception {
        Item item = withSuppressedAuthorization(() -> {
            Community parent = CommunityBuilder
                .createCommunity(context)
                .withName("parent community")
                .withAdminGroup(eperson)
                .build();
            Community child = CommunityBuilder
                .createSubCommunity(context, parent)
                .withName("child community")
                .withAdminGroup(eperson)
                .build();
            Collection coll = CollectionBuilder
                .createCollection(context, child)
                .withName("This collection contains an item")
                .build();
            return ItemBuilder
                .createItem(context, coll)
                .withTitle("This is an item")
                .build();
        });
        expectSomeResults(requestSitewideEditItemFeature());
        expectSomeResults(requestEditItemFeature(item));
    }

    private ResultActions requestSitewideEditItemFeature() throws Exception {
        return requestEditItemFeature(siteUri);
    }

    private ResultActions requestEditItemFeature(Item item) throws Exception {
        return requestEditItemFeature(getItemUri(item));
    }
    private ResultActions requestEditItemFeature(String uri) throws Exception {
        epersonToken = getAuthToken(eperson.getEmail(), password);
        return getClient(epersonToken).perform(get("/api/authz/authorizations/search/object?")
            .param("uri", uri)
            .param("feature", editItemFeature.getName())
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

    private String getItemUri(Item item) {
        ItemRest itemRest = itemConverter.convert(item, DefaultProjection.DEFAULT);
        return utils.linkToSingleResource(itemRest, "self").getHref();
    }
}
