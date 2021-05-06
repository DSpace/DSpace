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

import org.dspace.app.rest.authorization.impl.CanManageRelationshipsFeature;
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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageRelationships authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanManageRelationshipsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private AuthorizeService authorizeService;

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
    private AuthorizationFeature canManageRelationshipsFeature;

    final String feature = "canManageRelationships";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canManageRelationshipsFeature = authorizationFeatureService.find(CanManageRelationshipsFeature.NAME);

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
    public void canManageRelationshipsFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // permissions for userA
        authorizeService.addPolicy(context, itemA, Constants.WRITE, userA);
        // permissions for userB
        authorizeService.addPolicy(context, itemB, Constants.WRITE, userB);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenColAadmin = getAuthToken(userColAadmin.getEmail(), password);
        String tokenColBadmin = getAuthToken(userColBadmin.getEmail(), password);
        String tokenAUser = getAuthToken(userA.getEmail(), password);
        String tokenBUser = getAuthToken(userB.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageRelationshipsFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageRelationshipsFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageRelationshipsFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageRelationshipsFeature, itemRestB);

        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageRelationshipsFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageRelationshipsFeature, itemRestB);

        Authorization userA2ItemA = new Authorization(userA, canManageRelationshipsFeature, itemRestA);
        Authorization userB2ItemB = new Authorization(userB, canManageRelationshipsFeature, itemRestB);

        // define authorization that we know not exists
        Authorization userB2ItemA = new Authorization(userB, canManageRelationshipsFeature, itemRestA);
        Authorization userA2ItemB = new Authorization(userA, canManageRelationshipsFeature, itemRestB);
        Authorization eperson2ItemA = new Authorization(eperson, canManageRelationshipsFeature, itemRestA);
        Authorization eperson2ItemB = new Authorization(eperson, canManageRelationshipsFeature, itemRestB);
        Authorization anonymous2ItemA = new Authorization(null, canManageRelationshipsFeature, itemRestA);
        Authorization anonymous2ItemB = new Authorization(null, canManageRelationshipsFeature, itemRestB);
        Authorization colAadmin2ItemB = new Authorization(userColAadmin, canManageRelationshipsFeature, itemRestB);
        Authorization colBadmin2ItemA = new Authorization(userColBadmin, canManageRelationshipsFeature, itemRestA);

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
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colBadmin2ItemB))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userA2ItemA))));

        getClient(tokenBUser).perform(get("/api/authz/authorizations/" + userB2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userB2ItemB))));

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

}