/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.content.BitstreamLinkingServiceImpl.BITSTREAM;
import static org.dspace.content.BitstreamLinkingServiceImpl.DSPACE;
import static org.dspace.content.BitstreamLinkingServiceImpl.HAS_COPIES;
import static org.dspace.content.BitstreamLinkingServiceImpl.IS_COPY_OF;
import static org.dspace.content.BitstreamLinkingServiceImpl.IS_REPLACED_BY;
import static org.dspace.content.BitstreamLinkingServiceImpl.IS_REPLACEMENT_OF;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamLinkingService;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
/**
 * Integration Tests for BitstreamLinkingService
 *
 * @author Nathan Buckingham at atmire.com
 */
public class BitstreamLinkingIT extends AbstractIntegrationTestWithDatabase {

    BitstreamLinkingService bitstreamLinkingService = DSpaceServicesFactory.getInstance()
            .getServiceManager()
            .getServiceByName(BitstreamLinkingServiceImpl.class.getName(),
                              BitstreamLinkingServiceImpl.class);

    BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    Collection col;
    Item oldItem;
    Item newItem;
    Item thirdItem;
    Bitstream original;
    Bitstream copy;
    Bitstream secondCopy;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed.
     *
     * @throws Exception passed through.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();

        col = CollectionBuilder.createCollection(context, community)
                .withEntityType("Publication")
                .build();

        oldItem = ItemBuilder.createItem(context, col).build();
        newItem = ItemBuilder.createItem(context, col).build();
        thirdItem = ItemBuilder.createItem(context, col).build();

        Bundle oldBundle = BundleBuilder.createBundle(context, oldItem)
                .withName("ORIGINAL")
                .build();

        Bundle newBundle = BundleBuilder.createBundle(context, newItem)
                .withName("ORIGINAL")
                .build();

        Bundle thirdBundle = BundleBuilder.createBundle(context, newItem)
                .withName("ORIGINAL")
                .build();

        String bitstreamOneContent = "Dummy content one";
        try (InputStream is = IOUtils.toInputStream(bitstreamOneContent, CharEncoding.UTF_8)) {
            original = BitstreamBuilder.createBitstream(context, oldBundle, is)
                    .withName("bistream one")
                    .build();
        }

        String bitstreamTwoContent = "Dummy content of bitstream two";
        try (InputStream is = IOUtils.toInputStream(bitstreamTwoContent, CharEncoding.UTF_8)) {
            copy = BitstreamBuilder.createBitstream(context, newBundle, is)
                    .withName("bistream two")
                    .build();
        }

        String bitstreamThreeContent = "Dummy content of bitstream three";
        try (InputStream is = IOUtils.toInputStream(bitstreamThreeContent, CharEncoding.UTF_8)) {
            secondCopy = BitstreamBuilder.createBitstream(context, thirdBundle, is)
                    .withName("bistream three")
                    .build();
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void testCopyBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        bitstreamLinkingService.cloneMetadata(context, original, copy);

        List<MetadataValue> copiesUUIDs = bitstreamService.getMetadata(original, DSPACE,
                BITSTREAM, HAS_COPIES, null);
        assertThat("Metadata amount: " + HAS_COPIES, copiesUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + HAS_COPIES, copiesUUIDs.get(0).getValue(),
                equalTo(copy.getID().toString()));

        List<MetadataValue> copyHasCopies = bitstreamService.getMetadata(copy, DSPACE,
                BITSTREAM, HAS_COPIES, null);
        //Test that metadata didn't duplicate
        assertThat("Metadata amount: " + HAS_COPIES, copyHasCopies.size(), equalTo(0));

        List<MetadataValue> originalUUIDs = bitstreamService.getMetadata(copy, DSPACE,
                BITSTREAM, IS_COPY_OF, null);
        assertThat("Metadata amount: " + IS_COPY_OF, originalUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + IS_COPY_OF, originalUUIDs.get(0).getValue(),
                equalTo(original.getID().toString()));

        List<Bitstream> copies = bitstreamLinkingService.getCopies(context, original);
        assertThat("Copies count match", copies.size(), equalTo(1));
        assertThat("Copies bitstream match", copies.get(0), equalTo(copy));

        Bitstream original = bitstreamLinkingService.getOriginal(context, copy);
        assertThat("Original bitstream match", original, equalTo(original));

        bitstreamLinkingService.cloneMetadata(context, copy, secondCopy);

        List<MetadataValue> secondCopies = bitstreamService.getMetadata(copy, DSPACE,
                BITSTREAM, HAS_COPIES, null);
        assertThat("Metadata amount: " + HAS_COPIES, secondCopies.size(), equalTo(1));
        assertThat("Metadata value of :" + HAS_COPIES, secondCopies.get(0).getValue(),
                equalTo(secondCopy.getID().toString()));

        List<MetadataValue> secondCopyHasCopies = bitstreamService.getMetadata(secondCopy, DSPACE,
                BITSTREAM, HAS_COPIES, null);
        //Test that metadata didn't duplicate
        assertThat("Metadata amount: " + HAS_COPIES, secondCopyHasCopies.size(), equalTo(0));

        context.restoreAuthSystemState();
    }

    @Test
    public void testReplacementBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        bitstreamLinkingService.registerReplacementBitstream(context, original, copy);
        List<MetadataValue> copiesUUIDs = bitstreamService.getMetadata(original, DSPACE,
                BITSTREAM, IS_REPLACED_BY, null);
        assertThat("Metadata amount: " + IS_REPLACED_BY, copiesUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + IS_REPLACED_BY, copiesUUIDs.get(0).getValue(),
                equalTo(copy.getID().toString()));

        List<MetadataValue> originalUUIDs = bitstreamService.getMetadata(copy, DSPACE,
                BITSTREAM, IS_REPLACEMENT_OF, null);
        assertThat("Metadata amount: " + IS_REPLACEMENT_OF, originalUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + IS_REPLACEMENT_OF, originalUUIDs.get(0).getValue(),
                equalTo(original.getID().toString()));

        List<Bitstream> copies = bitstreamLinkingService.getReplacements(context, original);
        assertThat("Replacement Copies count match", copies.size(), equalTo(1));
        assertThat("Replacement Copies bitstream match", copies.get(0), equalTo(copy));

        Bitstream original = bitstreamLinkingService.getOriginalReplacement(context, copy);
        assertThat("Replacement Original bitstream match", original, equalTo(original));

        context.restoreAuthSystemState();

    }


}
