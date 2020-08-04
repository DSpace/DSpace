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

import org.dspace.app.rest.authorization.impl.CCLicenseFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the ccLicense feature
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class CCLicenseFeatureRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private Utils utils;

    private AuthorizationFeature ccLicenseFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ccLicenseFeature = authorizationFeatureService.find(CCLicenseFeature.NAME);
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
        Authorization authAdminCCLicense = new Authorization(admin, ccLicenseFeature, itemRest);

        // access the authorization for the admin user
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))));

        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", admin.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))))
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
        Authorization authAdminCCLicense = new Authorization(eperson, ccLicenseFeature, itemRest);

        // access the authorization for the community admin user
        String comAdminToken = getAuthToken(eperson.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))));

        getClient(comAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))))
            );

        // verify that the property core.authorization.collection-admin.item-admin.cc-license = false is respected
        // the community admins should be still authorized
        configurationService.setProperty("core.authorization.item-admin.cc-license", false);
        configurationService.setProperty("core.authorization.collection-admin.item-admin.cc-license", false);
        getClient(comAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))));

        getClient(comAdminToken).perform(get("/api/authz/authorizations/search/object")
            .param("uri", itemUri)
            .param("eperson", eperson.getID().toString())
            .param("feature", ccLicenseFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations", contains(
                        Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))))
        );

        // now verify that the property core.authorization.community-admin.item-admin.cc-license = false is respected
        // and also community admins are blocked
        // Please note that set to false the configuration for community keeping true for collection don't
        // make any sense as a community admin is always also a collection admin
        configurationService.setProperty("core.authorization.community-admin.item-admin.cc-license", false);
        getClient(comAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isNotFound());

        getClient(comAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
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
        Authorization authAdminCCLicense = new Authorization(eperson, ccLicenseFeature, itemRest);

        // access the authorization for the admin user
        String colAdminToken = getAuthToken(eperson.getEmail(), password);
        getClient(colAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))));

        getClient(colAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))))
            );
        // verify that the property core.authorization.collection-admin.item-admin.cc-license = false is respected
        configurationService.setProperty("core.authorization.item-admin.cc-license", false);
        configurationService.setProperty("core.authorization.collection-admin.item-admin.cc-license", false);
        getClient(colAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isNotFound());

        getClient(colAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void checkAuthorizationAsItemAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("A community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("A collection").build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item to withdraw").build();
        ResourcePolicy resource = ResourcePolicyBuilder.createResourcePolicy(context).withAction(Constants.ADMIN)
                .withUser(eperson).withDspaceObject(item).build();
        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization authAdminCCLicense = new Authorization(eperson, ccLicenseFeature, itemRest);

        // access the authorization for the admin user
        String itemAdminToken = getAuthToken(eperson.getEmail(), password);
        getClient(itemAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))));

        getClient(itemAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", contains(
                    Matchers.is(AuthorizationMatcher.matchAuthorization(authAdminCCLicense))))
            );
        // verify that the property core.authorization.item-admin.cc-license = false is respected
        configurationService.setProperty("core.authorization.item-admin.cc-license", false);
        getClient(itemAdminToken).perform(get("/api/authz/authorizations/" + authAdminCCLicense.getID()))
                    .andExpect(status().isNotFound());

        getClient(itemAdminToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
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
        Authorization authEpersonCCLicense = new Authorization(eperson, ccLicenseFeature, itemRest);
        Authorization authAnonymousCCLicense = new Authorization(null, ccLicenseFeature, itemRest);

        // check the authorization for a normal user
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + authEpersonCCLicense.getID()))
                    .andExpect(status().isNotFound());

        getClient(epersonToken).perform(get("/api/authz/authorizations/search/object")
                .param("uri", itemUri)
                .param("eperson", eperson.getID().toString())
                .param("feature", ccLicenseFeature.getName()))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        // check the authorization for the anonymous user
        getClient().perform(get("/api/authz/authorizations/" + authAnonymousCCLicense.getID()))
                .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/search/object")
            .param("uri", itemUri)
            .param("feature", ccLicenseFeature.getName()))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }
}
