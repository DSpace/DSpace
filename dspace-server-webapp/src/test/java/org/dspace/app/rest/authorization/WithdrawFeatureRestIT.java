/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.WithdrawFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the withdrawItem feature
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class WithdrawFeatureRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private Utils utils;

    private AuthorizationFeature withdrawFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        withdrawFeature = authorizationFeatureService.find(WithdrawFeature.NAME);
    }

    @Test
    public void authorizedAsAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("A community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("A collection").build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item to withdraw").build();
        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authAdminWithdraw = new Authorization(admin, withdrawFeature, itemRest);

        // access the authorization for the admin user
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authAdminWithdraw.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))));

        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", admin.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))))
            );
    }

    @Test
    public void checkAuthorizationAsCommunityAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("A community").withAdminGroup(eperson)
                .build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("A collection").build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item to withdraw").build();
        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authAdminWithdraw = new Authorization(eperson, withdrawFeature, itemRest);

        // access the authorization for the community admin user
        String comAdminToken = getAuthToken(eperson.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/authz/authorizations/" + authAdminWithdraw.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))));

        getClient(comAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))))
            );

        // verify that the property core.authorization.collection-admin.item.withdraw = false is respected
        // the community admins should be still authorized
        configurationService.setProperty("core.authorization.collection-admin.item.withdraw", false);
        getClient(comAdminToken).perform(get("/api/authz/authorizations/" + authAdminWithdraw.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))));

        getClient(comAdminToken).perform(get("/api/authz/authorizations/search/object")
            .param("uri", itemUri)
            .param("eperson", eperson.getID().toString())
            .param("feature", withdrawFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations", contains(
                        Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))))
        );

        // now verify that the property core.authorization.community-admin.item.withdraw = false is respected
        // and also community admins are blocked
        // Please note that set to false the configuration for community keeping true for collection don't
        // make any sense as a community admin is always also a collection admin
        configurationService.setProperty("core.authorization.community-admin.item.withdraw", false);
        getClient(comAdminToken).perform(get("/api/authz/authorizations/" + authAdminWithdraw.getID()))
                    .andExpect(status().isNotFound());

        getClient(comAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void checkAuthorizationAsCollectionAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("A community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("A collection")
                .withAdminGroup(eperson).build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item to withdraw").build();
        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authAdminWithdraw = new Authorization(eperson, withdrawFeature, itemRest);

        // access the authorization for the admin user
        String colAdminToken = getAuthToken(eperson.getEmail(), password);
        getClient(colAdminToken).perform(get("/api/authz/authorizations/" + authAdminWithdraw.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))));

        getClient(colAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminWithdraw))))
            );
        // verify that the property core.authorization.collection-admin.item.withdraw = false is respected
        configurationService.setProperty("core.authorization.collection-admin.item.withdraw", false);
        getClient(colAdminToken).perform(get("/api/authz/authorizations/" + authAdminWithdraw.getID()))
                    .andExpect(status().isNotFound());

        getClient(colAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void notAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("A community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("A collection").build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item to withdraw").build();
        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authEpersonWithdraw = new Authorization(eperson, withdrawFeature, itemRest);
        Authorization authAnonymousWithdraw = new Authorization(null, withdrawFeature, itemRest);

        // check the authorization for a normal user
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + authEpersonWithdraw.getID()))
                    .andExpect(status().isNotFound());

        getClient(epersonToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        // check the authorization for the anonymous user
        getClient().perform(get("/api/authz/authorizations/" + authAnonymousWithdraw.getID()))
                .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/search/object")
            .param("uri", itemUri)
            .param("feature", withdrawFeature.getName()))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void notAuthorizedInvalidStateTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("A community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("A collection")
                .withWorkflowGroup(1, eperson).build();

        Item withdrawnItem = ItemBuilder.createItem(context, col).withTitle("Item already withdrawn").withdrawn()
                .build();
        WorkspaceItem wsItem = WorkspaceItemBuilder.createWorkspaceItem(context, col).withTitle("A workspace item")
                .build();
        WorkflowItem wfItem = WorkflowItemBuilder.createWorkflowItem(context, col).withTitle("A workflow item").build();
        context.restoreAuthSystemState();

        ItemRest withdrawnItemRest = itemConverter.convert(withdrawnItem, Projection.DEFAULT);
        String withdrawnItemUri = utils.linkToSingleResource(withdrawnItemRest, "self").getHref();
        ItemRest wsItemRest = itemConverter.convert(wsItem.getItem(), Projection.DEFAULT);
        String wsItemUri = utils.linkToSingleResource(wsItemRest, "self").getHref();
        ItemRest wfItemRest = itemConverter.convert(wfItem.getItem(), Projection.DEFAULT);
        String wfItemUri = utils.linkToSingleResource(wfItemRest, "self").getHref();

        Authorization authWithdrawnItem = new Authorization(admin, withdrawFeature, withdrawnItemRest);
        Authorization authWsItem = new Authorization(admin, withdrawFeature, wsItemRest);
        Authorization authWFItem = new Authorization(admin, withdrawFeature, wfItemRest);
        // nor the admin should be authorized to withdraw the previous item
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authWithdrawnItem.getID()))
                    .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", withdrawnItemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        getClient(adminToken).perform(get("/api/authz/authorizations/" + authWsItem.getID()))
            .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", wsItemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        getClient(adminToken).perform(get("/api/authz/authorizations/" + authWFItem.getID()))
            .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", wfItemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", withdrawFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }
}
