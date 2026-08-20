/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.patch.operation.BundleRemoveOperation.OPERATION_PATH_BUNDLE_REMOVE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.MediaType;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link org.dspace.app.rest.repository.patch.operation.BundleRemoveOperation}
 */
public class BundleRemoveOperationIT extends AbstractControllerIntegrationTest {

    @Test
    public void deleteBundlesInBulk() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle1 = null;
        Bundle bundle2 = null;
        Bundle bundle3 = null;
        Bundle bundle4 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle1 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM1")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle1, is)
                           .withName("Bitstream 1")
                           .withMimeType("text/plain")
                           .build();

            bundle2 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM2")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle2, is)
                           .withName("Bitstream 2")
                           .withMimeType("text/plain")
                           .build();

            bundle3 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM3")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle3, is)
                           .withName("Bitstream 3")
                           .withMimeType("text/plain")
                           .build();

            bundle4 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM4")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle4, is)
                           .withName("Bitstream 4")
                           .withMimeType("text/plain")
                           .build();
        }

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp1 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle1.getID());
        ops.add(removeOp1);
        RemoveOperation removeOp2 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle2.getID());
        ops.add(removeOp2);
        RemoveOperation removeOp3 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle3.getID());
        ops.add(removeOp3);
        String patchBody = getPatchContent(ops);
        String token = getAuthToken(admin.getEmail(), password);

        Assert.assertTrue(bundleExists(token, bundle1, bundle2, bundle3, bundle4));

        getClient(token).perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isNoContent());

        // Verify that only the three bundles were deleted and the fourth one still exists
        Assert.assertTrue(bundleNotFound(token, bundle1, bundle2, bundle3));
        Assert.assertTrue(bundleExists(token, bundle4));
    }

    @Test
    public void deleteBundlesInBulk_invalidUUID() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle1 = null;
        Bundle bundle2 = null;
        Bundle bundle3 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle1 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM1")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle1, is)
                           .withName("Bitstream 1")
                           .withMimeType("text/plain")
                           .build();

            bundle2 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM2")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle2, is)
                           .withName("Bitstream 2")
                           .withMimeType("text/plain")
                           .build();

            bundle3 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM3")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle3, is)
                           .withName("Bitstream 3")
                           .withMimeType("text/plain")
                           .build();
        }

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp1 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle1.getID());
        ops.add(removeOp1);
        RemoveOperation removeOp2 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle2.getID());
        ops.add(removeOp2);
        UUID randomUUID = UUID.randomUUID();
        RemoveOperation removeOp3 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + randomUUID);
        ops.add(removeOp3);
        String patchBody = getPatchContent(ops);
        String token = getAuthToken(admin.getEmail(), password);

        Assert.assertTrue(bundleExists(token, bundle1, bundle2, bundle3));

        getClient(token).perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnprocessableEntity());

        // Verify that no bundles were deleted since the request was invalid
        Assert.assertTrue(bundleExists(token, bundle1, bundle2, bundle3));
    }

    @Test
    public void deleteBundlesInBulk_invalidRequestSize() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle1 = null;
        Bundle bundle2 = null;
        Bundle bundle3 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle1 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM1")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle1, is)
                           .withName("Bitstream 1")
                           .withMimeType("text/plain")
                           .build();

            bundle2 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM2")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle2, is)
                           .withName("Bitstream 2")
                           .withMimeType("text/plain")
                           .build();

            bundle3 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM3")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle3, is)
                           .withName("Bitstream 3")
                           .withMimeType("text/plain")
                           .build();
        }

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp1 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle1.getID());
        ops.add(removeOp1);
        RemoveOperation removeOp2 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle2.getID());
        ops.add(removeOp2);
        RemoveOperation removeOp3 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle3.getID());
        ops.add(removeOp3);
        String patchBody = getPatchContent(ops);
        String token = getAuthToken(admin.getEmail(), password);

        Assert.assertTrue(bundleExists(token, bundle1, bundle2, bundle3));
        DSpaceServicesFactory.getInstance().getConfigurationService().setProperty("rest.patch.operations.limit", 2);

        getClient(token).perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // Verify that no bundles were deleted since the request was invalid
        Assert.assertTrue(bundleExists(token, bundle1, bundle2, bundle3));
    }

    @Test
    public void deleteBundlesInBulk_Unauthorized() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle1 = null;
        Bundle bundle2 = null;
        Bundle bundle3 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle1 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM1")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle1, is)
                           .withName("Bitstream 1")
                           .withMimeType("text/plain")
                           .build();

            bundle2 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM2")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle2, is)
                           .withName("Bitstream 2")
                           .withMimeType("text/plain")
                           .build();

            bundle3 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM3")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle3, is)
                           .withName("Bitstream 3")
                           .withMimeType("text/plain")
                           .build();
        }

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp1 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle1.getID());
        ops.add(removeOp1);
        RemoveOperation removeOp2 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle2.getID());
        ops.add(removeOp2);
        RemoveOperation removeOp3 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle3.getID());
        ops.add(removeOp3);
        String patchBody = getPatchContent(ops);

        Assert.assertTrue(bundleExists(getAuthToken(admin.getEmail(), password), bundle1, bundle2, bundle3));

        getClient().perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteBundlesInBulk_Forbidden() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle1 = null;
        Bundle bundle2 = null;
        Bundle bundle3 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle1 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM1")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle1, is)
                           .withName("Bitstream 1")
                           .withMimeType("text/plain")
                           .build();

            bundle2 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM2")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle2, is)
                           .withName("Bitstream 2")
                           .withMimeType("text/plain")
                           .build();

            bundle3 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM3")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle3, is)
                           .withName("Bitstream 3")
                           .withMimeType("text/plain")
                           .build();
        }

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp1 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle1.getID());
        ops.add(removeOp1);
        RemoveOperation removeOp2 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle2.getID());
        ops.add(removeOp2);
        RemoveOperation removeOp3 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle3.getID());
        ops.add(removeOp3);
        String patchBody = getPatchContent(ops);
        String token = getAuthToken(eperson.getEmail(), password);

        Assert.assertTrue(bundleExists(getAuthToken(admin.getEmail(), password), bundle1, bundle2, bundle3));

        getClient(token).perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // Verify that no bundles were deleted
        Assert.assertTrue(bundleExists(getAuthToken(admin.getEmail(), password), bundle1, bundle2, bundle3));
    }

    @Test
    public void deleteSingleBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle1 = null;
        Bundle bundle2 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle1 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM1")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle1, is)
                           .withName("Bitstream 1")
                           .withMimeType("text/plain")
                           .build();

            bundle2 = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM2")
                                  .build();
            BitstreamBuilder.createBitstream(context, bundle2, is)
                           .withName("Bitstream 2")
                           .withMimeType("text/plain")
                           .build();
        }

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp1 = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundle1.getID());
        ops.add(removeOp1);
        String patchBody = getPatchContent(ops);
        String token = getAuthToken(admin.getEmail(), password);

        Assert.assertTrue(bundleExists(token, bundle1, bundle2));

        getClient(token).perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isNoContent());

        // Verify that only bundle1 was deleted and bundle2 still exists
        Assert.assertTrue(bundleNotFound(token, bundle1));
        Assert.assertTrue(bundleExists(token, bundle2));
    }

    @Test
    public void deleteBundleWithMultipleBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item")
                               .build();

        String bitstreamContent = "This is an archived bitstream";
        Bundle bundle = null;
        Bitstream bitstream1 = null;
        Bitstream bitstream2 = null;
        Bitstream bitstream3 = null;

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bundle = BundleBuilder.createBundle(context, item)
                                  .withName("CUSTOM")
                                  .build();
            bitstream1 = BitstreamBuilder.createBitstream(context, bundle, is)
                                        .withName("Bitstream 1")
                                        .withMimeType("text/plain")
                                        .build();
            bitstream2 = BitstreamBuilder.createBitstream(context, bundle, is)
                                        .withName("Bitstream 2")
                                        .withMimeType("text/plain")
                                        .build();
            bitstream3 = BitstreamBuilder.createBitstream(context, bundle, is)
                                        .withName("Bitstream 3")
                                        .withMimeType("text/plain")
                                        .build();
        }

        UUID bundleId = bundle.getID();
        UUID bitstream1Id = bitstream1.getID();
        UUID bitstream2Id = bitstream2.getID();
        UUID bitstream3Id = bitstream3.getID();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<>();
        RemoveOperation removeOp = new RemoveOperation(OPERATION_PATH_BUNDLE_REMOVE + bundleId);
        ops.add(removeOp);
        String patchBody = getPatchContent(ops);
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/bundles")
                                     .content(patchBody)
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isNoContent());

        // Verify bundle is deleted
        getClient(token).perform(get("/api/core/bundles/" + bundleId))
                        .andExpect(status().isNotFound());

        // Verify all bitstreams are also deleted
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream1Id))
                        .andExpect(status().isNotFound());
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream2Id))
                        .andExpect(status().isNotFound());
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream3Id))
                        .andExpect(status().isNotFound());
    }

    /**
     * Helper method to check if bundles exist
     */
    private boolean bundleExists(String token, Bundle... bundles) throws Exception {
        for (Bundle bundle : bundles) {
            MvcResult result = getClient(token).perform(get("/api/core/bundles/" + bundle.getID()))
                                               .andReturn();
            if (result.getResponse().getStatus() != 200) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method to check if bundles do not exist
     */
    private boolean bundleNotFound(String token, Bundle... bundles) throws Exception {
        for (Bundle bundle : bundles) {
            MvcResult result = getClient(token).perform(get("/api/core/bundles/" + bundle.getID()))
                                               .andReturn();
            if (result.getResponse().getStatus() != 404) {
                return false;
            }
        }
        return true;
    }
}
