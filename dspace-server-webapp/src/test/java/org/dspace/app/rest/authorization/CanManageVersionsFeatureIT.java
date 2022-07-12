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

import org.dspace.app.rest.authorization.impl.CanManageVersionsFeature;
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
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageVersions authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanManageVersionsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private Item itemA;
    private ItemRest itemARest;
    private Community communityA;
    private Collection collectionA;
    private AuthorizationFeature canManageVersionsFeature;

    final String feature = "canManageVersions";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canManageVersionsFeature = authorizationFeatureService.find(CanManageVersionsFeature.NAME);

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA").build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                                       .withName("collectionA").build();

        itemA = ItemBuilder.createItem(context, collectionA)
                           .withTitle("itemA").build();

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
    public void canManageVersionsFeatureAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminComA = EPersonBuilder.createEPerson(context)
                                          .withEmail("testComAdminA@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminComB = EPersonBuilder.createEPerson(context)
                                          .withEmail("testComBdminA@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("testCol1Admin@test.com")
                                          .withPassword(password)
                                          .build();

        EPerson adminCol2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("testCol2Admin@test.com")
                                          .withPassword(password)
                                          .build();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        Community subCommunityA = CommunityBuilder.createSubCommunity(context, rootCommunity)
                                                  .withName("Sub Community A")
                                                  .withAdminGroup(adminComA)
                                                  .build();

        CommunityBuilder.createSubCommunity(context, rootCommunity)
                        .withName("Sub Community B")
                        .withAdminGroup(adminComB)
                        .build();

        Collection col1 = CollectionBuilder.createCollection(context, subCommunityA)
                                          .withName("Collection 1")
                                          .withSubmitterGroup(eperson)
                                          .withAdminGroup(adminCol1)
                                          .build();

        CollectionBuilder.createCollection(context, subCommunityA)
                         .withName("Collection 2")
                         .withAdminGroup(adminCol2)
                         .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item")
                                .withIssueDate("2021-04-19")
                                .withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdminComA = getAuthToken(adminComA.getEmail(), password);
        String tokenAdminComB = getAuthToken(adminComB.getEmail(), password);
        String tokenAdminCol1 = getAuthToken(adminCol1.getEmail(), password);
        String tokenAdminCol2 = getAuthToken(adminCol2.getEmail(), password);

        // define authorizations that we know must exists
        Authorization adminOfComAToItemA = new Authorization(adminComA, canManageVersionsFeature, itemRestA);
        Authorization adminOfCol1ToItemA = new Authorization(adminCol1, canManageVersionsFeature, itemRestA);

        // define authorization that we know not exists
        Authorization adminOfComBToItemA = new Authorization(adminComB, canManageVersionsFeature, itemRestA);
        Authorization adminOfCol2ToItemA = new Authorization(adminCol2, canManageVersionsFeature, itemRestA);

        getClient(tokenAdminComA).perform(get("/api/authz/authorizations/" + adminOfComAToItemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                            AuthorizationMatcher.matchAuthorization(adminOfComAToItemA))));

        getClient(tokenAdminCol1).perform(get("/api/authz/authorizations/" + adminOfCol1ToItemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.is(
                                            AuthorizationMatcher.matchAuthorization(adminOfCol1ToItemA))));

        getClient(tokenAdminComB).perform(get("/api/authz/authorizations/" + adminOfComBToItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAdminCol2).perform(get("/api/authz/authorizations/" + adminOfCol2ToItemA.getID()))
                                 .andExpect(status().isNotFound());
    }

}
