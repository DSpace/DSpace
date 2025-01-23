/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.ClarinLicenseResourceMappingBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchServiceException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class ProvenanceServiceIT extends AbstractControllerIntegrationTest {
    @Autowired
    private ItemService itemService;
    @Autowired
    private ClarinLicenseLabelService clarinLicenseLabelService;
    @Autowired
    private ClarinLicenseService clarinLicenseService;

    private Collection  collection;
    private Item item;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        item = ItemBuilder.createItem(context, collection)
                .withTitle("Public item 1")
                .build();
        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        context.turnOffAuthorisationSystem();
        // Delete community created in init()
        try {
            ItemBuilder.deleteItem(item.getID());
            CollectionBuilder.deleteCollection(collection.getID());
            CommunityBuilder.deleteCommunity(parentCommunity.getID());
        } catch (Exception e) {
            // ignore
        }
        context.restoreAuthSystemState();

        item = null;
        collection = null;
        parentCommunity = null;
        super.destroy();
    }

    @Test
    public void updateLicenseTest() throws Exception {
        Bitstream bitstream = createBitstream(item, Constants.LICENSE_BUNDLE_NAME);
        ClarinLicense clarinLicense1 = createClarinLicense("Test 1", "Test Def");
        ClarinLicenseResourceMapping mapping = createResourceMapping(clarinLicense1, bitstream);
        ClarinLicense clarinLicense2 = createClarinLicense("Test 2", "Test Def");

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put("/api/core/items/" + item.getID() + "/bundles")
                        .param("licenseID", clarinLicense2.getID().toString()))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.UPDATE_LICENSE.getTemplate());

        deleteBitstream(bitstream);
        deleteClarinLicense(clarinLicense1);
        deleteClarinLicense(clarinLicense2);
        deleteResourceMapping(mapping.getID());
    }

    @Test
    public void addLicenseTest() throws Exception {
        ClarinLicense clarinLicense = createClarinLicense("Test", "Test Def");

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put("/api/core/items/" + item.getID() + "/bundles")
                        .param("licenseID", clarinLicense.getID().toString()))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.ADD_LICENSE.getTemplate());

        deleteClarinLicense(clarinLicense);
    }

    @Test
    public void removeLicenseTest() throws Exception {
        Bitstream bitstream = createBitstream(item, Constants.LICENSE_BUNDLE_NAME);
        ClarinLicense clarinLicense = createClarinLicense("Test", "Test Def");
        ClarinLicenseResourceMapping mapping = createResourceMapping(clarinLicense, bitstream);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put("/api/core/items/" + item.getID() + "/bundles")
                        .param("licenseID", "-1"))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.REMOVE_LICENSE.getTemplate());

        deleteBitstream(bitstream);
        deleteClarinLicense(clarinLicense);
        deleteResourceMapping(mapping.getID());
    }

    @Test
    public void makeDiscoverableTest() throws Exception {
        item.setDiscoverable(false);
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.DISCOVERABLE.getTemplate());
    }

    @Test
    public void makeNonDiscoverableTest() throws Exception {
        item.setDiscoverable(true);
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.NON_DISCOVERABLE.getTemplate());
    }

    @Test
    public void addedToMappedCollTest() throws Exception {
        Collection coll = createCollection();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
                post("/api/core/items/" + item.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + coll.getID() + "\n"
                        )
        );
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.MAPPED_COL.getTemplate());

        deleteCollection(coll.getID());
    }

    @Test
    public void addItemMetadataTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        AddOperation addOperation = new AddOperation("/metadata/dc.title", "Test");
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/items/" + item.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.ADD_ITEM_MTD.getTemplate());
    }

    @Test
    public void replaceItemMetadataTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        int index = 0;
        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/metadata/dc.title/" + index, "Test");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/items/" + item.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.REPLACE_ITEM_MTD.getTemplate());
    }

    @Test
    public void removeItemMetadataTest() throws Exception {
        int index = 0;
        String adminToken = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOperation = new RemoveOperation("/metadata/dc.title/" + index);
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/items/" + item.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.REMOVE_ITEM_MTD.getTemplate());
    }

    @Test
    public void removeBitstreamMetadataTest() throws Exception {
        Bitstream bitstream = createBitstream(item, "test");

        String adminToken = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        AddOperation addOperation = new AddOperation("/metadata/dc.description", "test");
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/bitstreams/" + bitstream.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()),
                ProvenanceExpectedMessages.REMOVE_BITSTREAM_MTD.getTemplate());

        deleteBitstream(bitstream);
    }

    @Test
    public void addBitstreamMetadataTest() throws Exception {
        Bitstream bitstream = createBitstream(item, "test");

        String adminToken = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        AddOperation addOperation = new AddOperation("/metadata/dc.description", "test");
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/bitstreams/" + bitstream.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()),
                ProvenanceExpectedMessages.REMOVE_BITSTREAM_MTD.getTemplate());
    }

    @Test
    public void updateMetadataBitstreamTest() throws Exception {
        Bitstream bitstream = createBitstream(item, "test");
        bitstream.setName(context, "test");

        String adminToken = getAuthToken(admin.getEmail(), password);
        int index = 0;
        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/metadata/dc.title/" + index + "/value", "test 1");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/bitstreams/" + bitstream.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()),
                ProvenanceExpectedMessages.REPLACE_BITSTREAM_MTD.getTemplate());

        deleteBitstream(bitstream);
    }

    @Test
    public void removeBitstreamFromItemTest() throws Exception {
        Bitstream bitstream = createBitstream(item, "test");

        String adminToken = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOperation = new RemoveOperation("/bitstreams/" + bitstream.getID());
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);
        getClient(adminToken).perform(patch("/api/core/bitstreams")
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON));
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.REMOVE_BITSTREAM.getTemplate());

        deleteBitstream(bitstream);
    }

    @Test
    public void addBitstreamToItemTest() throws Exception {
        Bundle bundle = createBundle(item, "test");

        String token = getAuthToken(admin.getEmail(), password);
        String input = "Hello, World!";
        context.turnOffAuthorisationSystem();
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt",
                org.springframework.http.MediaType.TEXT_PLAIN_VALUE,
                input.getBytes());
        context.restoreAuthSystemState();
        getClient(token)
                .perform(MockMvcRequestBuilders.multipart("/api/core/bundles/" + bundle.getID() + "/bitstreams")
                        .file(file))
                .andExpect(status().isCreated());
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.ADD_BITSTREAM.getTemplate());

        deleteBundle(bundle.getID());
    }

    @Test
    public void moveItemColTest() throws Exception {
        Collection col = createCollection();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
                .perform(put("/api/core/items/" + item.getID() + "/owningCollection/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col.getID()
                        ))
                .andExpect(status().isOk());
        objectCheck(itemService.find(context, item.getID()), ProvenanceExpectedMessages.MOVED_ITEM_COL.getTemplate());

        deleteCollection(col.getID());
    }


    private String provenanceMetadataModified(String metadata) {
        // Regex to match the date pattern
        String datePattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";
        Pattern pattern = Pattern.compile(datePattern);
        Matcher matcher = pattern.matcher(metadata);
        String rspModifiedProvenance = metadata;
        while (matcher.find()) {
            String dateString = matcher.group(0);
            rspModifiedProvenance = rspModifiedProvenance.replaceAll(dateString, "");
        }
        return rspModifiedProvenance;
    }

    private void objectCheck(DSpaceObject obj, String expectedMessage) throws Exception {
        List<MetadataValue> metadata = obj.getMetadata();
        boolean contain = false;
        for (MetadataValue value : metadata) {
            if (!Objects.equals(value.getMetadataField().toString(), "dc_description_provenance")) {
                continue;
            }
            if (provenanceMetadataModified(value.getValue()).contains(expectedMessage)) {
                contain = true;
                break;
            }
        }
        if (!contain) {
            Assert.fail("Metadata provenance do not contain expected data: " + expectedMessage);
        }
    }

    private Bundle createBundle(Item item, String bundleName) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Bundle bundle = BundleBuilder.createBundle(context, item).withName(bundleName).build();
        context.restoreAuthSystemState();
        return bundle;
    }

    private Bitstream createBitstream(Item item, String bundleName)
            throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        Bundle bundle = createBundle(item, Objects.isNull(bundleName) ? "test" : bundleName);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, bundle,
                toInputStream("Test Content", defaultCharset())).build();
        context.restoreAuthSystemState();
        return bitstream;
    }

    private void deleteBitstream(Bitstream bitstream) throws SQLException, IOException {
        int size = bitstream.getBundles().size();
        for (int i = 0; i < size; i++) {
            deleteBundle(bitstream.getBundles().get(i).getID());
        }
        BitstreamBuilder.deleteBitstream(bitstream.getID());
    }


    private void deleteBundle(UUID uuid) throws SQLException, IOException {
        BundleBuilder.deleteBundle(uuid);
    }

    private ClarinLicenseLabel createClarinLicenseLabel(String label, boolean extended, String title)
            throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        ClarinLicenseLabel clarinLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        clarinLicenseLabel.setLabel(label);
        clarinLicenseLabel.setExtended(extended);
        clarinLicenseLabel.setTitle(title);
        clarinLicenseLabelService.update(context, clarinLicenseLabel);
        context.restoreAuthSystemState();
        return clarinLicenseLabel;
    }

    private ClarinLicense createClarinLicense(String name, String definition)
            throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        clarinLicense.setDefinition(definition);
        clarinLicense.setName(name);
        HashSet<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        ClarinLicenseLabel clarinLicenseLabel = createClarinLicenseLabel("lbl", false, "Test Title");
        clarinLicenseLabels.add(clarinLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);
        clarinLicenseService.update(context, clarinLicense);
        context.restoreAuthSystemState();
        return clarinLicense;
    }

    private void deleteClarinLicenseLable(Integer id) throws Exception {
        ClarinLicenseLabelBuilder.deleteClarinLicenseLabel(id);
    }

    private void deleteClarinLicense(ClarinLicense license) throws Exception {
        int size = license.getLicenseLabels().size();
        for (int i = 0; i < size; i++) {
            deleteClarinLicenseLable(license.getLicenseLabels().get(i).getID());
        }
        ClarinLicenseBuilder.deleteClarinLicense(license.getID());
    }

    private Collection createCollection() {
        context.turnOffAuthorisationSystem();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        context.restoreAuthSystemState();
        return col;
    }

    private void deleteCollection(UUID uuid) throws SearchServiceException, SQLException, IOException {
        CollectionBuilder.deleteCollection(uuid);
    }

    private ClarinLicenseResourceMapping createResourceMapping(ClarinLicense license, Bitstream bitstream)
            throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        ClarinLicenseResourceMapping resourceMapping =
                ClarinLicenseResourceMappingBuilder.createClarinLicenseResourceMapping(context).build();
        context.restoreAuthSystemState();
        resourceMapping.setLicense(license);
        resourceMapping.setBitstream(bitstream);
        return resourceMapping;
    }

    private void deleteResourceMapping(Integer id) throws Exception {
        ClarinLicenseResourceMappingBuilder.delete(id);
    }
}
