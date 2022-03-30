/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.ShowClaimItemFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of Show Claim Item Authorization Feature implementation.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ShowClaimItemFeatureIT extends AbstractControllerIntegrationTest {

    private Item collectionAProfile;
    private Item collectionBProfile;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private Utils utils;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private AuthorizationFeature showClaimProfileFeature;

    private Collection personCollection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        personCollection =
            CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Person")
                             .withName("claimableA").build();
        final Collection claimableCollectionB =
            CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Person")
                             .withName("claimableB").build();

        collectionAProfile = ItemBuilder.createItem(context, personCollection).build();
        collectionBProfile = ItemBuilder.createItem(context, claimableCollectionB).build();

        configurationService.addPropertyValue("claimable.entityType", "Person");

        context.restoreAuthSystemState();

        showClaimProfileFeature = authorizationFeatureService.find(ShowClaimItemFeature.NAME);

    }

    @Test
    public void testCanClaimAProfile() throws Exception {

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(collectionAProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", showClaimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").exists())
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(collectionBProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", showClaimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").exists())
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));

    }

    @Test
    public void testNotClaimableEntity() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publicationCollection = CollectionBuilder
            .createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("notClaimable")
            .build();

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publicationCollection).build();

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(publication))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", showClaimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").doesNotExist())
                        .andExpect(jsonPath("$.page.totalElements", equalTo(0)));

    }

    @Test
    public void testItemAlreadyInARelation() throws Exception {

        context.turnOffAuthorisationSystem();

        Item ownedItem = ItemBuilder.createItem(context, personCollection)
                                    .withDspaceObjectOwner("owner", "ownerAuthority").build();
        context.restoreAuthSystemState();

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(ownedItem))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", showClaimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").exists())
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));

    }

    @Test
    public void testUserWithProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, personCollection)
            .withTitle("User")
            .withDspaceObjectOwner("User", context.getCurrentUser().getID().toString())
            .build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(context.getCurrentUser().getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                .param("uri", uri(collectionAProfile))
                .param("eperson", context.getCurrentUser().getID().toString())
                .param("feature", showClaimProfileFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded").doesNotExist())
            .andExpect(jsonPath("$.page.totalElements", equalTo(0)));
    }

    @Test
    public void testWithoutClaimableEntities() throws Exception {

        configurationService.setProperty("claimable.entityType", null);

        getClient(getAuthToken(context.getCurrentUser().getEmail(), password))
            .perform(get("/api/authz/authorizations/search/object")
                .param("uri", uri(collectionAProfile))
                .param("eperson", context.getCurrentUser().getID().toString())
                .param("feature", showClaimProfileFeature.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded").doesNotExist())
            .andExpect(jsonPath("$.page.totalElements", equalTo(0)));

    }

    private String uri(Item item) {
        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemRestURI = utils.linkToSingleResource(itemRest, "self").getHref();
        return itemRestURI;
    }

}
