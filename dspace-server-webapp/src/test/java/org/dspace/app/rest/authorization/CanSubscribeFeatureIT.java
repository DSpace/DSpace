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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.impl.CanSubscribeFeature;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of Subscribe Dso Feature implementation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CanSubscribeFeatureIT extends AbstractControllerIntegrationTest {

    private static final Logger log = LogManager.getLogger(CanSubscribeFeatureIT.class);

    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private CollectionConverter collectionConverter;
    @Autowired
    private CommunityConverter communityConverter;
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;
    @Autowired
    private ResourcePolicyService resourcePolicyService;

    private Community communityAuthorized;
    private Collection collectionAuthorized;
    private AuthorizationFeature canSubscribeFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .build();
        communityAuthorized = CommunityBuilder.createCommunity(context)
                                              .withName("communityA")
                                              .build();
        collectionAuthorized = CollectionBuilder.createCollection(context, communityAuthorized)
                                                .withName("Collection A")
                                                .build();
        context.restoreAuthSystemState();
        canSubscribeFeature = authorizationFeatureService.find(CanSubscribeFeature.NAME);
    }

    @Test
    public void canSubscribeCommunityAndCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CommunityRest comRest = communityConverter.convert(parentCommunity, DefaultProjection.DEFAULT);
        CollectionRest colRest = collectionConverter.convert(collectionAuthorized, DefaultProjection.DEFAULT);

        // define authorizations that we know must exists
        Authorization epersonToCommunity = new Authorization(eperson, canSubscribeFeature, comRest);
        Authorization adminToCommunity = new Authorization(admin, canSubscribeFeature, comRest);
        Authorization epersonToCollection = new Authorization(eperson, canSubscribeFeature, colRest);
        Authorization adminToCollection = new Authorization(admin, canSubscribeFeature, colRest);

        // define authorization that we know not exists
        Authorization anonymousToCommunity = new Authorization(null, canSubscribeFeature, comRest);
        Authorization anonymousToCollection = new Authorization(null, canSubscribeFeature, colRest);
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + epersonToCommunity.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", Matchers.is(
                                          AuthorizationMatcher.matchAuthorization(epersonToCommunity))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminToCommunity.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                        AuthorizationMatcher.matchAuthorization(adminToCommunity))));

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + epersonToCollection.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", Matchers.is(
                                          AuthorizationMatcher.matchAuthorization(epersonToCollection))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminToCollection.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                        AuthorizationMatcher.matchAuthorization(adminToCollection))));

        getClient().perform(get("/api/authz/authorizations/" + anonymousToCommunity.getID()))
                   .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymousToCollection.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void canNotSubscribeItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                                                              .withCanLogin(true)
                                                              .withPassword(password)
                                                              .withEmail("test@email.it")
                                                              .build();
        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                                                    .withName("Group A")
                                                    .addMember(eperson)
                                                    .build();
        Item item = ItemBuilder.createItem(context, collectionAuthorized)
                               .withTitle("Test item")
                               .build();

        cleanUpPermissions(resourcePolicyService.find(context, item));
        setPermissions(item, groupWithReadPermission, Constants.READ);

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        // define authorization that we know not exists
        Authorization anonymousToItem = new Authorization(null, canSubscribeFeature, itemRest);
        Authorization epersonToItem = new Authorization(eperson, canSubscribeFeature, itemRest);
        Authorization adminToItem = new Authorization(admin, canSubscribeFeature, itemRest);
        Authorization ePersonNotSubscribePermissionToItem = new Authorization(ePersonNotSubscribePermission,
                                                                              canSubscribeFeature, itemRest);

        context.restoreAuthSystemState();

        String token1 = getAuthToken(eperson.getEmail(), password);
        String token2 = getAuthToken(admin.getEmail(), password);
        String token3 = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);

        getClient(token1).perform(get("/api/authz/authorizations/" + epersonToItem.getID()))
                         .andExpect(status().isNotFound());

        getClient(token2).perform(get("/api/authz/authorizations/" + adminToItem.getID()))
                         .andExpect(status().isNotFound());

        getClient(token3).perform(get("/api/authz/authorizations/" + ePersonNotSubscribePermissionToItem.getID()))
                         .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymousToItem.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void canNotSubscribeCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                                                              .withCanLogin(true)
                                                              .withPassword(password)
                                                              .withEmail("test@email.it")
                                                              .build();

        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                                                    .withName("Group A")
                                                    .addMember(eperson)
                                                    .build();

        cleanUpPermissions(resourcePolicyService.find(context, collectionAuthorized));
        setPermissions(collectionAuthorized, groupWithReadPermission, Constants.READ);

        CollectionRest collectionRest = collectionConverter.convert(collectionAuthorized, Projection.DEFAULT);

        // define authorizations that we know must exists
        Authorization epersonToCollection = new Authorization(eperson, canSubscribeFeature, collectionRest);
        Authorization adminToCollection = new Authorization(admin, canSubscribeFeature, collectionRest);

        // define authorization that we know not exists
        Authorization ePersonNotSubscribePermissionToColl = new Authorization(ePersonNotSubscribePermission,
                                                                              canSubscribeFeature, collectionRest);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + epersonToCollection.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", Matchers.is(
                                          AuthorizationMatcher.matchAuthorization(epersonToCollection))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminToCollection.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                        AuthorizationMatcher.matchAuthorization(adminToCollection))));

        getClient(token).perform(get("/api/authz/authorizations/" + ePersonNotSubscribePermissionToColl.getID()))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void canNotSubscribeCommunityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePersonNotSubscribePermission = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withPassword(password)
                .withEmail("test@email.it")
                .build();

        // the user to be tested is not part of the group with read permission
        Group groupWithReadPermission = GroupBuilder.createGroup(context)
                .withName("Group A")
                .addMember(eperson)
                .build();

        cleanUpPermissions(resourcePolicyService.find(context, communityAuthorized));
        setPermissions(communityAuthorized, groupWithReadPermission, Constants.READ);

        CommunityRest communityRest = communityConverter.convert(communityAuthorized, Projection.DEFAULT);

        // define authorizations that we know must exists
        Authorization epersonToComm = new Authorization(eperson, canSubscribeFeature, communityRest);
        Authorization adminToComm = new Authorization(admin, canSubscribeFeature, communityRest);

        // define authorization that we know not exists
        Authorization ePersonNotSubscribePermissionToComm = new Authorization(ePersonNotSubscribePermission,
                                                                              canSubscribeFeature, communityRest);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String token = getAuthToken(ePersonNotSubscribePermission.getEmail(), password);

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + epersonToComm.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", Matchers.is(
                                          AuthorizationMatcher.matchAuthorization(epersonToComm))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminToComm.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                        AuthorizationMatcher.matchAuthorization(adminToComm))));

        getClient(token).perform(get("/api/authz/authorizations/" + ePersonNotSubscribePermissionToComm.getID()))
                        .andExpect(status().isNotFound());
    }

    private void setPermissions(DSpaceObject dSpaceObject, Group group, Integer permissions) {
        try {
            ResourcePolicyBuilder.createResourcePolicy(context)
                                 .withDspaceObject(dSpaceObject)
                                 .withAction(permissions)
                                 .withGroup(group)
                                 .build();
        } catch (SQLException | AuthorizeException sqlException) {
            log.error(sqlException.getMessage());
        }
    }

    private void cleanUpPermissions(List<ResourcePolicy> resourcePolicies) {
        try {
            for (ResourcePolicy resourcePolicy : resourcePolicies) {
                ResourcePolicyBuilder.delete(resourcePolicy.getID());
            }
        } catch (SQLException | SearchServiceException | IOException sqlException) {
            log.error(sqlException.getMessage());
        }
    }

}