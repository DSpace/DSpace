/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.CanManageBitstreamBundlesFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageBitstreamBundles authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanManageBitstreamBundlesFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private Item itemA;
    private Item itemB;
    private EPerson userA;
    private EPerson userB;
    private EPerson userColAadmin;
    private EPerson userColBadmin;
    private EPerson userComAdmin;
    private Community communityA;
    private Collection collectionA;
    private Collection collectionB;
    private AuthorizationFeature canManageBitstreamBundlesFeature;

    final String feature = "canManageBitstreamBundles";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canManageBitstreamBundlesFeature = authorizationFeatureService.find(CanManageBitstreamBundlesFeature.NAME);

        userA = EPersonBuilder.createEPerson(context)
                             .withEmail("userEmail@test.com")
                             .withPassword(password).build();

        userB = EPersonBuilder.createEPerson(context)
                              .withEmail("userB.email@test.com")
                              .withPassword(password).build();

        userColAadmin = EPersonBuilder.createEPerson(context)
                                     .withEmail("userColAadmin@test.com")
                                     .withPassword(password).build();

        userColBadmin = EPersonBuilder.createEPerson(context)
                                      .withEmail("userColBadmin@test.com")
                                      .withPassword(password).build();

        userComAdmin = EPersonBuilder.createEPerson(context)
                                     .withEmail("userComAdmin@test.com")
                                     .withPassword(password).build();

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA")
                                     .withAdminGroup(userComAdmin).build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                                       .withName("Collection A")
                                       .withAdminGroup(userColAadmin).build();

        collectionB = CollectionBuilder.createCollection(context, communityA)
                                       .withName("Collection B")
                                       .withAdminGroup(userColBadmin).build();

        itemA = ItemBuilder.createItem(context, collectionA)
                           .withTitle("Item A").build();

        itemB = ItemBuilder.createItem(context, collectionB)
                           .withTitle("Item B").build();
        context.restoreAuthSystemState();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkCanCreateVersionsFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //permissions for userA
        authorizeService.addPolicy(context, itemA, Constants.ADD, userA);
        authorizeService.addPolicy(context, itemA, Constants.REMOVE, userA);
        // permissions for userB
        authorizeService.addPolicy(context, itemA, Constants.REMOVE, userB);
        authorizeService.addPolicy(context, itemB, Constants.REMOVE, userB);
        authorizeService.addPolicy(context, itemB, Constants.ADD, userB);

        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenBUser = getAuthToken(userB.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);
        String tokenColBadmin = getAuthToken(userColBadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userB2ItemB = new Authorization(userB, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userB2ItemA = new Authorization(userB, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemB = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestB);
        Authorization eperson2ItemA = new Authorization(eperson, canManageBitstreamBundlesFeature, itemRestA);
        Authorization eperson2ItemB = new Authorization(eperson, canManageBitstreamBundlesFeature, itemRestB);
        Authorization anonymous2ItemA = new Authorization(null, canManageBitstreamBundlesFeature, itemRestA);
        Authorization anonymous2ItemB = new Authorization(null, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colAadmin2ItemB = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colBadmin2ItemA = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userA2ItemA))));

        getClient(tokenBUser).perform(get("/api/authz/authorizations/" + userB2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userB2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenBUser).perform(get("/api/authz/authorizations/" + userB2ItemA.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemB.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemA.getID()))
                               .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemB.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemA.getID()))
                   .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemB.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCreateBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyDeleteBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCollectionAdminCreateBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.collection-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);
        String tokenColBadmin = getAuthToken(userColBadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemB))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCollectionAdminDeleteBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.collection-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);
        String tokenColBadmin = getAuthToken(userColBadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemB))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCommunityAdminCreateBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.community-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);
        String tokenColBadmin = getAuthToken(userColBadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCommunityAdminDeleteBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.community-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);
        String tokenColBadmin = getAuthToken(userColBadmin.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

}
