/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.ClarinLicenseRestRepository.OPERATION_PATH_LICENSE_RESOURCE;
import static org.dspace.app.rest.repository.ClarinUserMetadataRestController.CHECK_EMAIL_RESPONSE_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.ClarinUserMetadataBuilder;
import org.dspace.builder.ClarinUserRegistrationBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class ClarinUserMetadataRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ClarinLicenseService clarinLicenseService;
    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;
    @Autowired
    ClarinUserMetadataService clarinUserMetadataService;

    WorkspaceItem witem;
    WorkspaceItem witem2;
    ClarinLicense clarinLicense;
    Bitstream bitstream;
    Bitstream bitstream2;

    // Attach ClarinLicense to the Bitstream
    private void prepareEnvironment(String requiredInfo, Integer confirmation) throws Exception {
        // 1. Create Workspace Item with uploaded file
        // 2. Create Clarin License
        // 3. Send request to add Clarin License to the Workspace Item
        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        // 5. Check if the Clarin License was attached to the Bitstream

        // 1. Create WI with uploaded file
        context.turnOffAuthorisationSystem();
        witem = this.createWorkspaceItemWithFile(false);
        witem2 = this.createWorkspaceItemWithFile(true);

        List<Operation> replaceOperations = new ArrayList<Operation>();
        String clarinLicenseName = "Test Clarin License";

        // 2. Create clarin license with clarin license label
        clarinLicense = createClarinLicense(clarinLicenseName, "Test Def", requiredInfo, confirmation);

        // creating replace operation
        Map<String, String> licenseReplaceOpValue = new HashMap<String, String>();
        licenseReplaceOpValue.put("value", clarinLicenseName);
        replaceOperations.add(new ReplaceOperation("/" + OPERATION_PATH_LICENSE_RESOURCE,
                licenseReplaceOpValue));

        context.restoreAuthSystemState();
        String updateBody = getPatchContent(replaceOperations);

        // 3. Send request to add Clarin License to the Workspace Item
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem2.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value", is(clarinLicenseName)));
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem2.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value", is(clarinLicenseName)));

        // 5. Check if the Clarin License was attached to the Bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(2)));
    }

    @Test
    public void notAuthorizedUser_shouldReturnToken() throws Exception {
        this.prepareEnvironment("NAME", 0);
        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("NAME");
        clarinUserMetadata2.setMetadataValue("Test2");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);

        String adminToken = getAuthToken(admin.getEmail(), password);
        // Load bitstream from the item.
        getClient().perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void notAuthorizedUser_shouldSendEmail() throws Exception {
        this.prepareEnvironment("SEND_TOKEN", 0);
        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("NAME");
        clarinUserMetadata2.setMetadataValue("Test2");

        ClarinUserMetadataRest clarinUserMetadata3 = new ClarinUserMetadataRest();
        clarinUserMetadata3.setMetadataKey("SEND_TOKEN");

        ClarinUserMetadataRest clarinUserMetadata4 = new ClarinUserMetadataRest();
        clarinUserMetadata4.setMetadataKey("EXTRA_EMAIL");
        clarinUserMetadata4.setMetadataValue("test@test.edu");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);
        clarinUserMetadataRestList.add(clarinUserMetadata3);
        clarinUserMetadataRestList.add(clarinUserMetadata4);

        String adminToken = getAuthToken(admin.getEmail(), password);
        // Load bitstream from the item.
        getClient().perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void authorizedUserWithoutMetadata_shouldReturnToken() throws Exception {
        this.prepareEnvironment("NAME", 0);
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("ADDRESS");
        clarinUserMetadata2.setMetadataValue("Test2");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    @Test
    public void authorizedUserWithoutMetadata_shouldSendEmail() throws Exception {
        this.prepareEnvironment("SEND_TOKEN", 0);
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("ADDRESS");
        clarinUserMetadata2.setMetadataValue("Test2");

        ClarinUserMetadataRest clarinUserMetadata3 = new ClarinUserMetadataRest();
        clarinUserMetadata3.setMetadataKey("SEND_TOKEN");

        ClarinUserMetadataRest clarinUserMetadata4 = new ClarinUserMetadataRest();
        clarinUserMetadata4.setMetadataKey("EXTRA_EMAIL");
        clarinUserMetadata4.setMetadataValue("test@test.edu");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);
        clarinUserMetadataRestList.add(clarinUserMetadata3);
        clarinUserMetadataRestList.add(clarinUserMetadata4);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    @Test
    public void authorizedUserWithMetadata_shouldSendToken() throws Exception {
        this.prepareEnvironment("NAME,ADDRESS", 0);
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        ClarinUserMetadataBuilder.createClarinUserMetadata(context)
                .withUserRegistration(clarinUserRegistration)
                .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("ADDRESS");
        clarinUserMetadata2.setMetadataValue("Test2");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    @Test
    public void authorizedUserWithMetadata_shouldSendEmail() throws Exception {
        this.prepareEnvironment("SEND_TOKEN,NAME,ADDRESS", 0);
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        ClarinUserMetadataBuilder.createClarinUserMetadata(context)
                .withUserRegistration(clarinUserRegistration)
                .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("ADDRESS");
        clarinUserMetadata2.setMetadataValue("Test2");

        ClarinUserMetadataRest clarinUserMetadata3 = new ClarinUserMetadataRest();
        clarinUserMetadata3.setMetadataKey("SEND_TOKEN");

        ClarinUserMetadataRest clarinUserMetadata4 = new ClarinUserMetadataRest();
        clarinUserMetadata4.setMetadataKey("EXTRA_EMAIL");
        clarinUserMetadata4.setMetadataValue("test@test.edu");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);
        clarinUserMetadataRestList.add(clarinUserMetadata3);
        clarinUserMetadataRestList.add(clarinUserMetadata4);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    // Confirmation = 1
    @Test
    public void authorizedUserWithoutMetadata_shouldDownloadToken() throws Exception {
        this.prepareEnvironment(null, 1);
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(new ArrayList<ClarinUserMetadataRest>(0)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void shouldNotCreateDuplicateUserMetadataBasedOnHistory() throws Exception {
        // Prepare environment with Clarin License, resource mapping, allowance, user registration and user metadata
        // then try to download the same bitstream again and the user metadata should not be created based on history
        this.prepareEnvironment("NAME,ADDRESS", 0);
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        ClarinUserMetadataRest clarinUserMetadata2 = new ClarinUserMetadataRest();
        clarinUserMetadata2.setMetadataKey("ADDRESS");
        clarinUserMetadata2.setMetadataValue("Test2");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);
        clarinUserMetadataRestList.add(clarinUserMetadata2);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));


        // Get created User Metadata - there should be 2 records
        getClient(adminToken).perform(get("/api/core/clarinusermetadata")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        // Second download

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream2.getID())
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created two CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        // Get created User Metadata - there should be 4 records
        getClient(adminToken).perform(get("/api/core/clarinusermetadata")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(4)));

        // The User Metadata should not have updated transaction ID after a new download - test for fixed issue
        List<ClarinUserMetadata> allUserMetadata = clarinUserMetadataService.findAll(context);
        ClarinLicenseResourceUserAllowance clrua1 = allUserMetadata.get(0).getTransaction();
        ClarinLicenseResourceUserAllowance clrua2 = allUserMetadata.get(3).getTransaction();
        assertThat(clrua1.getID(), not(clrua2.getID()));

        // Check that the user registration for test data full user has been created
        // Test /api/core/clarinusermetadatas search by userRegistrationAndBitstream endpoint
        getClient(adminToken).perform(get("/api/core/clarinusermetadata/search/byUserRegistrationAndBitstream")
                .param("userRegUUID", String.valueOf(clarinUserRegistration.getID()))
                .param("bitstreamUUID", String.valueOf(bitstream2.getID()))
                .contentType(contentType))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(2)));

        // Download again the second bitstream and the user metadata should be returned only from the last transaction

        // Create a new User Metadata
        ClarinUserMetadataRest clarinUserMetadata3 = new ClarinUserMetadataRest();
        clarinUserMetadata3.setMetadataKey("NAME");
        clarinUserMetadata3.setMetadataValue("New Test");

        ClarinUserMetadataRest clarinUserMetadata4 = new ClarinUserMetadataRest();
        clarinUserMetadata4.setMetadataKey("ADDRESS");
        clarinUserMetadata4.setMetadataValue("New Test");

        List<ClarinUserMetadataRest> newUserMetadataRestList = new ArrayList<>();
        newUserMetadataRestList.add(clarinUserMetadata3);
        newUserMetadataRestList.add(clarinUserMetadata4);

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/core/clarinusermetadata/manage?bitstreamUUID=" + bitstream2.getID())
                        .content(mapper.writeValueAsBytes(newUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", not(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created two CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(3)));

        // Get created User Metadata from the new transaction - there should be 2 records
        getClient(adminToken).perform(get("/api/core/clarinusermetadata/search/byUserRegistrationAndBitstream")
                        .param("userRegUUID", String.valueOf(clarinUserRegistration.getID()))
                        .param("bitstreamUUID", String.valueOf(bitstream2.getID()))
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        // Delete all created user metadata - clean test environment
        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    private WorkspaceItem createWorkspaceItemWithFile(boolean secondBitstream) {
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .build();

        context.setCurrentUser(eperson);
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .build();

        if (secondBitstream) {
            this.bitstream2 = witem.getItem().getBundles().get(0).getBitstreams().get(0);
        } else {
            this.bitstream = witem.getItem().getBundles().get(0).getBitstreams().get(0);
        }

        return witem;
    }

    /**
     * Create Clarin License Label object for testing purposes.
     */
    private ClarinLicenseLabel createClarinLicenseLabel(String label, boolean extended, String title)
            throws SQLException, AuthorizeException {
        ClarinLicenseLabel clarinLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        clarinLicenseLabel.setLabel(label);
        clarinLicenseLabel.setExtended(extended);
        clarinLicenseLabel.setTitle(title);

        clarinLicenseLabelService.update(context, clarinLicenseLabel);
        return clarinLicenseLabel;
    }


    /**
     * Create ClarinLicense object with ClarinLicenseLabel object for testing purposes.
     */
    private ClarinLicense createClarinLicense(String name, String definition, String requiredInfo, int confirmation)
            throws SQLException, AuthorizeException {
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        clarinLicense.setConfirmation(confirmation);
        clarinLicense.setDefinition(definition);
        clarinLicense.setRequiredInfo(requiredInfo);
        clarinLicense.setName(name);

        // Add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        ClarinLicenseLabel clarinLicenseLabel = createClarinLicenseLabel("lbl", false, "Test Title");
        clarinLicenseLabels.add(clarinLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);

        clarinLicenseService.update(context, clarinLicense);
        return clarinLicense;
    }
}
