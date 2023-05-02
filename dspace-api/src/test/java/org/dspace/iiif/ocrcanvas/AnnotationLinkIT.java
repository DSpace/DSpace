/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.ocrcanvas;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

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

public class AnnotationLinkIT extends AbstractIntegrationTestWithDatabase {

    protected Community child1;
    protected Community child2;
    protected Collection col1;
    protected Collection col2;
    protected Collection col3;
    protected Item iiifItem;
    protected Item iiifItem2;
    protected Bitstream ocrBitstream;
    protected Bitstream ocrBitstream2;
    protected Bitstream imageBitstream;
    protected Bitstream imageBitstream2;

    private static final String METADATA_CANVASID_SCHEMA = "bitstream";
    private static final String METADATA_CANVASID_ELEMENT = "iiif";
    private static final String METADATA_CANVASID_QUALIFIER = "canvasid";
    private static final String METADATA_CANVASID_FIELD = METADATA_CANVASID_SCHEMA + "." +
        METADATA_CANVASID_ELEMENT + "."  + METADATA_CANVASID_QUALIFIER;

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
    public void processItemAddAlto() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr = this.getClass().getResourceAsStream("alto.xml");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("ocr.xml")
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String itemID = iiifItem.getID().toString();
        execScript(itemID);

        assertTrue(ocrBitstream.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream.getID().toString())));

    }

    @Test
    public void processItemAddHocr() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("test.hocr")
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String itemID = iiifItem.getID().toString();
        execScript(itemID);

        assertTrue(ocrBitstream.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream.getID().toString())));

    }

    @Test
    public void processItemAddMiniOcr() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("miniocr.xml")
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String itemID = iiifItem.getID().toString();
        execScript(itemID);

        assertTrue(ocrBitstream.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream.getID().toString())));

    }

    @Test
    public void processCommunity() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        iiifItem2 = ItemBuilder.createItem(context, col2)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("miniocr.xml")
                                       .withMimeType("text/xml")
                                       .build();

        InputStream image2 = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream2 = BitstreamBuilder.createBitstream(context, iiifItem2, image2)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr2 = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream2 = BitstreamBuilder.createBitstream(context, iiifItem2, ocr2, "OtherContent")
                                       .withName("miniocr.xml")
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String id = parentCommunity.getID().toString();
        execScript(id);
        assertTrue(ocrBitstream.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream.getID().toString())));
        assertTrue(ocrBitstream2.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream2.getID().toString())));

    }

    @Test
    public void processCollection() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        iiifItem2 = ItemBuilder.createItem(context, col1)
                               .withTitle("Test Item")
                               .withIssueDate("2017-10-17")
                               .enableIIIF()
                               .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("miniocr.xml")
                                       .withMimeType("text/xml")
                                       .build();

        InputStream image2 = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream2 = BitstreamBuilder.createBitstream(context, iiifItem2, image2)
                                          .withName("image.jpg")
                                          .withMimeType("image/jpeg")
                                          .build();

        InputStream ocr2 = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream2 = BitstreamBuilder.createBitstream(context, iiifItem2, ocr2, "OtherContent")
                                        .withName("miniocr.xml")
                                        .withMimeType("text/xml")
                                        .build();

        context.restoreAuthSystemState();

        String id = col1.getID().toString();
        execScript(id);
        assertTrue(ocrBitstream.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream.getID().toString())));
        assertTrue(ocrBitstream2.getMetadata().stream()
                                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                                .anyMatch(m -> m.getValue().contentEquals(imageBitstream2.getID().toString())));

    }

    @Test
    public void processDeleteMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Item iiifItemForDelete = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        Bitstream imageBitstream = BitstreamBuilder.createBitstream(context, iiifItemForDelete, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream ocr = this.getClass().getResourceAsStream("alto.xml");
        Bitstream ocrBitstreamForDelete = BitstreamBuilder.createBitstream(context, iiifItemForDelete,
                                                              ocr, "OtherContent")
                                       .withName("ocr.xml")
                                       .withCanvasId(imageBitstream.getID().toString())
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String itemID = iiifItemForDelete.getID().toString();

        execDeleteScript(itemID);
        assertFalse(ocrBitstreamForDelete.getMetadata().stream()
                                .anyMatch(m -> m.getMetadataField()
                                                .toString('.').contentEquals(METADATA_CANVASID_FIELD)));

    }

    @Test
    public void processItemWithNoOcr() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();


        InputStream ocr = this.getClass().getResourceAsStream("notocr.txt");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("notocr.txt")
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String itemID = iiifItem.getID().toString();
        execScript(itemID);

        assertFalse(ocrBitstream.getMetadata().stream()
                                .anyMatch(m -> m.getMetadataField()
                                                .toString('.').contentEquals(METADATA_CANVASID_FIELD)));

    }

    @Test
    public void processReplaceCanvasMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Item itemForReplace = ItemBuilder.createItem(context, col1)
                                            .withTitle("Test Item")
                                            .withIssueDate("2017-10-17")
                                            .enableIIIF()
                                            .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        Bitstream imageBitstream = BitstreamBuilder.createBitstream(context, itemForReplace, image)
                                                   .withName("image.jpg")
                                                   .withMimeType("image/jpeg")
                                                   .build();

        InputStream ocr = this.getClass().getResourceAsStream("alto.xml");
        Bitstream ocrBitstreamForReplace = BitstreamBuilder.createBitstream(context, itemForReplace,
                                                              ocr, "OtherContent")
                                                          .withName("ocr.xml")
                                                          .withCanvasId("43ccba5c-a428-4cfb-bc5a-7c3123f4a1ea")
                                                          .withMimeType("text/xml")
                                                          .build();

        context.restoreAuthSystemState();

        String itemID = itemForReplace.getID().toString();

        execReplaceScript(itemID);
        assertTrue(ocrBitstreamForReplace.getMetadata().stream()
                               .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_CANVASID_FIELD))
                               .anyMatch(m -> m.getValue().contentEquals(imageBitstream.getID().toString())));

    }

    @Test
    public void processItemWithMissingOcr() throws Exception {
        context.turnOffAuthorisationSystem();

        iiifItem = ItemBuilder.createItem(context, col1)
                              .withTitle("Test Item")
                              .withIssueDate("2017-10-17")
                              .enableIIIF()
                              .build();

        InputStream image = this.getClass().getResourceAsStream("cat.jpg");
        imageBitstream = BitstreamBuilder.createBitstream(context, iiifItem, image)
                                         .withName("image.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();

        InputStream image2 = this.getClass().getResourceAsStream("cat.jpg");
        BitstreamBuilder.createBitstream(context, iiifItem, image2)
                                         .withName("image2.jpg")
                                         .withMimeType("image/jpeg")
                                         .build();


        InputStream ocr = this.getClass().getResourceAsStream("test.hocr");
        ocrBitstream = BitstreamBuilder.createBitstream(context, iiifItem, ocr, "OtherContent")
                                       .withName("test.hocr")
                                       .withMimeType("text/xml")
                                       .build();

        context.restoreAuthSystemState();

        String itemID = iiifItem.getID().toString();
        execScript(itemID);

        assertFalse(ocrBitstream.getMetadata().stream()
                                .anyMatch(m -> m.getMetadataField()
                                                .toString('.').contentEquals(METADATA_CANVASID_FIELD)));

    }

    private int execScript(String id) throws Exception {
        return runDSpaceScript("iiif-ocr-canvas-link", "-e", "admin@email.com", "-i", id);
    }

    private int execDeleteScript(String id) throws Exception {
        return runDSpaceScript("iiif-ocr-canvas-link", "-e", "admin@email.com", "-i", id, "-d");
    }


    private int execReplaceScript(String id) throws Exception {
        return runDSpaceScript("iiif-ocr-canvas-link", "-e", "admin@email.com", "-i", id, "-r");
    }
}
