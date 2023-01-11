/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.CanSynchronizeWithORCID;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.ItemRest;
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canSynchronizeWithORCID authorization feature.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class CanSynchronizeWithORCIDIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ConfigurationService configurationService;

    final String feature = "canSynchronizeWithORCID";
    private Item itemA;
    private ItemRest itemARest;
    private Community communityA;
    private Collection collectionA;
    private AuthorizationFeature canSynchronizeWithORCID;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        canSynchronizeWithORCID = authorizationFeatureService.find(CanSynchronizeWithORCID.NAME);

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA").build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                                       .withName("collectionA").build();

        itemA = ItemBuilder.createItem(context, collectionA)
                           .withTitle("itemA")
                           .withDspaceObjectOwner("user" , context.getCurrentUser().getID().toString())
                           .build();

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
    public void testCanSynchronizeWithORCIDIfItemDoesNotHasDspaceObjectOwner() throws Exception {

        EPerson user = context.getCurrentUser();

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collectionA)
                           .withTitle("item")
                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(item))
                                     .param("eperson", user.getID().toString())
                                     .param("feature", canSynchronizeWithORCID.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void testCanSynchronizeWithORCIDIfItemHasDspaceObjectOwnerOfAnotherUUID() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson anotherUser = EPersonBuilder.createEPerson(context)
                                            .withEmail("user@example.com")
                                            .withPassword(password)
                                            .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(anotherUser.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(itemA))
                                     .param("eperson", anotherUser.getID().toString())
                                     .param("feature", canSynchronizeWithORCID.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void testCanSynchronizeWithORCIDIfItemHasDspaceObjectOwner() throws Exception {

        EPerson user = context.getCurrentUser();

        String token = getAuthToken(user.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(itemA))
                                     .param("eperson", user.getID().toString())
                                     .param("feature", canSynchronizeWithORCID.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").exists())
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    public void testCanSynchronizeWithORCIDWithSynchronizationDisabled() throws Exception {

        configurationService.setProperty("orcid.synchronization-enabled", false);

        EPerson user = context.getCurrentUser();

        String token = getAuthToken(user.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(itemA))
                                     .param("eperson", user.getID().toString())
                                     .param("feature", canSynchronizeWithORCID.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    private String uri(Item item) {
        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemRestURI = utils.linkToSingleResource(itemRest, "self").getHref();
        return itemRestURI;
    }

}
