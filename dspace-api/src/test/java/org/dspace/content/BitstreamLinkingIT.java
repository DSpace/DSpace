/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

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
    Bitstream original;
    Bitstream copy;


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

        Bundle oldBundle = BundleBuilder.createBundle(context, oldItem)
                .withName("ORIGINAL")
                .build();

        Bundle newBundle = BundleBuilder.createBundle(context, newItem)
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
            copy  = BitstreamBuilder.createBitstream(context, newBundle, is)
                    .withName("bistream two")
                    .build();
        }
        context.restoreAuthSystemState();
    }


    @Test
    public void testCopyBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        bitstreamLinkingService.registerBitstreams(context, original, copy);

        String[] hasCopies = HAS_COPIES.split("\\.");
        List<MetadataValue> copiesUUIDs = bitstreamService.getMetadata(original, hasCopies[0],
                hasCopies[1], hasCopies[2], null);
        assertThat("Metadata amount: " + HAS_COPIES, copiesUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + HAS_COPIES, copiesUUIDs.get(0).getValue(),
                equalTo(copy.getID().toString()));

        String[] isCopyOf = IS_COPY_OF.split("\\.");
        List<MetadataValue> originalUUIDs = bitstreamService.getMetadata(copy, isCopyOf[0],
                isCopyOf[1], isCopyOf[2], null);
        assertThat("Metadata amount: " + IS_COPY_OF, originalUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + IS_COPY_OF, originalUUIDs.get(0).getValue(),
                equalTo(original.getID().toString()));

        List<Bitstream> copies = bitstreamLinkingService.getCopies(context, original);
        assertThat("Copies count match", copies.size(), equalTo(1));
        assertThat("Copies bitstream match", copies.get(0), equalTo(copy));

        List<Bitstream> originals = bitstreamLinkingService.getOriginals(context, copy);
        assertThat("Original count match", originals.size(), equalTo(1));
        assertThat("Original bitstream match", originals.get(0), equalTo(original));

        context.restoreAuthSystemState();

    }

    @Test
    public void testReplacementBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        bitstreamLinkingService.registerReplacementBitstream(context, original, copy);

        String[] hasCopies = IS_REPLACED_BY.split("\\.");
        List<MetadataValue> copiesUUIDs = bitstreamService.getMetadata(original, hasCopies[0],
                hasCopies[1], hasCopies[2], null);
        assertThat("Metadata amount: " + IS_REPLACED_BY, copiesUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + IS_REPLACED_BY, copiesUUIDs.get(0).getValue(),
                equalTo(copy.getID().toString()));

        String[] isCopyOf = IS_REPLACEMENT_OF.split("\\.");
        List<MetadataValue> originalUUIDs = bitstreamService.getMetadata(copy, isCopyOf[0],
                isCopyOf[1], isCopyOf[2], null);
        assertThat("Metadata amount: " + IS_REPLACEMENT_OF, originalUUIDs.size(), equalTo(1));
        assertThat("Metadata value of :" + IS_REPLACEMENT_OF, originalUUIDs.get(0).getValue(),
                equalTo(original.getID().toString()));

        List<Bitstream> copies = bitstreamLinkingService.getReplacements(context, original);
        assertThat("Replacement Copies count match", copies.size(), equalTo(1));
        assertThat("Replacement Copies bitstream match", copies.get(0), equalTo(copy));

        List<Bitstream> originals = bitstreamLinkingService.getOriginalReplacement(context, copy);
        assertThat("Replacement Original count match", originals.size(), equalTo(1));
        assertThat("Replacement Original bitstream match", originals.get(0), equalTo(original));

        context.restoreAuthSystemState();

    }


}
