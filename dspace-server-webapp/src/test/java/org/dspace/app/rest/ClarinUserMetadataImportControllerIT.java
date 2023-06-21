/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.ClarinLicenseRestRepository.OPERATION_PATH_LICENSE_RESOURCE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
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
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration test to test the /api/clarin/import/* endpoints
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinUserMetadataImportControllerIT extends AbstractEntityIntegrationTest {

    @Autowired
    ClarinLicenseService clarinLicenseService;
    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;
    @Autowired
    ClarinUserMetadataService clarinUserMetadataService;

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
    public void importUserMetadataWithEpersonTest() throws Exception {
        this.prepareEnvironment("NAME");
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/clarin/import/usermetadata")
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userRegistrationId", clarinUserRegistration.getID().toString())
                        .param("bitstreamUUID", bitstream.getID().toString())
                        .param("createdOn", "2012-09-19T10:30:03.741633")
                        .param("token", "111"))
                .andExpect(status().isOk());

        //find created data and control it
        ClarinUserMetadata clarinUserMetadata = clarinUserMetadataService.findAll(context).get(0);
        assertEquals(clarinUserMetadata.getMetadataKey(), "NAME");
        assertEquals(clarinUserMetadata.getMetadataValue(), "Test");
        assertEquals(clarinUserMetadata.getEperson().getPersonID(), admin.getID());
        assertEquals(clarinUserMetadata.getTransaction().getCreatedOn().getTime(),
                getDateFromString("2012-09-19T10:30:03.741633").getTime());
        assertEquals(clarinUserMetadata.getTransaction().getToken(), "111");

        //clean all
        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    @Test
    public void importUserMetadataWithoutEpersonTest() throws Exception {
        this.prepareEnvironment("NAME");
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        clarinUserRegistration.setPersonID(null);
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        ClarinUserMetadataRest clarinUserMetadata1 = new ClarinUserMetadataRest();
        clarinUserMetadata1.setMetadataKey("NAME");
        clarinUserMetadata1.setMetadataValue("Test");

        List<ClarinUserMetadataRest> clarinUserMetadataRestList = new ArrayList<>();
        clarinUserMetadataRestList.add(clarinUserMetadata1);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // There should exist record in the UserRegistration table
        getClient(adminToken).perform(get("/api/core/clarinuserregistrations")
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // Manage UserMetadata and get token
        getClient(adminToken).perform(post("/api/clarin/import/usermetadata")
                        .content(mapper.writeValueAsBytes(clarinUserMetadataRestList.toArray()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userRegistrationId", clarinUserRegistration.getID().toString())
                        .param("bitstreamUUID", bitstream.getID().toString())
                        .param("createdOn", "2012-09-19T10:30:03.741633")
                        .param("token", "111"))
                .andExpect(status().isOk());

        //find created data and control it
        ClarinUserMetadata clarinUserMetadata = clarinUserMetadataService.findAll(context).get(0);
        assertEquals(clarinUserMetadata.getMetadataKey(), "NAME");
        assertEquals(clarinUserMetadata.getMetadataValue(), "Test");
        assertEquals(clarinUserMetadata.getEperson().getPersonID(), null);
        assertEquals(clarinUserMetadata.getTransaction().getCreatedOn().getTime(),
                getDateFromString("2012-09-19T10:30:03.741633").getTime());
        assertEquals(clarinUserMetadata.getTransaction().getToken(), "111");

        //clean all
        ClarinUserMetadataBuilder.deleteClarinUserMetadata(clarinUserRegistration.getID());
    }

    /**
     * Create Workspace item with file.
     */
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

    /**
     * Convert String date to Date.
     */
    private Date getDateFromString(String value) throws java.text.ParseException {
        Date output = null;
        if (StringUtils.isBlank(value)) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        try {
            output = sdf.parse(value);
        } catch (java.text.ParseException e) {
            throw new RuntimeException("Cannot convert date: " + value + " from String to Date.");
        }
        return output;
    }
}
