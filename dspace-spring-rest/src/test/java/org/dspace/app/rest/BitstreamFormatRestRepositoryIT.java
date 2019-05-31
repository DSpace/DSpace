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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.BitstreamFormatBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.converter.BitstreamFormatConverter;
import org.dspace.app.rest.matcher.BitstreamFormatMatcher;
import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;

import org.dspace.content.BitstreamFormat;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 * @author Maria Verdonck
 */

public class BitstreamFormatRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    BitstreamFormatConverter converter;

    @Test
    public void findAllPaginationTest() throws Exception {
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats")));
    }

    @Test
    @Ignore
    public void unknownFormatRequiredByDefault() throws Exception {
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats")))
                .andExpect(jsonPath("$._embedded.bitstreamformats", Matchers.is(
                        BitstreamFormatMatcher.matchBitstreamFormatMimeType("Unknown")
                )));
    }

    @Test
    @Ignore
    public void findAllMimeTypeCheck() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description")
                .build();
        getClient().perform(get("/api/core/bitstreamformats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$._embedded.bitstreamformats", Matchers.contains(
                        BitstreamFormatMatcher
                                .matchBitstreamFormat(bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())
                )));
    }

    @Test
    public void findOne() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description")
                .build();

        getClient().perform(get("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.description", is(bitstreamFormat.getDescription())))
                .andExpect(jsonPath("$.mimetype", is(bitstreamFormat.getMIMEType())))
                .andExpect(jsonPath("$.type", is("bitstreamformat")))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href",
                        endsWith("/api/core/bitstreamformats/" + bitstreamFormat.getID())));
    }

    @Test
    public void findOneNonExistentIDInURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BitstreamFormatRest bitstreamFormatRest = this.createRandomMockBitstreamRest(false);

        String nonExistentBitstreamFormatID = "404404404";

        getClient().perform(get("/api/core/bitstreamformats/" + nonExistentBitstreamFormatID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void createAdminAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BitstreamFormatRest bitstreamFormatRest = this.createRandomMockBitstreamRest(false);
        //Create bitstream format
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/bitstreamformats/")
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isCreated())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String newlyCreatedBitstreamID = String.valueOf(map.get("id"));
        //Verify creation
        getClient().perform(get("/api/core/bitstreamformats/" + newlyCreatedBitstreamID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.description", is(bitstreamFormatRest.getDescription())))
                .andExpect(jsonPath("$.mimetype", is(bitstreamFormatRest.getMimetype())))
                .andExpect(jsonPath("$.type", is("bitstreamformat")))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href",
                        endsWith("/api/core/bitstreamformats/" + newlyCreatedBitstreamID)));
    }

    @Test
    public void createNonValidSupportLevel() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BitstreamFormatRest bitstreamFormatRest = this.createRandomMockBitstreamRest(false);
        bitstreamFormatRest.setSupportLevel("NONVALID SUPPORT LVL");
        //Attempt to create bitstream with a non-valid support lvl
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/bitstreamformats/")
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createNoAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BitstreamFormatRest bitstreamFormatRest = this.createRandomMockBitstreamRest(false);

        //Try to create bitstreamFormat without auth token
        getClient(null).perform(post("/api/core/bitstreamformats/")
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createNonAdminAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BitstreamFormatRest bitstreamFormatRest = this.createRandomMockBitstreamRest(false);
        context.turnOffAuthorisationSystem();
        EPerson user = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("first", "last")
                .withEmail("testaze@gmail.com")
                .withPassword(password)
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .build();
        context.restoreAuthSystemState();
        //Try to create bitstreamFormat with non-admin auth token
        String token = getAuthToken(user.getEmail(), password);
        getClient(token).perform(post("/api/core/bitstreamformats/")
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createAlreadyExisting() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BitstreamFormatRest bitstreamFormatRest = this.createRandomMockBitstreamRest(true);
        //Try to create same bitstream twice
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/bitstreamformats/")
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isCreated());
        //Second time it fails
        getClient(token).perform(post("/api/core/bitstreamformats/")
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void updateAdminAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateAdminAccess")
                .build();
        context.restoreAuthSystemState();

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);
        String token = getAuthToken(admin.getEmail(), password);
        //Update it
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        getClient(token).perform(put("/api/core/bitstreamformats/" + bitstreamFormat.getID())
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isOk());

        //Verify change
        getClient().perform(get("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.description", is(bitstreamFormatRest.getDescription())))
                .andExpect(jsonPath("$.mimetype", is(bitstreamFormatRest.getMimetype())))
                .andExpect(jsonPath("$.type", is("bitstreamformat")))
                .andExpect(jsonPath("$._links.self.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.self.href",
                        endsWith("/api/core/bitstreamformats/" + bitstreamFormat.getID())));
    }

    @Test
    public void updateNonValidSupportLevel() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateAdminAccess")
                .build();
        context.restoreAuthSystemState();

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);
        String token = getAuthToken(admin.getEmail(), password);
        //Update it
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        bitstreamFormatRest.setSupportLevel("NONVALID SUPPORT LEVEL");
        getClient(token).perform(put("/api/core/bitstreamformats/" + bitstreamFormat.getID())
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateNonExistingIDInURLAndJSON() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNonExistingIDInURLAndJSON")
                .build();
        context.restoreAuthSystemState();

        int nonExistentBitstreamFormatID = 404404404;

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);
        String token = getAuthToken(admin.getEmail(), password);
        //Update it with non existent ID in URL and in JSON
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        bitstreamFormatRest.setId(nonExistentBitstreamFormatID);
        getClient(token).perform(put("/api/core/bitstreamformats/" + nonExistentBitstreamFormatID)
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateNonExistingIDInJustURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNonExistingIDInJustURL")
                .build();
        context.restoreAuthSystemState();

        int nonExistentBitstreamFormatID = 404404404;

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);
        String token = getAuthToken(admin.getEmail(), password);
        //Update it with non existent ID in URL
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        getClient(token).perform(put("/api/core/bitstreamformats/" + nonExistentBitstreamFormatID)
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateNonExistingIDInJSONButValidInURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNonExistingIDInJSONButValidInURL")
                .build();
        context.restoreAuthSystemState();

        int nonExistentBitstreamFormatID = 404404404;

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);
        String token = getAuthToken(admin.getEmail(), password);
        //Update it with non existent ID in JSON, but valid in URL
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        bitstreamFormatRest.setId(nonExistentBitstreamFormatID);
        getClient(token).perform(put("/api/core/bitstreamformats/" + bitstreamFormat.getID())
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateNotMatchingIDsInJSONAndURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat1 = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNotMatchingIDsInJSONAndURL 1")
                .build();
        BitstreamFormat bitstreamFormat2 = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNotMatchingIDsInJSONAndURL 2")
                .build();
        context.restoreAuthSystemState();

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat1);
        String token = getAuthToken(admin.getEmail(), password);
        //Update but id in body is not same id as in URL
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        bitstreamFormatRest.setId(bitstreamFormat2.getID());
        getClient(token).perform(put("/api/core/bitstreamformats/" + bitstreamFormat1.getID())
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateNoAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNoAccess")
                .build();
        context.restoreAuthSystemState();

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);

        //Try to update bitstreamFormat without auth token
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        getClient(null).perform(put("/api/core/bitstreamformats/" + bitstreamFormat.getID())
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateNonAdminAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateNonAdminAccess")
                .build();
        EPerson user = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("first", "last")
                .withEmail("testaze@gmail.com")
                .withPassword(password)
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .build();
        context.restoreAuthSystemState();

        BitstreamFormatRest bitstreamFormatRest = converter.fromModel(bitstreamFormat);
        String token = getAuthToken(user.getEmail(), password);

        //Try to update bitstreamFormat without non-admin auth token
        bitstreamFormatRest.setShortDescription("Test short UPDATED");
        getClient(token).perform(put("/api/core/bitstreamformats/" + bitstreamFormat.getID())
                .content(mapper.writeValueAsBytes(bitstreamFormatRest)).contentType(contentType))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteAdminAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateAdminAccess")
                .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        //Delete it
        getClient(token).perform(delete("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isNoContent());

        //Verify deleted
        getClient().perform(get("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteNonExistingID() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateAdminAccess")
                .build();
        context.restoreAuthSystemState();

        String nonExistentID = "404404404";

        String token = getAuthToken(admin.getEmail(), password);
        //Delete it
        getClient(token).perform(delete("/api/core/bitstreamformats/" + nonExistentID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteNoAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateAdminAccess")
                .build();
        context.restoreAuthSystemState();

        //Delete it
        getClient(null).perform(delete("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteNonAdminAccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        //Create bitstream format
        context.turnOffAuthorisationSystem();
        BitstreamFormat bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context)
                .withMimeType("application/octet-stream")
                .withDescription("Description - updateAdminAccess")
                .build();
        EPerson user = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("first", "last")
                .withEmail("testaze@gmail.com")
                .withPassword(password)
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .build();
        context.restoreAuthSystemState();

        //Delete it
        String token = getAuthToken(user.getEmail(), password);
        getClient(token).perform(delete("/api/core/bitstreamformats/" + bitstreamFormat.getID()))
                .andExpect(status().isForbidden());
    }

    private BitstreamFormatRest createRandomMockBitstreamRest(boolean withRand) {
        BitstreamFormatRest bitstreamFormatRest = new BitstreamFormatRest();
        String random = null;
        if (withRand) {
            Random rand = new Random();
            random = String.valueOf(rand.nextInt(100) + 1);
        }
        bitstreamFormatRest.setShortDescription("Test short" + random);
        bitstreamFormatRest.setDescription("Full description of Test short");
        bitstreamFormatRest.setMimetype("text/plain");
        bitstreamFormatRest.setSupportLevel("KNOWN");
        bitstreamFormatRest.setInternal(false);
        bitstreamFormatRest.setExtensions(Arrays.asList("txt", "asc"));
        return bitstreamFormatRest;
    }
}
