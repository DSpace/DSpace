/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.mediafilter.factory.MediaFilterServiceFactory;
import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.InstallItemService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RestrictedBundleBitstreamIT extends AbstractIntegrationTestWithDatabase {

    private final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    private final MediaFilterService mediaFilterService =
            MediaFilterServiceFactory.getInstance().getMediaFilterService();
    private final InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    Collection collection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("parent community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test Restricted Bundle Collection").build();
        context.restoreAuthSystemState();
    }
    @Test
    public void testRestrictedBundleBitstreamInProgress() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("In-progress item with a license added")
                .grantLicense()
                .build();
        context.restoreAuthSystemState();

        // Expect license bundle and bitstream to have the usual set of workspace policies
        // so the submitter can ADD, etc.
        // This class isn't concerned with the policies themselves, just testing that the logic
        // to clear all policies and return early is not being called
        Bundle licenseBundle = workspaceItem.getItem().getBundles("LICENSE").getFirst();
        Bitstream licenseBitstream = licenseBundle.getBitstreams().getFirst();
        Assert.assertFalse("In-progress license bundle should have ADDs etc",
                licenseBundle.getResourcePolicies().isEmpty());
        Assert.assertFalse("In-progress license bitstream should have ADDs etc",
                licenseBitstream.getResourcePolicies().isEmpty());

        // Install the item
        context.turnOffAuthorisationSystem();
        installItemService.installItem(context, workspaceItem);
        context.restoreAuthSystemState();

        Bundle reloadedLicenseBundle = context.reloadEntity(licenseBundle);
        Bitstream reloadedBitstream = context.reloadEntity(licenseBitstream);

        // Expect license bundle and bitstream to have NO policies
        Assert.assertTrue("Installed license bundle should have NO policies",
                reloadedLicenseBundle.getResourcePolicies().isEmpty());
        Assert.assertTrue("Installed license bitstream should have NO policies",
                reloadedBitstream.getResourcePolicies().isEmpty());
    }

    @Test
    public void testRestrictedBundleBitstreamArchived() throws Exception {
        context.turnOffAuthorisationSystem();
        // New public item, in archive. Then add a SWORD bundle + bitstream.
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Archived item with SWORD bundle added")
                .inArchive()
                .build();
        Bundle swordBundle = BundleBuilder.createBundle(context, item).withName("SWORD").build();
        InputStream bitstreamContent = IOUtils.toInputStream("Test archived bitstream", CharEncoding.UTF_8);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, bitstreamContent)
                .withName("SWORD file").build();
        bundleService.addBitstream(context, swordBundle, bitstream);
        context.restoreAuthSystemState();

        // Expect NO policies on bundle or bitstream
        Bundle reloadedSwordBundle = context.reloadEntity(swordBundle);
        Bitstream reloadedSwordBitstream = context.reloadEntity(bitstream);
        Assert.assertTrue("SWORD bundle should contain no resource policies",
                reloadedSwordBundle.getResourcePolicies().isEmpty());
        Assert.assertTrue("SWORD bitstream should contain no resource policies",
                reloadedSwordBitstream.getResourcePolicies().isEmpty());
    }

    @Test
    public void testRestrictedBundleBitstreamMediaFilter() throws Exception {
        context.turnOffAuthorisationSystem();
        // New public item, in archive. Then add a TEXT bundle + bitstream and call the
        // "update policies" method with the media filter service, which normally applies additional policies.
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Archived item with SWORD bundle added")
                .inArchive()
                .build();
        Bundle textBundle = BundleBuilder.createBundle(context, item).withName("TEXT").build();
        InputStream bitstreamContent = IOUtils.toInputStream("Test archived bitstream", CharEncoding.UTF_8);
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, bitstreamContent)
                .withName("Derived fulltext file").build();
        bundleService.addBitstream(context, textBundle, bitstream);
        mediaFilterService.updatePoliciesOfDerivativeBitstreams(context, item, bitstream);
        context.restoreAuthSystemState();

        // Expect NO policies on bundle or bitstream
        Bundle reloadedSwordBundle = context.reloadEntity(textBundle);
        Bitstream reloadedSwordBitstream = context.reloadEntity(bitstream);
        Assert.assertTrue("TEXT bundle should contain no resource policies",
                reloadedSwordBundle.getResourcePolicies().isEmpty());
        Assert.assertTrue("TEXT bitstream should contain no resource policies",
                reloadedSwordBitstream.getResourcePolicies().isEmpty());
    }
}
