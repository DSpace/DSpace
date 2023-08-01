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
    Community community;
    Collection collection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community).build();
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
        getClient(token).perform(get(getBundlePrimaryBitstreamUrl(bundle.getID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BitstreamMatcher.matchProperties(bitstream)));
    }

    @Test
    public void testGetPrimaryBitstreamBundleNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(getBundlePrimaryBitstreamUrl(UUID.randomUUID())))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPrimaryBitstreamNonExisting() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(getBundlePrimaryBitstreamUrl(bundle.getID())))
                        .andExpect(status().isNoContent())
                        .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    public void testPostPrimaryBitstream() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
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
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(UUID.randomUUID()))
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
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
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
        Bitstream bitstream2 = createBitstream(bundle);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
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
        Bitstream bitstream2 = createBitstream(bundle2);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isUnprocessableEntity());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    @Test
    public void testPostPrimaryBitstreamCommunityAdmin() throws Exception {
        // create new structure with Admin permissions on Community
        context.turnOffAuthorisationSystem();
        Community com2 = CommunityBuilder.createCommunity(context).withAdminGroup(eperson).build();
        Collection col2 = CollectionBuilder.createCollection(context, com2).build();
        Item item2 = ItemBuilder.createItem(context, col2).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle2.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(),
                                                                           bundle2.getHandle(), bundle2.getType())));
        // verify primaryBitstream was actually added
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertEquals(bitstream2, bundle2.getPrimaryBitstream());

        // verify Community Admin can't set a primaryBitstream outside their own Community
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testPostPrimaryBitstreamCollectionAdmin() throws Exception {
        // create new structure with Admin permissions on Collection
        context.turnOffAuthorisationSystem();
        Collection col2 = CollectionBuilder.createCollection(context, community).withAdminGroup(eperson).build();
        Item item2 = ItemBuilder.createItem(context, col2).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle2.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(),
                                                                           bundle2.getHandle(), bundle2.getType())));
        // verify primaryBitstream was actually added
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertEquals(bitstream2, bundle2.getPrimaryBitstream());

        // verify Collection Admin can't set a primaryBitstream outside their own Collection
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testPostPrimaryBitstreamItemAdmin() throws Exception {
        // create new structure with Admin permissions on Item
        context.turnOffAuthorisationSystem();
        Item item2 = ItemBuilder.createItem(context, collection).withAdminUser(eperson).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle2.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(),
                                                                           bundle2.getHandle(), bundle2.getType())));
        // verify primaryBitstream was actually added
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertEquals(bitstream2, bundle2.getPrimaryBitstream());

        // verify Item Admin can't set a primaryBitstream outside their own Item
        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testPostPrimaryBitstreamForbidden() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testPostPrimaryBitstreamUnauthenticated() throws Exception {
        getClient().perform(post(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdatePrimaryBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        bundle.setPrimaryBitstreamID(bitstream);
        Bitstream bitstream2 = createBitstream(bundle);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
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
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(UUID.randomUUID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdatePrimaryBitstreamInvalidBitstream() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
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
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
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
        Bitstream bitstream2 = createBitstream(bundle2);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isUnprocessableEntity());
        // verify primaryBitstream is still the original one
        bundle = context.reloadEntity(bundle);
        Assert.assertEquals(bitstream, bundle.getPrimaryBitstream());
    }

    @Test
    public void testUpdatePrimaryBitstreamCommunityAdmin() throws Exception {
        // create new structure with Admin permissions on Community
        context.turnOffAuthorisationSystem();
        Community com2 = CommunityBuilder.createCommunity(context).withAdminGroup(eperson).build();
        Collection col2 = CollectionBuilder.createCollection(context, com2).build();
        Item item2 = ItemBuilder.createItem(context, col2).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        bundle2.setPrimaryBitstreamID(bitstream2);
        Bitstream bitstream3 = createBitstream(bundle2);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle2.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream3.getID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(),
                                                                           bundle2.getHandle(), bundle2.getType())));
        // verify primaryBitstream was actually updated
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertEquals(bitstream3, bundle2.getPrimaryBitstream());

        bundle.setPrimaryBitstreamID(bitstream);
        // verify Community Admin can't update a primaryBitstream outside their own Community
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdatePrimaryBitstreamCollectionAdmin() throws Exception {
        // create new structure with Admin permissions on Collection
        context.turnOffAuthorisationSystem();
        Collection col2 = CollectionBuilder.createCollection(context, community).withAdminGroup(eperson).build();
        Item item2 = ItemBuilder.createItem(context, col2).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        bundle2.setPrimaryBitstreamID(bitstream2);
        Bitstream bitstream3 = createBitstream(bundle2);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle2.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream3.getID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(),
                                                                           bundle2.getHandle(), bundle2.getType())));
        // verify primaryBitstream was actually updated
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertEquals(bitstream3, bundle2.getPrimaryBitstream());

        bundle.setPrimaryBitstreamID(bitstream);
        // verify Collection Admin can't update a primaryBitstream outside their own Collection
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdatePrimaryBitstreamItemAdmin() throws Exception {
        // create new structure with Admin permissions on Item
        context.turnOffAuthorisationSystem();
        Item item2 = ItemBuilder.createItem(context, collection).withAdminUser(eperson).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        bundle2.setPrimaryBitstreamID(bitstream2);
        Bitstream bitstream3 = createBitstream(bundle2);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle2.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream3.getID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(),
                                                                           bundle2.getHandle(), bundle2.getType())));
        // verify primaryBitstream was actually updated
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertEquals(bitstream3, bundle2.getPrimaryBitstream());

        bundle.setPrimaryBitstreamID(bitstream);
        // verify Item Admin can't update a primaryBitstream outside their own Item
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdatePrimaryBitstreamForbidden() throws Exception {
        context.turnOffAuthorisationSystem();
        bundle.setPrimaryBitstreamID(bitstream);
        Bitstream bitstream2 = createBitstream(bundle);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdatePrimaryBitstreamUnauthenticated() throws Exception {
        context.turnOffAuthorisationSystem();
        bundle.setPrimaryBitstreamID(bitstream);
        Bitstream bitstream2 = createBitstream(bundle);
        context.restoreAuthSystemState();

        getClient().perform(put(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream2.getID())))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeletePrimaryBitstream() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID())))
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
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(UUID.randomUUID())))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void testDeletePrimaryBitstreamBundleNonExisting() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID())))
                        .andExpect(status().isBadRequest());
        // verify primaryBitstream is still null
        bundle = context.reloadEntity(bundle);
        Assert.assertNull(bundle.getPrimaryBitstream());
    }

    @Test
    public void testDeletePrimaryBitstreamCommunityAdmin() throws Exception {
        // create new structure with Admin permissions on Community
        context.turnOffAuthorisationSystem();
        Community com2 = CommunityBuilder.createCommunity(context).withAdminGroup(eperson).build();
        Collection col2 = CollectionBuilder.createCollection(context, com2).build();
        Item item2 = ItemBuilder.createItem(context, col2).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        bundle2.setPrimaryBitstreamID(bitstream2);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle2.getID())))
                        .andExpect(status().isNoContent());
        // verify primaryBitstream was actually deleted
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertNull(bundle2.getPrimaryBitstream());
        // verify bitstream itself still exists
        Assert.assertEquals(1, bundle2.getBitstreams().size());
        Assert.assertEquals(bitstream2, bundle2.getBitstreams().get(0));

        bundle.setPrimaryBitstreamID(bitstream);
        // verify Community Admin can't delete a primaryBitstream outside their own Community
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testDeletePrimaryBitstreamCollectionAdmin() throws Exception {
        // create new structure with Admin permissions on Collection
        context.turnOffAuthorisationSystem();
        Collection col2 = CollectionBuilder.createCollection(context, community).withAdminGroup(eperson).build();
        Item item2 = ItemBuilder.createItem(context, col2).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        bundle2.setPrimaryBitstreamID(bitstream2);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle2.getID())))
                        .andExpect(status().isNoContent());
        // verify primaryBitstream was actually deleted
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertNull(bundle2.getPrimaryBitstream());
        // verify bitstream itself still exists
        Assert.assertEquals(1, bundle2.getBitstreams().size());
        Assert.assertEquals(bitstream2, bundle2.getBitstreams().get(0));

        bundle.setPrimaryBitstreamID(bitstream);
        // verify Collection Admin can't delete a primaryBitstream outside their own Collection
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testDeletePrimaryBitstreamItemAdmin() throws Exception {
        // create new structure with Admin permissions on Item
        context.turnOffAuthorisationSystem();
        Item item2 = ItemBuilder.createItem(context, collection).withAdminUser(eperson).build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item2).withName("ORIGINAL").build();
        Bitstream bitstream2 = createBitstream(bundle2);
        bundle2.setPrimaryBitstreamID(bitstream2);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle2.getID())))
                        .andExpect(status().isNoContent());
        // verify primaryBitstream was actually deleted
        bundle2 = context.reloadEntity(bundle2);
        Assert.assertNull(bundle2.getPrimaryBitstream());
        // verify bitstream itself still exists
        Assert.assertEquals(1, bundle2.getBitstreams().size());
        Assert.assertEquals(bitstream2, bundle2.getBitstreams().get(0));

        bundle.setPrimaryBitstreamID(bitstream);
        // verify Item Admin can't delete a primaryBitstream outside their own Item
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID()))
                                     .contentType(textUriContentType)
                                     .content(getBitstreamUrl(bitstream.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testDeletePrimaryBitstreamForbidden() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testDeletePrimaryBitstreamUnauthenticated() throws Exception {
        bundle.setPrimaryBitstreamID(bitstream);

        getClient().perform(delete(getBundlePrimaryBitstreamUrl(bundle.getID())))
                        .andExpect(status().isUnauthorized());
    }

    private String getBundlePrimaryBitstreamUrl(UUID uuid) {
        return "/api/core/bundles/" + uuid + "/primaryBitstream";
    }

    private String getBitstreamUrl(UUID uuid) {
        return "/api/core/bitstreams/" + uuid;
    }

    private Bitstream createBitstream(Bundle bundle) throws Exception {
        String bitstreamContent = "Bitstream Content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
             return BitstreamBuilder.createBitstream(context, bundle, is)
                                         .withName("Bitstream")
                                         .withMimeType("text/plain")
                                         .build();
        }
    }
}
