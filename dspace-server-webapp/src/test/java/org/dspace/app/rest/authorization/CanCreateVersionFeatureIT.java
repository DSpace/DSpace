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

import org.dspace.app.rest.authorization.impl.CanCreateVersionFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canCreateVersion authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanCreateVersionFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private Item itemA;
    private Item itemB;
    private EPerson user;
    private ItemRest itemARest;
    private Community communityA;
    private Collection collectionA;
    private AuthorizationFeature canCreateVersionFeature;

    final String feature = "canCreateVersion";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canCreateVersionFeature = authorizationFeatureService.find(CanCreateVersionFeature.NAME);

        user = EPersonBuilder.createEPerson(context)
                             .withEmail("userEmail@test.com")
                             .withPassword(password).build();

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA").build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                                       .withName("collectionA").build();

        itemA = ItemBuilder.createItem(context, collectionA)
                           .withTitle("Item A").build();

        itemB = ItemBuilder.createItem(context, collectionA)
                           .withTitle("Item B").build();

        context.restoreAuthSystemState();

        itemARest = itemConverter.convert(itemA, Projection.DEFAULT);
    }

    @Test
    public void anonymousHasNotAccessTest() throws Exception {
        getClient().perform(get("/api/authz/authorizations/search/object")
                   .param("embed", "feature")
                   .param("feature", feature)
                   .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void epersonHasNotAccessTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/search/object")
                               .param("embed", "feature")
                               .param("feature", feature)
                               .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.page.totalElements", is(0)))
                            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminItemSuccessTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                             .param("embed", "feature")
                             .param("feature", feature)
                             .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                          .andExpect(status().isOk())
                          .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                          .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void submitterItemSuccessTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("versioning.submitterCanCreateNewVersion", true);
        itemA.setSubmitter(user);

        context.restoreAuthSystemState();

        String userToken = getAuthToken(user.getEmail(), password);
        getClient(userToken).perform(get("/api/authz/authorizations/search/object")
                            .param("embed", "feature")
                            .param("feature", feature)
                            .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                         .andExpect(status().isOk())
                         .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                         .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void submitterItemWithPropertySubmitterCanCreateNewVersionIsFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("versioning.submitterCanCreateNewVersion", false);
        itemA.setSubmitter(user);

        context.restoreAuthSystemState();

        String userToken = getAuthToken(user.getEmail(), password);
        getClient(userToken).perform(get("/api/authz/authorizations/search/object")
                            .param("embed", "feature")
                            .param("feature", feature)
                            .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                         .andExpect(status().isOk())
                         .andExpect(jsonPath("$.page.totalElements", is(0)))
                         .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkCanCreateVersionsFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("versioning.submitterCanCreateNewVersion", true);
        itemA.setSubmitter(user);
        itemB.setSubmitter(admin);

        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenUser = getAuthToken(user.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canCreateVersionFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canCreateVersionFeature, itemRestB);
        Authorization user2ItemA = new Authorization(user, canCreateVersionFeature, itemRestA);

        // define authorization that we know not exists
        Authorization eperson2ItemA = new Authorization(eperson, canCreateVersionFeature, itemRestA);
        Authorization eperson2ItemB = new Authorization(eperson, canCreateVersionFeature, itemRestB);
        Authorization user2ItemB = new Authorization(user, canCreateVersionFeature, itemRestB);


        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenUser).perform(get("/api/authz/authorizations/" + user2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user2ItemA))));

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemA.getID()))
                               .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemB.getID()))
                               .andExpect(status().isNotFound());

        getClient(tokenUser).perform(get("/api/authz/authorizations/" + user2ItemB.getID()))
                            .andExpect(status().isNotFound());

    }
}