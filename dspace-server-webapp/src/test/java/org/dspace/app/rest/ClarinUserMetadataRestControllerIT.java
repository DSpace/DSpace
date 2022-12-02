/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.DownloadTokenExpiredException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.ClarinUserMetadataBuilder;
import org.dspace.builder.ClarinUserRegistrationBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.dspace.app.rest.repository.ClarinUserMetadataRestController.CHECK_EMAIL_RESPONSE_CONTENT;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.dspace.app.rest.repository.ClarinLicenseRestRepository.OPERATION_PATH_LICENSE_RESOURCE;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClarinUserMetadataRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ClarinLicenseService clarinLicenseService;
    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;

    WorkspaceItem witem;
    ClarinLicense clarinLicense;
    Bitstream bitstream;

    // Attach ClarinLicense to the Bitstream
    private void prepareEnvironment(String requiredInfo) throws Exception {
        // 1. Create Workspace Item with uploaded file
        // 2. Create Clarin License
        // 3. Send request to add Clarin License to the Workspace Item
        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        // 5. Check if the Clarin License was attached to the Bitstream

        // 1. Create WI with uploaded file
        context.turnOffAuthorisationSystem();
        witem = createWorkspaceItemWithFile();

        List<Operation> replaceOperations = new ArrayList<Operation>();
        String clarinLicenseName = "Test Clarin License";

        // 2. Create clarin license with clarin license label
        clarinLicense = createClarinLicense(clarinLicenseName, "Test Def", requiredInfo, 0);

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

        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value", is(clarinLicenseName)));

        // 5. Check if the Clarin License was attached to the Bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(1)));
    }

    @Test
    public void notAuthorizedUser_shouldReturnToken() throws Exception {
        this.prepareEnvironment("NAME");
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
        this.prepareEnvironment("SEND_TOKEN");
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
                .andExpect(jsonPath("$", is(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void authorizedUserWithoutMetadata_shouldReturnToken() throws Exception {
        this.prepareEnvironment("NAME");
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
        this.prepareEnvironment("SEND_TOKEN");
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
        this.prepareEnvironment("NAME,ADDRESS");
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
        this.prepareEnvironment("SEND_TOKEN,NAME,ADDRESS");
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
                .andExpect(jsonPath("$", is(CHECK_EMAIL_RESPONSE_CONTENT)));

        // Get created CLRUA
        getClient(adminToken).perform(get("/api/core/clarinlruallowances")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    private WorkspaceItem createWorkspaceItemWithFile() {
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

        bitstream = witem.getItem().getBundles().get(0).getBitstreams().get(0);

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
