/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.matcher.HarvesterMetadataMatcher;
import org.dspace.app.rest.matcher.MetadataConfigsMatcher;
import org.dspace.app.rest.model.HarvestTypeEnum;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for collection harvest settings controller
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public class CollectionHarvestSettingsControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    HarvestedCollectionService harvestedCollectionService;

    Collection collection;
    Collection collectionNoHarvestSettings;
    EPerson ePersonWithWriteRights;

    @Before
    public void SetUp() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community community = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
                                        .withName("Collection 1")
                                        .build();

        collectionNoHarvestSettings = CollectionBuilder.createCollection(context, community)
                                                        .withName("Collection 2")
                                                        .build();

        ePersonWithWriteRights = EPersonBuilder.createEPerson(context)
            .withEmail("email@email.com")
            .withPassword(password)
            .build();

        authorizeService.addPolicy(context, collection, Constants.WRITE, ePersonWithWriteRights);

        context.restoreAuthSystemState();
    }

    /**
     * Function to create a JSONObject containing the harvest settings
     * @param harvestType           The harvest type
     * @param oaiSource             The OAI source
     * @param oaiSetId              The OAI set id
     * @param metadataConfigId      The metadata config id
     * @return A JSONObject containing the given harvest settings
     */
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
    public void GetCollectionHarvestSettings() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();

        // Add harvest settings to collection
        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://dspace.org/oai/request",
                "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isOk());

        //Retrieve harvest settings
        getClient(token).perform(
            get("/api/core/collections/" + collection.getID() + "/harvester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HarvesterMetadataMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", HarvesterMetadataMatcher.matchLinks()))
                .andExpect(jsonPath("$.harvest_type", is("METADATA_ONLY")))
                .andExpect(jsonPath("$.oai_source", is("https://dspace.org/oai/request")))
                .andExpect(jsonPath("$.oai_set_id", is("col_1721.1_114174")))
                .andExpect(jsonPath("$.harvest_message", is(nullValue())))
                .andExpect(jsonPath("$.metadata_config_id", is("dc")))
                .andExpect(jsonPath("$.harvest_status", is("READY")))
                .andExpect(jsonPath("$.harvest_start_time", is(nullValue())))
                .andExpect(jsonPath("$.last_harvested", is(nullValue())))
                .andExpect(jsonPath("$._links.self.href",
                    endsWith("api/core/collections/" + collection.getID() + "/harvester")))
                .andExpect(jsonPath("$._embedded.harvestermetadata",  Matchers.allOf(
                    MetadataConfigsMatcher.matchMetadataConfigs(configs)
                )))
                .andExpect(jsonPath("$._embedded.harvestermetadata._links.self.href",
                    endsWith("/api/config/harvestermetadata")))
        ;
    }

    @Test
    public void GetCollectionHarvestSettingsIfNotAdmin() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void GetAndPutCollectionHarvestSettingsIfUserHasWriteRights() throws Exception {
        context.setCurrentUser(ePersonWithWriteRights);
        String token = getAuthToken(ePersonWithWriteRights.getEmail(), password);
        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://dspace.org/oai/request",
                "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isOk());

        getClient(token).perform(
            get("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json"))
            .andExpect(status().isOk());
    }

    @Test
    public void getAndPutCollectionHarvestSettingsAnonymousUserException() throws Exception {
        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://dspace.org/oai/request",
                "col_1721.1_114174", "dc");

        getClient().perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void GetAndPutCollectionHarvestSettingsIfUserHasNoWriteRightsException() throws Exception {
        context.setCurrentUser(eperson);
        String token = getAuthToken(eperson.getEmail(), password);
        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://dspace.org/oai/request",
                "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
                   .andExpect(status().isForbidden());
    }

    @Test
    public void getCollectionHarvestSettingsIfNotSet() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();

        getClient(token).perform(
            get("/api/core/collections/" + collectionNoHarvestSettings.getID() + "/harvester"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.harvest_type", is("NONE")))
            .andExpect(jsonPath("$.oai_source", is(nullValue())))
            .andExpect(jsonPath("$.oai_set_id", is(nullValue())))
            .andExpect(jsonPath("$.harvest_message", is(nullValue())))
            .andExpect(jsonPath("$.metadata_config_id", is(nullValue())))
            .andExpect(jsonPath("$.harvest_status", is(nullValue())))
            .andExpect(jsonPath("$.harvest_start_time", is(nullValue())))
            .andExpect(jsonPath("$.last_harvested", is(nullValue())))
            .andExpect(jsonPath("$._links.self.href",
                endsWith("api/core/collections/" + collectionNoHarvestSettings.getID() + "/harvester")))
            .andExpect(jsonPath("$._embedded.harvestermetadata",  Matchers.allOf(
                MetadataConfigsMatcher.matchMetadataConfigs(configs)
            )))
            .andExpect(jsonPath("$._embedded.harvestermetadata._links.self.href",
                endsWith("/api/config/harvestermetadata")));
    }

    @Test
    public void PutWorksWithStandardSettings() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://dspace.org/oai/request",
                "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isOk());

        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        assertTrue(harvestedCollection.getHarvestType()
            == HarvestTypeEnum.valueOf(json.getString("harvest_type")).getValue());
        assertTrue(harvestedCollection.getOaiSource().equals(json.getString("oai_source")));
        assertTrue(harvestedCollection.getOaiSetId().equals(json.getString("oai_set_id")));
        assertTrue(harvestedCollection.getHarvestMetadataConfig().equals(json.getString("metadata_config_id")));
    }

    @Test
    public void PutUnProcessableEntityIfIncorrectSettings() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("METADATA_ONLY", "https://mydspace.edu/oai/request",
                "col_1721.1_114174", "bc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void PutHarvestSettingsDeletedIfHarvestTypeIsNone() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("NONE", "", "", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isOk());

        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        assertNull(harvestedCollection);
    }

    @Test
    public void PutUnauthorizedIfNotAuthenticated() throws Exception {
        getClient().perform(put("/api/core/collections/" + collection.getID() + "/harvester")
            .contentType("application/json"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void PutForbiddenIfNotEnoughpermissions() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put("/api/core/collections/" + collection.getID() + "/harvester")
            .contentType("application/json"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void PutNotFoundIfNoSuchCollection() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        String fakeUuid = "6c9a081e-f2e5-42cd-8cf8-338f64b0841b";
        getClient(token).perform(put("/api/core/collections/" + fakeUuid + "/harvester")
            .contentType("application/json"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void PutUnprocessableEntityIfHarvestTypeIncorrect() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        JSONObject json = createHarvestSettingsJson("INCORRECT_HARVEST_TYPE", "https://mydspace.edu/oai/request",
                "col_1721.1_114174", "dc");

        getClient(token).perform(
            put("/api/core/collections/" + collection.getID() + "/harvester")
                .contentType("application/json")
                .content(json.toString()))
            .andExpect(status().isUnprocessableEntity());
    }
}
