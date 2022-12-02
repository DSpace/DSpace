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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.services.ConfigurationService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Added CLARIN/DSpace modifications for WorkspaceItemRestRepositoryIT class.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinWorkspaceItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    public static final String REST_SERVER_URL = "http://localhost/api/";
    public static final String EU_SPONSOR = "EU;Test funding code;Test funding organization;Test funding name;" +
            "info:eu-repo/grantAgreement/Test funding code";
    private static final String NON_EU_SPONSOR = "No EU;Test funding code;Test funding organization;Test funding name";
    private static final String EU_SPONSOR_RELATION = "info:eu-repo/grantAgreement/Test funding code";

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ClarinLicenseService clarinLicenseService;
    @Autowired
    private ClarinLicenseLabelService clarinLicenseLabelService;
    @Autowired
    private ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    @Test
    public void uploadFileBiggerThanUploadFileSizeLimit() throws Exception {
        // Create a big file in the /temp folder
        String TMP_DIR = System.getProperty("java.io.tmpdir");
        String FILE_NAME = "test.txt";
        Long SIZE_IN_BYTES = 2000000000L;
        File file = null;

        try {
            file = new File(TMP_DIR, FILE_NAME);

            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(SIZE_IN_BYTES);
            raf.close();

            context.turnOffAuthorisationSystem();

            // A community-collection structure with one parent community and one collection
            parentCommunity = CommunityBuilder.createCommunity(context)
                    .withName("Parent Community")
                    .build();
            Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                    .withName("Collection").build();

            WorkspaceItem wItem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                    .withTitle("Test WorkspaceItem")
                    .withIssueDate("2017-10-17")
                    .withMetadata("local", "bitstream", "redirectToURL", file.getAbsolutePath())
                    .build();

            context.restoreAuthSystemState();
            String tokenAdmin = getAuthToken(admin.getEmail(), password);

            List<Operation> addAccessCondition = new ArrayList<Operation>();
            List<Map<String, String>> accessConditions = new ArrayList<Map<String, String>>();

            Map<String, String> accessCondition1 = new HashMap<String, String>();
            accessCondition1.put("name", "administrator");
            accessConditions.add(accessCondition1);

            addAccessCondition.add(new AddOperation("/sections/defaultAC/accessConditions",
                    accessConditions));

            String patchBody = getPatchContent(addAccessCondition);
            // add access conditions
            getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + wItem.getID())
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sections.upload.files[0]"
                                    + ".metadata['dc.title'][0].value",
                            is(FILE_NAME)))
                    .andExpect(jsonPath("$.sections.upload.files[0].sizeBytes",
                            is(SIZE_IN_BYTES.intValue())));
        } catch (Exception e) {
            throw new Exception("Cannot upload file bigger than upload file size limit, " + e.getMessage());
        } finally {
            // the big file should be deleted
            File checkFile = null;

            String shouldDeleteFile = configurationService.getProperty("delete.big.file.after.upload");
            if (!Objects.isNull(file) && StringUtils.equals("true", shouldDeleteFile)) {
                checkFile = new File(file.getAbsolutePath());
                Assert.assertFalse(checkFile.exists());
            }

            try {
                // if is not deleted, delete that test big file
                if (!Objects.isNull(checkFile) && checkFile.exists()) {
                    FileUtils.forceDelete(file);
                }
            } catch (Exception e) {
                throw new Exception("Cannot delete the file in the end of the test: " + e.getMessage());
            }
        }
    }

    @Test
    public void patchAddEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();

        List<Operation> addTitle = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> titleValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", EU_SPONSOR);
        titleValues.add(value);
        addTitle.add(new AddOperation("/sections/traditionalpageone/local.sponsor", titleValues));

        String patchBody = getPatchContent(addTitle);

        // Verify submitter cannot modify metadata via item PATCH. They must use submission forms.
        String tokenEperson = getAuthToken(admin.getEmail(), password);
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        EU_SPONSOR, EU_SPONSOR_RELATION)));
    }

    @Test
    public void patchAddNonEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();

        List<Operation> addTitle = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> titleValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", NON_EU_SPONSOR);
        titleValues.add(value);
        addTitle.add(new AddOperation("/sections/traditionalpageone/local.sponsor", titleValues));

        String patchBody = getPatchContent(addTitle);

        // Verify submitter cannot modify metadata via item PATCH. They must use submission forms.
        String tokenEperson = getAuthToken(admin.getEmail(), password);
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        NON_EU_SPONSOR, null)));
    }

    @Test
    public void patchAddMoreNonEUWithEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();
        String secondNonEUSponsor = "2" + NON_EU_SPONSOR;
        String secondEUSponsor = "2" + EU_SPONSOR;

        List<Operation> addSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        Map<String, String> secondSponsor = new HashMap<String, String>();
        secondSponsor.put("value", NON_EU_SPONSOR);

        Map<String, String> thirdSponsor = new HashMap<String, String>();
        thirdSponsor.put("value", secondNonEUSponsor);

        Map<String, String> fourthSponsor = new HashMap<String, String>();
        fourthSponsor.put("value", secondEUSponsor);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor", firstSponsorValues));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/1", secondSponsor));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/2", thirdSponsor));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/3", fourthSponsor));

        String patchBody = getPatchContent(addSponsorOperations);

        // Verify submitter cannot modify metadata via item PATCH. They must use submission forms.
        String tokenEperson = getAuthToken(admin.getEmail(), password);
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][0].value",
                        is(EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][1].value",
                        is(NON_EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][2].value",
                        is(secondNonEUSponsor)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][3].value",
                        is(secondEUSponsor)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value",
                        is(EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value",
                        is(EU_SPONSOR_RELATION)));
    }

    @Test
    public void patchAddMoreEUWithNonEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();
        String secondNonEUSponsor = "2" + NON_EU_SPONSOR;
        String secondEUSponsor = "2" + EU_SPONSOR;

        List<Operation> addSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", NON_EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        Map<String, String> secondSponsor = new HashMap<String, String>();
        secondSponsor.put("value", EU_SPONSOR);

        Map<String, String> thirdSponsor = new HashMap<String, String>();
        thirdSponsor.put("value", secondEUSponsor);

        Map<String, String> fourthSponsor = new HashMap<String, String>();
        fourthSponsor.put("value", secondNonEUSponsor);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor", firstSponsorValues));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/1", secondSponsor));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/2", thirdSponsor));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/3", fourthSponsor));

        String patchBody = getPatchContent(addSponsorOperations);

        // Verify submitter cannot modify metadata via item PATCH. They must use submission forms.
        String tokenEperson = getAuthToken(admin.getEmail(), password);
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][0].value",
                        is(NON_EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][1].value",
                        is(EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][2].value",
                        is(secondEUSponsor)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][3].value",
                        is(secondNonEUSponsor)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value",
                        is(EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value",
                        is(EU_SPONSOR_RELATION)));
    }

    // replace nonEU - should do not have relation
    @Test
    public void patchReplaceNonEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();
        String updatedSponsor = "updated" + NON_EU_SPONSOR;

        List<Operation> addSponsorOperations = new ArrayList<Operation>();
        List<Operation> replaceSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", NON_EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        // creating replace operation
        Map<String, String> updatedSponsorValue = new HashMap<String, String>();
        updatedSponsorValue.put("value", updatedSponsor);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor",
                firstSponsorValues));
        replaceSponsorOperations.add(new ReplaceOperation("/sections/traditionalpageone/local.sponsor/0",
                updatedSponsorValue));

        String patchBody = getPatchContent(addSponsorOperations);
        String updateBody = getPatchContent(replaceSponsorOperations);

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        // Add operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        NON_EU_SPONSOR, null)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value").doesNotExist());

        // Replace operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        updatedSponsor, null)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value").doesNotExist());
    }

    // replace EU - should do not have relation
    @Test
    public void patchReplaceEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();
        String updatedSponsor = "updated" + EU_SPONSOR;

        List<Operation> addSponsorOperations = new ArrayList<Operation>();
        List<Operation> replaceSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        // creating replace operation
        Map<String, String> updatedSponsorValue = new HashMap<String, String>();
        updatedSponsorValue.put("value", updatedSponsor);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor",
                firstSponsorValues));
        replaceSponsorOperations.add(new ReplaceOperation("/sections/traditionalpageone/local.sponsor/0",
                updatedSponsorValue));

        String patchBody = getPatchContent(addSponsorOperations);
        String updateBody = getPatchContent(replaceSponsorOperations);

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        // Add operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        EU_SPONSOR, EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());

        // Replace operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        updatedSponsor, EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());
    }

    // replace non EU to EU - should have relation
    @Test
    public void patchReplaceFromNonEUToEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();

        List<Operation> addSponsorOperations = new ArrayList<Operation>();
        List<Operation> replaceSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", NON_EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        // creating replace operation
        Map<String, String> updatedSponsorValue = new HashMap<String, String>();
        updatedSponsorValue.put("value", EU_SPONSOR);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor",
                firstSponsorValues));
        replaceSponsorOperations.add(new ReplaceOperation("/sections/traditionalpageone/local.sponsor/0",
                updatedSponsorValue));

        String patchBody = getPatchContent(addSponsorOperations);
        String updateBody = getPatchContent(replaceSponsorOperations);

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        // Add operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        NON_EU_SPONSOR, null)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value").doesNotExist());

        // Replace operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        EU_SPONSOR, EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());
    }

    // replace EU to non EU - should do not have relation
    @Test
    public void patchReplaceFromRUToNonEUFundingMetadataOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();

        List<Operation> addSponsorOperations = new ArrayList<Operation>();
        List<Operation> replaceSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        // creating replace operation
        Map<String, String> updatedSponsorValue = new HashMap<String, String>();
        updatedSponsorValue.put("value", NON_EU_SPONSOR);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor", firstSponsorValues));
        replaceSponsorOperations.add(new ReplaceOperation("/sections/traditionalpageone/local.sponsor/0",
                updatedSponsorValue));

        String patchBody = getPatchContent(addSponsorOperations);
        String updateBody = getPatchContent(replaceSponsorOperations);

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        // Add operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        EU_SPONSOR, EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());

        // Replace operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",  WorkspaceItemMatcher.matchItemWithSponsorRelation(witem,
                        NON_EU_SPONSOR, null)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());
    }

    // add EU, add nonEU, changee EU to nonEU, change nonEU to EU
    @Test
    public void patchAddMoreEUAndNonEUAndReplaceEachOtherOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();
        String secondEuSponsor = "2" + EU_SPONSOR;

        List<Operation> addSponsorOperations = new ArrayList<Operation>();
        List<Operation> replaceSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        Map<String, String> secondSponsor = new HashMap<String, String>();
        secondSponsor.put("value", NON_EU_SPONSOR);

        // creating replace operation
        Map<String, String> updatedSponsorValue = new HashMap<String, String>();
        updatedSponsorValue.put("value", secondEuSponsor);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor", firstSponsorValues));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/1", secondSponsor));
        replaceSponsorOperations.add(new ReplaceOperation("/sections/traditionalpageone/local.sponsor/1",
                updatedSponsorValue));

        String patchBody = getPatchContent(addSponsorOperations);
        String updateBody = getPatchContent(replaceSponsorOperations);

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        // Add operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][0].value",
                        is(EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][1].value",
                        is(NON_EU_SPONSOR)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value",
                        is(EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());

        // Replace operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][0].value",
                        is(EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][1].value",
                        is(secondEuSponsor)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value",
                        is(EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value",
                        is(EU_SPONSOR_RELATION)));
    }

    // add EU, add nonEU, changee EU to nonEU, change nonEU to EU
    @Test
    public void patchAddMoreNonEUAndEUAndReplaceEachOtherOnItemStillInSubmissionTest() throws Exception {
        WorkspaceItem witem = this.createSimpleWorkspaceItem();
        String secondNonEuSponsor = "2" + NON_EU_SPONSOR;

        List<Operation> addSponsorOperations = new ArrayList<Operation>();
        List<Operation> replaceSponsorOperations = new ArrayList<Operation>();

        // create a list of values to use in add operation
        List<Map<String, String>> firstSponsorValues = new ArrayList<Map<String, String>>();
        Map<String, String> firstSponsor = new HashMap<String, String>();
        firstSponsor.put("value", NON_EU_SPONSOR);
        firstSponsorValues.add(firstSponsor);

        Map<String, String> secondSponsor = new HashMap<String, String>();
        secondSponsor.put("value", EU_SPONSOR);

        // creating replace operation
        Map<String, String> updatedSponsorValue = new HashMap<String, String>();
        updatedSponsorValue.put("value", secondNonEuSponsor);

        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor", firstSponsorValues));
        addSponsorOperations.add(new AddOperation("/sections/traditionalpageone/local.sponsor/1", secondSponsor));
        replaceSponsorOperations.add(new ReplaceOperation("/sections/traditionalpageone/local.sponsor/1",
                updatedSponsorValue));

        String patchBody = getPatchContent(addSponsorOperations);
        String updateBody = getPatchContent(replaceSponsorOperations);

        String tokenEperson = getAuthToken(admin.getEmail(), password);
        // Add operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][0].value",
                        is(NON_EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][1].value",
                        is(EU_SPONSOR)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value",
                        is(EU_SPONSOR_RELATION)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());

        // Replace operation
        getClient(tokenEperson).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][0].value",
                        is(NON_EU_SPONSOR)))
                .andExpect(jsonPath("$.sections.traditionalpageone['local.sponsor'][1].value",
                        is(secondNonEuSponsor)))
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][0].value").doesNotExist())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.relation'][1].value").doesNotExist());
    }

    /**
     * Add Clarin License to the WorkspaceItem. The Clarin License name should be added to the
     * item's metadata `dc.rights` and the Clarin License should be attached to the actual bitstream.
     */
    @Test
    public void addClarinLicenseToWI() throws Exception {
        // 1. Create Workspace Item with uploaded file
        // 2. Create Clarin License
        // 3. Send request to add Clarin License to the Workspace Item
        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        // 5. Check if the Clarin License was attached to the Bitstream

        // 1. Create WI with uploaded file
        context.turnOffAuthorisationSystem();
        WorkspaceItem witem = createWorkspaceItemWithFile();

        List<Operation> replaceOperations = new ArrayList<Operation>();
        String clarinLicenseName = "Test Clarin License";

        // 2. Create clarin license with clarin license label
        ClarinLicense clarinLicense = createClarinLicense(clarinLicenseName, "Test Def", "Test R Info", 0);

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
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
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

    /**
     * The Clarin License should be detached from the bitstream and the Clarin License name should be removed from the
     * item's metadata `dc.rights` on removing Clarin License from the Workspace Item.
     */
    @Test
    public void removeClarinLicenseFromWI() throws Exception {
        // 1. Create Workspace Item with uploaded file
        // 2. Create Clarin License
        // 3. Send request to add Clarin License to the Workspace Item
        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        // 5. Check if the Clarin License was attached to the Bitstream
        // 6. Send request to remove Clarin License from the Workspace Item (the clarin license name is empty string)
        // 7. Check if the Clarin License name was cleared from the Item's `dc.rights` metadata
        // 8. Check if the Clarin License was detached from the Workspace Item bitstream

        context.turnOffAuthorisationSystem();
        // 1. Create Workspace Item with uploaded file
        WorkspaceItem witem = this.createWorkspaceItemWithFile();
        List<Operation> replaceOperations = new ArrayList<Operation>();
        // 2. Create Clarin License
        String clarinLicenseName = "Test Clarin License";
        ClarinLicense clarinLicense = createClarinLicense(clarinLicenseName, "Test Def", "Test R Info", 0);
        context.restoreAuthSystemState();

        // Creating replace operation
        Map<String, String> licenseReplaceOpValue = new HashMap<String, String>();
        licenseReplaceOpValue.put("value", clarinLicenseName);
        replaceOperations.add(new ReplaceOperation("/" + OPERATION_PATH_LICENSE_RESOURCE,
                licenseReplaceOpValue));

        String updateBody = getPatchContent(replaceOperations);

        // 3. Send request to add Clarin License to the Workspace Item
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value", is(clarinLicenseName)));

        // 5. Check if the Clarin License was attached to the Bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(1)));


        // Creating of replace operation with empty clarin license name
        Map<String, String> licenseReplaceEmptyOpValue = new HashMap<String, String>();
        licenseReplaceEmptyOpValue.put("value", "");
        replaceOperations.clear();
        replaceOperations.add(new ReplaceOperation("/" + OPERATION_PATH_LICENSE_RESOURCE,
                licenseReplaceEmptyOpValue));

        String updateToEmptyBody = getPatchContent(replaceOperations);
        // 6. Send request to remove Clarin License from the Workspace Item (the clarin license name is empty string)
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateToEmptyBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // 7. Check if the Item has cleared `dc.rights` metadata
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value").doesNotExist());

        // 8. Check if the Clarin License was detached from the Workspace Item bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(0)));
    }

    /**
     * Change the Item's `dc.rights` metadata and change the attached Workspace Item bitstream when the
     * Clarin License is changed in the Workspace Item.
     */
    @Test
    public void updateClarinLicenseInWI() throws Exception {
        // 1. Create Workspace Item with uploaded file
        // 2. Create Clarin Licenses
        // 3. Send request to add Clarin License to the Workspace Item
        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        // 5. Check if the Clarin License was attached to the Bitstream
        // 6. Send request to change Clarin License in the Workspace Item
        // 7. Check if the Clarin License name was updated in the Item's `dc.rights` metadata
        // 8. Check if the first Clarin License was detached from the Workspace Item bitstream
        // 9. Check if the updated Clarin License was attached to the Workspace Item bitstream

        context.turnOffAuthorisationSystem();
        // 1. Create Workspace Item with uploaded file
        WorkspaceItem witem = this.createWorkspaceItemWithFile();
        List<Operation> replaceOperations = new ArrayList<Operation>();
        String clarinLicenseName = "Test Clarin License";
        String updateClarinLicenseName = "Updated Clarin License";

        // 2. Create Clarin Licenses
        ClarinLicense clarinLicense = createClarinLicense(clarinLicenseName, "Test Def", "Test R Info", 0);
        ClarinLicense updatedClarinLicense =
                createClarinLicense(updateClarinLicenseName, "Test Def2", "Test R Info2", 0);
        context.restoreAuthSystemState();

        // Creating replace operation
        Map<String, String> licenseReplaceOpValue = new HashMap<String, String>();
        licenseReplaceOpValue.put("value", clarinLicenseName);
        replaceOperations.add(new ReplaceOperation("/" + OPERATION_PATH_LICENSE_RESOURCE,
                licenseReplaceOpValue));

        String updateBody = getPatchContent(replaceOperations);

        // 3. Send request to add Clarin License to the Workspace Item
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value", is(clarinLicenseName)));

        // 5. Check if the Clarin License was attached to the Bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(1)));

        // Creating replace operation with updated clarin license name
        Map<String, String> licenseReplaceNewOpValue = new HashMap<String, String>();
        licenseReplaceNewOpValue.put("value", updateClarinLicenseName);
        replaceOperations.clear();
        replaceOperations.add(new ReplaceOperation("/" + OPERATION_PATH_LICENSE_RESOURCE,
                licenseReplaceNewOpValue));

        String updateToNewBody = getPatchContent(replaceOperations);
        // 6. Send request to change Clarin License in the Workspace Item
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateToNewBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // 7. Check if the Clarin License name was updated in the Item's `dc.rights` metadata
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value",
                        is(updateClarinLicenseName)));

        // 8. Check if the Clarin License was detached from the Workspace Item bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(0)));

        // 9. Check if the updated Clarin License was attached to the Workspace Item bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + updatedClarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(1)));
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

        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        ClarinLicenseLabel clarinLicenseLabel = createClarinLicenseLabel("lbl", false, "Test Title");
        clarinLicenseLabels.add(clarinLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);

        clarinLicenseService.update(context, clarinLicense);
        return clarinLicense;
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

        return witem;
    }

    private WorkspaceItem createSimpleWorkspaceItem() {
        context.turnOffAuthorisationSystem();

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
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        return witem;
    }
}
