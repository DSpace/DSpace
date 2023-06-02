/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.BundleMatcher;
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
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for the PrimaryBitstreamController
 */
public class PrimaryBitstreamControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    BundleService bundleService;
    @Autowired
    BitstreamService bitstreamService;

    Item item;
    Bitstream bitstream;
    Bundle bundle;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        item = ItemBuilder.createItem(context, collection).build();

        // create bitstream in ORIGINAL bundle of item
        String bitstreamContent = "TEST CONTENT";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
             bitstream = BitstreamBuilder.createBitstream(context, item, is)
                                         .withName("Bitstream")
                                         .withMimeType("text/plain")
                                         .build();
        }
        bundle = item.getBundles("ORIGINAL").get(0);
        context.restoreAuthSystemState();
    }

    @Test
    public void testGetPrimaryBitstream() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(getBundleUrl(bundle.getID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BitstreamMatcher.matchProperties(bitstream)));
    }

    @Test
    public void testGetPrimaryBitstreamBundleNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(getBundleUrl(UUID.randomUUID())))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPrimaryBitstreamNonExisting() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(getBundleUrl(bundle.getID())))
                        .andExpect(status().isNoContent())
                        .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    public void testPostPrimaryBitstream() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle.getName(), bundle.getID(),
                                                                           bundle.getHandle(), bundle.getType())));
        // verify primaryBitstream was actually added
        bundle = context.reloadEntity(bundle);
        Assert.assertEquals(bitstream, bundle.getPrimaryBitstream());
    }

    @Test
    public void testPostPrimaryBitstreamBundleNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundleUrl(UUID.randomUUID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isNotFound());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    @Test
    public void testPostPrimaryBitstreamInvalidBitstream() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(UUID.randomUUID())))
                        .andExpect(status().isUnprocessableEntity());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    @Test
    public void testPostPrimaryBitstreamAlreadyExists() throws Exception {
        context.turnOffAuthorisationSystem();
        bundle.setPrimaryBitstreamID(bitstream);
        Bitstream bitstream2 = createSecondBitstream(bundle);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isBadRequest());
        // verify primaryBitstream is still the original one
        bundle = context.reloadEntity(bundle);
        Assert.assertEquals(bitstream, bundle.getPrimaryBitstream());
    }

    @Test
    public void testPostPrimaryBitstreamNotInBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle2 = BundleBuilder.createBundle(context, item).withName("Bundle2").build();
        Bitstream bitstream2 = createSecondBitstream(bundle2);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isUnprocessableEntity());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    @Test
    public void testUpdatePrimaryBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        bundle.setPrimaryBitstreamID(bitstream);
        Bitstream bitstream2 = createSecondBitstream(bundle);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle.getName(), bundle.getID(),
                                                                               bundle.getHandle(), bundle.getType())));
        // verify primaryBitstream was actually updated
        bundle = context.reloadEntity(bundle);
        Assert.assertEquals(bitstream2, bundle.getPrimaryBitstream());
    }

    @Test
    public void testUpdatePrimaryBitstreamBundleNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundleUrl(UUID.randomUUID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdatePrimaryBitstreamInvalidBitstream() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(UUID.randomUUID())))
                        .andExpect(status().isUnprocessableEntity());
        // verify primaryBitstream is still the original one
        bundle = context.reloadEntity(bundle);
        Assert.assertEquals(bitstream, bundle.getPrimaryBitstream());
    }

    @Test
    public void testUpdatePrimaryBitstreamNonExisting() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isBadRequest());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    @Test
    public void testUpdatePrimaryBitstreamNotInBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        bundle.setPrimaryBitstreamID(bitstream);
        Bundle bundle2 = BundleBuilder.createBundle(context, item).withName("Bundle2").build();
        Bitstream bitstream2 = createSecondBitstream(bundle2);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundleUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isUnprocessableEntity());
        // verify primaryBitstream is still the original one
        bundle = context.reloadEntity(bundle);
        Assert.assertEquals(bitstream, bundle.getPrimaryBitstream());
    }

    @Test
    public void testDeletePrimaryBitstream() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete(getBundleUrl(bundle.getID())))
                        .andExpect(status().isNoContent());
        // verify primaryBitstream was actually deleted
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
        // verify bitstream itself still exists
        Assert.assertEquals(1, bundle.getBitstreams().size());
        Assert.assertEquals(bitstream, bundle.getBitstreams().get(0));
    }

    @Test
    public void testDeletePrimaryBitstreamBundleNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete(getBundleUrl(UUID.randomUUID())))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testDeletePrimaryBitstreamBundleNonExisting() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete(getBundleUrl(bundle.getID())))
                        .andExpect(status().isBadRequest());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    private String getBundleUrl(UUID uuid) {
        return "/api/core/bundles/" + uuid + "/primaryBitstream";
    }

    private String getBitstreamUrl(UUID uuid) {
        return "/api/core/bitstreams/" + uuid;
    }

    private Bitstream createSecondBitstream(Bundle bundle) throws Exception {
        String bitstreamContent = "Second Bitstream";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
             return BitstreamBuilder.createBitstream(context, bundle, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .build();
        }
    }
}
