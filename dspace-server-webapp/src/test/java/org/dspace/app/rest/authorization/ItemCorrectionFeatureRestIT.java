/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.dspace.app.rest.matcher.AuthorizationMatcher.matchAuthorization;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.ItemCorrectionFeature;
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
 * Integration test via REST for {@link ItemCorrectionFeature}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemCorrectionFeatureRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private AuthorizationFeature canCorrectItem;

    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        canCorrectItem = authorizationFeatureService.find(ItemCorrectionFeature.NAME);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Test community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .withRelationshipType("Publication")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testFeatureWithCorrectionDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .build();

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        configurationService.setProperty("item-correction.permit-all", false);
        configurationService.setProperty("item-correction.enabled", false);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(admin.getID()))
            .param("feature", ItemCorrectionFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFeatureWithAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .build();

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        configurationService.setProperty("item-correction.permit-all", false);

        String token = getAuthToken(admin.getEmail(), password);

        Authorization expectedAuthorization = new Authorization(admin, canCorrectItem, itemRest);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(admin.getID()))
            .param("feature", ItemCorrectionFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", hasItem(matchAuthorization(expectedAuthorization))));

    }

    @Test
    public void testFeatureWithNoAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .build();

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        configurationService.setProperty("item-correction.permit-all", false);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ItemCorrectionFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    @Test
    public void testFeatureWithPermitAllEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .build();

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        configurationService.setProperty("item-correction.permit-all", true);

        Authorization expectedAuthorization = new Authorization(eperson, canCorrectItem, itemRest);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                .param("uri", getItemUri(itemRest))
                .param("eperson", String.valueOf(eperson.getID()))
                .param("feature", ItemCorrectionFeature.NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.authorizations", hasItem(matchAuthorization(expectedAuthorization))));

    }

    public String getItemUri(ItemRest itemRest) {
        return utils.linkToSingleResource(itemRest, "self").getHref();
    }
}
