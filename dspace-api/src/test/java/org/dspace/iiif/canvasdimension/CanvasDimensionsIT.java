/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CanvasDimensionsIT extends AbstractIntegrationTestWithDatabase  {

    protected Community child1;
    protected Community child2;
    protected Collection col1;
    protected Collection col2;
    protected Collection col3;
    protected Item iiifItem;
    protected Item iiifItem2;
    protected Item iiifItem3;
    protected Bitstream bitstream;
    protected Bitstream bitstream2;

    private final static String METADATA_IIIF_HEIGHT = "iiif.image.height";
    private final static String METADATA_IIIF_WIDTH = "iiif.image.width";

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setup() throws IOException {

        System.setOut(new PrintStream(outContent));

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                 .withName("Sub Community 1")
                                 .build();
        child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                 .withName("Sub Community 2")
                                 .build();

        col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        System.setOut(originalOut);
        super.destroy();
    }

    @Test
    public void processItemNoForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
                .createBitstream(context, iiifItem, input)
                .withName("Bitstream2.jpg")
                .withMimeType("image/jpeg")
                .build();
        context.restoreAuthSystemState();

        String handle = iiifItem.getHandle();
        execCanvasScript(handle);
        // The test image is small so the canvas dimension should be doubled, e.g. height 200 -> height 400
        assertTrue(bitstream.getMetadata().stream()
                        .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                        .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processCollectionNoForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .build();
        context.restoreAuthSystemState();

        String id = col1.getID().toString();
        execCanvasScript(id);
        // The test image is small so the canvas dimension should be doubled, e.g. height 200 -> height 400
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processSubCommunityNoForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .build();
        context.restoreAuthSystemState();

        String id = child1.getID().toString();
        execCanvasScript(id);

        // The test image is small so the canvas dimension should be doubled, e.g. height 200 -> height 400
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processParentCommunityNoForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .build();
        context.restoreAuthSystemState();

        String handle = parentCommunity.getHandle();
        execCanvasScript(handle);
        // The test image is small so the canvas dimension should be doubled, e.g. height 200 -> height 400
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processParentCommunityMultipleSubsNoForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new Items
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();
        iiifItem2 = ItemBuilder.createItem(context, col2)
                               .withTitle("Test Item2")
                               .withIssueDate("2017-10-17")
                               .enableIIIF()
                               .build();

        // Add jpeg image bitstreams (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .build();

        input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream2 = BitstreamBuilder
            .createBitstream(context, iiifItem2, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .build();

        context.restoreAuthSystemState();

        String id = parentCommunity.getID().toString();
        execCanvasScript(id);

        // All bitstreams should be updated with canvas metadata.
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));
        assertTrue(bitstream2.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream2.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processItemWithForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        context.restoreAuthSystemState();

        String id = iiifItem.getID().toString();
        execCanvasScriptForceOption(id);

        // The existing metadata should be updated
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processCollectionWithForce() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        context.restoreAuthSystemState();

        String id = col1.getID().toString();
        execCanvasScriptForceOption(id);

        // The existing metadata should be updated
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));

    }

    @Test
    public void processItemWithExistingMetadata() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        context.restoreAuthSystemState();

        String handle = iiifItem.getHandle();
        execCanvasScript(handle);
        // The existing canvas metadata should be unchanged
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("100")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("100")));

    }

    @Test
    public void processParentCommunityWithMaximum() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item 1")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();
        // Second item
        iiifItem2 = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item 2")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();
        // Third item so we can test max2process
        iiifItem3 = ItemBuilder.createItem(context, col1)
                               .withTitle("Test Item3")
                               .withIssueDate("2017-10-17")
                               .enableIIIF()
                               .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream2 = BitstreamBuilder
            .createBitstream(context, iiifItem2, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        input = this.getClass().getResourceAsStream("cat.jpg");
        Bitstream bitstream3 = BitstreamBuilder
            .createBitstream(context, iiifItem3, input)
            .withName("Bitstream3.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();

        context.restoreAuthSystemState();

        String id = parentCommunity.getID().toString();

        execCanvasScriptWithMaxRecs(id);
        // check System.out for number of items processed.
        assertEquals("2 IIIF items were processed.", StringUtils.chomp(outContent.toString()));
    }

    @Test
    public void processParentCommunityWithMultipleSkip() throws Exception {
        context.turnOffAuthorisationSystem();
        col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3").build();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();
        // Second item so we can test max2process
        iiifItem2 = ItemBuilder.createItem(context, col2)
                                    .withTitle("Test Item")
                                    .withIssueDate("2017-10-17")
                                    .enableIIIF()
                                    .build();

        iiifItem3 = ItemBuilder.createItem(context, col3)
                                    .withTitle("Test Item")
                                    .withIssueDate("2017-10-17")
                                    .enableIIIF()
                                    .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream2 = BitstreamBuilder
            .createBitstream(context, iiifItem2, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();

        input = this.getClass().getResourceAsStream("cat.jpg");
        Bitstream bitstream3 = BitstreamBuilder
            .createBitstream(context, iiifItem3, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();

        context.restoreAuthSystemState();

        String id = parentCommunity.getID().toString();

        execCanvasScriptWithSkipList(id,col2.getHandle() + "," + col3.getHandle());
        // The test image is small so the canvas dimension should be doubled, e.g. height 200 -> height 400
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));
        // Second bitstream should be unchanged because its within a skipped collection
        assertTrue(bitstream2.getMetadata().stream()
                             .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                             .anyMatch(m -> m.getValue().contentEquals("100")));
        assertTrue(bitstream2.getMetadata().stream()
                             .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                             .anyMatch(m -> m.getValue().contentEquals("100")));
        // Third bitstream should be unchanged because its within a skipped collection
        assertTrue(bitstream3.getMetadata().stream()
                             .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                             .anyMatch(m -> m.getValue().contentEquals("100")));
        assertTrue(bitstream3.getMetadata().stream()
                             .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                             .anyMatch(m -> m.getValue().contentEquals("100")));

    }

    @Test
    public void processParentCommunityWithSingleSkip() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new Item
        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();
        // Second item so we can test max2process
        iiifItem2 = ItemBuilder.createItem(context, col2)
                                    .withTitle("Test Item")
                                    .withIssueDate("2017-10-17")
                                    .enableIIIF()
                                    .build();

        // Add jpeg image bitstream (300 x 200)
        InputStream input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream = BitstreamBuilder
            .createBitstream(context, iiifItem, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();
        input = this.getClass().getResourceAsStream("cat.jpg");
        bitstream2 = BitstreamBuilder
            .createBitstream(context, iiifItem2, input)
            .withName("Bitstream2.jpg")
            .withMimeType("image/jpeg")
            .withIIIFCanvasWidth(100)
            .withIIIFCanvasHeight(100)
            .build();

        context.restoreAuthSystemState();

        String id = parentCommunity.getID().toString();

        execCanvasScriptWithSkipList(id, col2.getHandle());
        // The test image is small so the canvas dimension should be doubled, e.g. height 200 -> height 400
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                            .anyMatch(m -> m.getValue().contentEquals("400")));
        assertTrue(bitstream.getMetadata().stream()
                            .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                            .anyMatch(m -> m.getValue().contentEquals("600")));
        // Second bitstream should be unchanged because it's inside a skipped collection
        assertTrue(bitstream2.getMetadata().stream()
                             .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_HEIGHT))
                             .anyMatch(m -> m.getValue().contentEquals("100")));
        assertTrue(bitstream2.getMetadata().stream()
                             .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_WIDTH))
                             .anyMatch(m -> m.getValue().contentEquals("100")));

    }

    private void execCanvasScript(String id) throws Exception {
        runDSpaceScript("iiif-canvas-dimensions", "-e", "admin@email.com", "-i", id);
    }

    private void execCanvasScriptForceOption(String id) throws Exception {
        runDSpaceScript("iiif-canvas-dimensions", "-e", "admin@email.com", "-i", id, "-f");
    }

    private void execCanvasScriptWithMaxRecs(String id) throws Exception {
        // maximum 2
        runDSpaceScript("iiif-canvas-dimensions", "-e", "admin@email.com", "-i",  id, "-m", "2", "-f", "-q");
    }

    private void execCanvasScriptWithSkipList(String id, String skip) throws Exception {
        runDSpaceScript("iiif-canvas-dimensions", "-e", "admin@email.com", "-i", id, "-s", skip, "-f");
    }

}
