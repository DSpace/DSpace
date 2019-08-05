/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.model.HarvestTypeEnum;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CollectionHarvestSettingsControllerIT extends AbstractControllerIntegrationTest {

    Collection collection;

    @Autowired
    HarvestedCollectionService harvestedCollectionService;

    @Before
    public void SetUp() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community community = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        collection = CollectionBuilder.createCollection(context, community).withName("Collection 1").build();



        context.restoreAuthSystemState();
    }

    public JSONObject createHarvestSettingsJson(String harvestType,
                                                String oaiSource, String oaiSetId, String metadataConfigId) {
        JSONObject json = new JSONObject();
        json.put("harvest_type", harvestType);
        json.put("oai_source", oaiSource);
        json.put("oai_set_id", oaiSetId);
        json.put("metadata_config_id", metadataConfigId);
        return json;
    }

    @Test
    public void EndpointWorksWithStandardSettings() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://dspace.mit.edu/oai/request", "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isOk());

        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        assertTrue(harvestedCollection.getHarvestType() == HarvestTypeEnum.valueOf(json.getString("harvest_type")).getValue());
        assertTrue(harvestedCollection.getOaiSource().equals(json.getString("oai_source")));
        assertTrue(harvestedCollection.getOaiSetId().equals(json.getString("oai_set_id")));
        assertTrue(harvestedCollection.getHarvestMetadataConfig().equals(json.getString("metadata_config_id")));
    }

    @Test
    public void HarvestSettingsDeletedIfHarvestTypeIsNone() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("NONE", "", "", "");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isOk());

        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        assertNull(harvestedCollection);
    }

    @Test
    public void UnauthorizedIfNotAuthenticated() throws Exception {
        getClient().perform(put("/api/core/collections/" + collection.getID() + "/harvester")
            .contentType("application/json"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void ForbiddenIfNotEnoughpermissions() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put("/api/core/collections/" + collection.getID() + "/harvester")
            .contentType("application/json"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void NotFoundIfNoSuchCollection() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        String fakeUuid = "6c9a081e-f2e5-42cd-8cf8-338f64b0841b";
        getClient(token).perform(put("/api/core/collections/" + fakeUuid + "/harvester")
            .contentType("application/json"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void UnprocessableEntityIfHarvestTypeIncorrect() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("INCORRECT_HARVEST_TYPE", "https://dspace.mit.edu/oai/request", "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isUnprocessableEntity());
    }
}
