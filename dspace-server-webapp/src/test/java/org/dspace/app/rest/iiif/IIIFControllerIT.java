/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class IIIFControllerIT extends AbstractControllerIntegrationTest {

    public static final String IIIFBundle = "IIIF";

    @Autowired
    ItemService itemService;

    @Test
    public void disabledTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 2")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .disableIIIF()
                .build();

        context.restoreAuthSystemState();
        // Status 404
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().is(404));
        getClient().perform(get("/iiif/" + publicItem2.getID() + "/manifest"))
                .andExpect(status().is(404));
    }

    @Test
    public void notFoundTest() throws Exception {
        // Status 404
        getClient().perform(get("/iiif/" + UUID.randomUUID().toString() + "/manifest"))
                .andExpect(status().is(404));
    }

    @Test
    public void findOneIIIFSearchableEntityTypeWithGlobalConfigIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .enableIIIFSearch()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream2.png")
                    .withMimeType("image/png")
                    .build();
        }

        context.restoreAuthSystemState();

        // Default canvas size and label.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.service.profile", is("http://iiif.io/api/search/0/search")))
                .andExpect(jsonPath("$.thumbnail.@id", Matchers.containsString("/iiif/2/"
                        + bitstream1.getID())))
                .andExpect(jsonPath("$.metadata[0].label", is("Title")))
                .andExpect(jsonPath("$.metadata[0].value", is("Public item 1")))
                .andExpect(jsonPath("$.metadata[1].label", is("Issue Date")))
                .andExpect(jsonPath("$.metadata[1].value", is("2017-10-17")))
                .andExpect(jsonPath("$.metadata[2].label", is("Authors")))
                .andExpect(jsonPath("$.metadata[2].value[0]", is("Smith, Donald")))
                .andExpect(jsonPath("$.metadata[2].value[1]", is("Doe, John")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(2200)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].images[0].resource.service.@id",
                        Matchers.endsWith(bitstream1.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[0].label", is("File name")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[0].value", is("Bitstream1.jpg")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[1].label", is("Format")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[1].value", is("JPEG")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[2].label", is("Mime Type")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[2].value", is("image/jpeg")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[3].label", is("File size")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[3].value", is("19 bytes")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[4].label", is("Checksum")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].metadata[4].value",
                        is("11e23c5702595ba512c1c2ee8e8d6153 (MD5)")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Page 2")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].images[0].resource.service.@id",
                        Matchers.endsWith(bitstream2.getID().toString())))
                .andExpect(jsonPath("$.structures").doesNotExist())
                .andExpect(jsonPath("$.related.@id",
                        Matchers.containsString("/items/" + publicItem1.getID())));
    }

    @Test
    public void findOneIIIFSearchableWithMixedConfigIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .withIIIFCanvasWidth(2000)
                .withIIIFCanvasHeight(3000)
                .withIIIFCanvasNaming("Global")
                .enableIIIFSearch()
                .build();

        String bitstreamContent = "ThisIsSomeText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFLabel("Custom Label")
                    .withIIIFCanvasWidth(3163)
                    .withIIIFCanvasHeight(4220)
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream2.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        context.restoreAuthSystemState();
        // Expect canvas label, width and height to match bitstream description.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Custom Label")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(3163)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].height", is(4220)))
                .andExpect(jsonPath("$.sequences[0].canvases[1].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Global 2")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].width", is(2000)))
                .andExpect(jsonPath("$.sequences[0].canvases[1].height", is(3000)))
                .andExpect(jsonPath("$.structures").doesNotExist())
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneIIIFSearchableWithCustomBundleAndConfigIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .enableIIIFSearch()
                .build();

        String bitstreamContent = "ThisIsSomeText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream1.png")
                    .withMimeType("image/png")
                    .withIIIFLabel("Custom Label")
                    .withIIIFCanvasWidth(3163)
                    .withIIIFCanvasHeight(4220)
                    .build();
        }

        context.restoreAuthSystemState();
        // Expect canvas label, width and height to match bitstream description.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Custom Label")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(3163)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].height", is(4220)))
                .andExpect(jsonPath("$.structures").doesNotExist())
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneIIIFEntityPagedHintIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withMetadata("dc", "rights", "uri", "https://license.org")
                .enableIIIF()
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 2")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withMetadata("dc", "rights", "uri", "https://license.org")
                .withIIIFViewingHint("paged")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();

            BitstreamBuilder.
                    createBitstream(context, publicItem2, is, IIIFBundle)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }

        String bitstreamContent2 = "ThisIsSomeDummyText2";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent2, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream2.png")
                    .withMimeType("image/png")
                    .build();

            BitstreamBuilder.
                    createBitstream(context, publicItem2, is, IIIFBundle)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }

        String bitstreamContent3 = "ThisIsSomeDummyText3";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent3, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream3.tiff")
                    .withMimeType("image/tiff")
                    .build();

            BitstreamBuilder.
                    createBitstream(context, publicItem2, is, IIIFBundle)
                    .withName("Bitstream3.tiff")
                    .withMimeType("image/tiff")
                    .build();
        }

        context.restoreAuthSystemState();

        // The sequence viewing hint should be "individuals" in the item metadata.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.license", is("https://license.org")))
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.viewingHint", is("individuals")))
                .andExpect(jsonPath("$.service").doesNotExist());

        getClient().perform(get("/iiif/" + publicItem2.getID() + "/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.license", is("https://license.org")))
            .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
            .andExpect(jsonPath("$.sequences[0].canvases", Matchers.hasSize(3)))
            .andExpect(jsonPath("$.viewingHint", is("paged")))
            .andExpect(jsonPath("$.service").doesNotExist());
    }

    @Test
    public void findOneWithStructures() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withIIIFCanvasHeight(3000)
                .withIIIFCanvasWidth(2000)
                .withIIIFCanvasNaming("Global")
                .enableIIIF()
                .enableIIIFSearch()
                .build();

        String bitstreamContent = "ThisIsSomeText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFToC("Section 1")
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream2.png")
                    .withMimeType("image/png")
                    .withIIIFToC("Section 2")
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream3.tiff")
                    .withMimeType("image/tiff")
                    .withIIIFToC("Section 2")
                    .build();
        }

        context.restoreAuthSystemState();
        // expect structures elements with label and canvas id.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Global 1")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(2000)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].height", is(3000)))
                .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Global 2")))
                .andExpect(jsonPath("$.sequences[0].canvases[2].label", is("Global 3")))
                .andExpect(jsonPath("$.structures[0].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0")))
                .andExpect(jsonPath("$.structures[0].label", is("Table of Contents")))
                .andExpect(jsonPath("$.structures[0].viewingHint", is("top")))
                .andExpect(jsonPath("$.structures[0].ranges[0]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0")))
                .andExpect(jsonPath("$.structures[0].ranges[1]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-1")))
                .andExpect(jsonPath("$.structures[1].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0")))
                .andExpect(jsonPath("$.structures[1].label", is("Section 1")))
                .andExpect(jsonPath("$.structures[1].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.structures[2].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-1")))
                .andExpect(jsonPath("$.structures[2].label", is("Section 2")))
                .andExpect(jsonPath("$.structures[2].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.structures[2].canvases[1]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c2")))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneWithBundleStructures() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withIIIFCanvasHeight(3000)
                .withIIIFCanvasWidth(2000)
                .withIIIFCanvasNaming("Global")
                .enableIIIF()
                .enableIIIFSearch()
                .build();

        String bitstreamContent = "ThisIsSomeText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream2.png")
                    .withMimeType("image/png")
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream3.tiff")
                    .withMimeType("image/tiff")
                    .build();
        }
        context.restoreAuthSystemState();

        // Expected structures elements based on the above test content
        // NOTE: we cannot guarantee the order of Bundles in the Manifest, therefore this test has to simply check
        // that each Bundle exists in the manifest with Canvases corresponding to each bitstream.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                   // should contain 3 canvases, corresponding to each bitstream
                   .andExpect(jsonPath("$.sequences[0].canvases[*].label",
                                       Matchers.contains("Global 1", "Global 2", "Global 3")))

                   // First structure should be a Table of Contents
                   .andExpect(jsonPath("$.structures[0].@id",
                                       Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0")))
                   .andExpect(jsonPath("$.structures[0].label", is("Table of Contents")))
                   .andExpect(jsonPath("$.structures[0].viewingHint", is("top")))
                   .andExpect(jsonPath("$.structures[0].ranges[0]",
                                       Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0")))
                   .andExpect(jsonPath("$.structures[0].ranges[1]",
                                       Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-1")))

                   // Should contain a structure with label=IIIF, corresponding to IIIF bundle
                   // It should have exactly 2 canvases (corresponding to 2 bitstreams)
                   .andExpect(jsonPath("$.structures[?(@.label=='IIIF')].canvases[0]").exists())
                   .andExpect(jsonPath("$.structures[?(@.label=='IIIF')].canvases[1]").exists())
                   .andExpect(jsonPath("$.structures[?(@.label=='IIIF')].canvases[2]").doesNotExist())

                   // Should contain a structure with label=ORIGINAL, corresponding to ORIGINAL bundle
                   // It should have exactly 1 canvas (corresponding to 1 bitstream)
                   .andExpect(jsonPath("$.structures[?(@.label=='ORIGINAL')].canvases[0]").exists())
                   .andExpect(jsonPath("$.structures[?(@.label=='ORIGINAL')].canvases[1]").doesNotExist())
                   .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneWithHierarchicalStructures() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .enableIIIFSearch()
                .build();

        String bitstreamContent = "ThisIsSomeText";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFToC("Section 1")
                    .build();
        }
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream2.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFToC("Section 1|||a")
                    .build();
        }
        Bitstream bitstream3 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream3 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream3.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFToC("Section 1|||a")
                    .build();
        }
        Bitstream bitstream4 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream4 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream4.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFToC("Section 1|||b")
                    .build();
        }
        Bitstream bitstream5 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream5 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream5.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFToC("Section 1")
                    .build();
        }
        Bitstream bitstream6 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream6 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream6.png")
                    .withMimeType("image/png")
                    .withIIIFToC("Section 2")
                    .build();
        }
        Bitstream bitstream7 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream7 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream7.tiff")
                    .withMimeType("image/tiff")
                    .withIIIFToC("Section 2")
                    .build();
        }
        Bitstream bitstream8 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream8 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream8.tiff")
                    .withMimeType("image/tiff")
                    .withIIIFToC("Section 2|||sub 2-1")
                    .build();
        }

        context.restoreAuthSystemState();
        // expect structures elements with label and canvas id.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases", Matchers.hasSize(8)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].images[0].resource.@id",
                        Matchers.containsString(bitstream1.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[1].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].images[0].resource.@id",
                        Matchers.containsString(bitstream2.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[2].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c2")))
                .andExpect(jsonPath("$.sequences[0].canvases[2].images[0].resource.@id",
                        Matchers.containsString(bitstream3.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[3].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c3")))
                .andExpect(jsonPath("$.sequences[0].canvases[3].images[0].resource.@id",
                        Matchers.containsString(bitstream4.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[4].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c4")))
                .andExpect(jsonPath("$.sequences[0].canvases[4].images[0].resource.@id",
                        Matchers.containsString(bitstream5.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[5].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c5")))
                .andExpect(jsonPath("$.sequences[0].canvases[5].images[0].resource.@id",
                        Matchers.containsString(bitstream6.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[6].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c6")))
                .andExpect(jsonPath("$.sequences[0].canvases[6].images[0].resource.@id",
                        Matchers.containsString(bitstream7.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[7].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c7")))
                .andExpect(jsonPath("$.sequences[0].canvases[7].images[0].resource.@id",
                        Matchers.containsString(bitstream8.getID().toString())))
                .andExpect(jsonPath("$.structures[0].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0")))
                // the toc contains two top sections 1 & 2 without direct children canvases
                .andExpect(jsonPath("$.structures[0].label", is("Table of Contents")))
                .andExpect(jsonPath("$.structures[0].viewingHint", is("top")))
                .andExpect(jsonPath("$.structures[0].ranges", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.structures[0].ranges[0]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0")))
                .andExpect(jsonPath("$.structures[0].ranges[1]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-1")))
                .andExpect(jsonPath("$.structures[0].canvases").doesNotExist())
                // section 1 contains bitstream 1 and 5 and the sub section a and b
                .andExpect(jsonPath("$.structures[1].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0")))
                .andExpect(jsonPath("$.structures[1].label", is("Section 1")))
                .andExpect(jsonPath("$.structures[1].ranges", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.structures[1].ranges[0]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0-0")))
                .andExpect(jsonPath("$.structures[1].ranges[1]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0-1")))
                .andExpect(jsonPath("$.structures[1].canvases", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.structures[1].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.structures[1].canvases[1]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c4")))
                // section 1 > a contains bitstream 2 and 3, no sub sections
                .andExpect(jsonPath("$.structures[2].label", is("a")))
                .andExpect(jsonPath("$.structures[2].ranges").doesNotExist())
                .andExpect(jsonPath("$.structures[2].canvases", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.structures[2].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.structures[2].canvases[1]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c2")))
                // section 1 > b contains only the bitstream 4 and no sub sections
                .andExpect(jsonPath("$.structures[3].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-0-1")))
                .andExpect(jsonPath("$.structures[3].label", is("b")))
                .andExpect(jsonPath("$.structures[3].ranges").doesNotExist())
                .andExpect(jsonPath("$.structures[3].canvases", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.structures[3].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c3")))
                // section 2 contains bitstream 6 and 7, sub section "sub 2-1"
                .andExpect(jsonPath("$.structures[4].label", is("Section 2")))
                .andExpect(jsonPath("$.structures[4].ranges", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.structures[4].ranges[0]",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-1-0")))
                .andExpect(jsonPath("$.structures[4].canvases", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.structures[4].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c5")))
                .andExpect(jsonPath("$.structures[4].canvases[1]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c6")))
                // section 2 > sub 2-1 contains only the bitstream 8 no sub sections
                .andExpect(jsonPath("$.structures[5].@id",
                        Matchers.endsWith("/iiif/" + publicItem1.getID() + "/manifest/range/r0-1-0")))
                .andExpect(jsonPath("$.structures[5].label", is("sub 2-1")))
                .andExpect(jsonPath("$.structures[5].ranges").doesNotExist())
                .andExpect(jsonPath("$.structures[5].canvases", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.structures[5].canvases[0]",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c7")))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneIIIFNotSearcheableIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withMetadata("dc", "rights", "uri", "https://license.org")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }

        context.restoreAuthSystemState();

        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.license", is("https://license.org")))
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.service").doesNotExist());

    }

    @Test
    public void findOneIIIFWithOtherContentIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withMetadata("dc", "rights", "uri", "https://license.org")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is, "OtherContent")
                    .withName("file.xml")
                    .withMimeType("application/xml")
                    .build();
        }

        context.restoreAuthSystemState();

        // Expect seeAlso annotation list.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.license", is("https://license.org")))
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.seeAlso.@type", is("sc:AnnotationList")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.service").doesNotExist());

    }

    @Test
    public void findOneUsingOriginalBundleIgnoreFileIT() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream2.mp4")
                    .withMimeType("video/mp4")
                    .build();
        }
        Bitstream pdf = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            pdf = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream3.pdf")
                    .withMimeType("application/pdf")
                    .build();
        }

        context.restoreAuthSystemState();

        // Image in the ORIGINAL bundle added as canvas; MP4 ignored, PDF offered as rendering...
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.rendering.@id",
                        Matchers.endsWith(pdf.getID().toString() + "/content")))
                .andExpect(jsonPath("$.rendering.label", is("Bitstream3.pdf")))
                .andExpect(jsonPath("$.rendering.format", is("application/pdf")))
                .andExpect(jsonPath("$.service").doesNotExist());
    }

    @Test
    public void findOneIIIFRestrictedItem() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();

        Group staffGroup = GroupBuilder.createGroup(context)
                .withName("Staff")
                .build();
        Group anotherGroup = GroupBuilder.createGroup(context)
                .withName("anotherGroup")
                .build();

        Item restrictedItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Restricted item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .withIIIFCanvasWidth(2000)
                .withIIIFCanvasHeight(3000)
                .withIIIFCanvasNaming("Global")
                .enableIIIFSearch()
                .withReaderGroup(staffGroup)
                .build();

        EPerson staffEperson = EPersonBuilder.createEPerson(context).withEmail("staff@example.com")
                .withPassword(password).withGroupMembership(staffGroup).build();
        String bitstreamContent = "ThisIsSomeText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, restrictedItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFLabel("Custom Label")
                    .withIIIFCanvasWidth(3163)
                    .withIIIFCanvasHeight(4220)
                    .withReaderGroup(staffGroup)
                    .build();
        }
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, restrictedItem1, is)
                    .withName("Bitstream2.jpg")
                    .withMimeType("image/jpeg")
                    .withReaderGroup(anotherGroup)
                    .build();
        }
        context.restoreAuthSystemState();
        // anonymous cannot get the iiif manifest for the restricted item
        getClient().perform(get("/iiif/" + restrictedItem1.getID() + "/manifest"))
                .andExpect(status().isUnauthorized());
        // not authorized eperson cannot get the iiif manifest
        getClient(getAuthToken(eperson.getEmail(), password))
                .perform(get("/iiif/" + restrictedItem1.getID() + "/manifest")).andExpect(status().isForbidden());
        // authorized eperson get the full manifest including canvas related to not accessible bitstreams
        // access to the bitstream is eventually denied/granted via the IIIF server for a downgraded image
        // Expect canvas label, width and height to match bitstream description.
        getClient(getAuthToken(staffEperson.getEmail(), password))
                .perform(get("/iiif/" + restrictedItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + restrictedItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Custom Label")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(3163)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].height", is(4220)))
                .andExpect(jsonPath("$.sequences[0].canvases[1].@id",
                        Matchers.containsString("/iiif/" + restrictedItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Global 2")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].width", is(2000)))
                .andExpect(jsonPath("$.sequences[0].canvases[1].height", is(3000)))
                .andExpect(jsonPath("$.structures").doesNotExist())
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneCanvas() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("IMG1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        context.restoreAuthSystemState();

        // Single canvas.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/canvas/c0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@type", is("sc:Canvas")))
                .andExpect(jsonPath("$.metadata[0].label", is("File name")))
                .andExpect(jsonPath("$.metadata[0].value", is("IMG1.jpg")))
                .andExpect(jsonPath("$.metadata[1].label", is("Format")))
                .andExpect(jsonPath("$.metadata[1].value", is("JPEG")))
                .andExpect(jsonPath("$.metadata[2].label", is("Mime Type")))
                .andExpect(jsonPath("$.metadata[2].value", is("image/jpeg")))
                .andExpect(jsonPath("$.metadata[3].label", is("File size")))
                .andExpect(jsonPath("$.metadata[3].value", is("19 bytes")))
                .andExpect(jsonPath("$.metadata[4].label", is("Checksum")))
                .andExpect(jsonPath("$.metadata[4].value", is("11e23c5702595ba512c1c2ee8e8d6153 (MD5)")))
                .andExpect(jsonPath("$.images[0].@type", is("oa:Annotation")));
    }

    @Test
    public void missingCanvas() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("IMG1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        context.restoreAuthSystemState();
        // Status 404.  The item contains only one bitstream. The item manifest likewise contains one canvas.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/canvas/c2"))
                .andExpect(status().is(404));
    }

    @Test
    public void getAnnotationListSeeAlso() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .enableIIIF()
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("IMG1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                    createBitstream(context, publicItem1, is, "OtherContent")
                    .withName("file.xml")
                    .withMimeType("application/xml")
                    .build();
        }

        context.restoreAuthSystemState();

        // Expect seeAlso AnnotationList if the dspace item includes an OtherContent bundle.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest/seeAlso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@type", is("sc:AnnotationList")))
                .andExpect(jsonPath("$.resources[0].@type", is("oa:Annotation")))
                .andExpect(jsonPath("$.resources[0].motivation", is ("oa:linking")))
                .andExpect(jsonPath("$.resources[0].resource.@id",
                        Matchers.containsString(bitstream2.getID() + "/content")));

    }

    @Test
    public void findOneWithCacheEvictionAfterBitstreamUpdate() throws Exception {
        String patchRequestBody =
            "[{\"op\": \"replace\",\"path\": \"/metadata/iiif.label/0/value\",\"value\": \"Test label\"}]";

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                                           .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .enableIIIF()
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Bitstream1.jpg")
                .withMimeType("image/jpeg")
                .build();
        }

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withIIIFLabel("Original label")
                .withName("Bitstream2.png")
                .withMimeType("image/png")
                .build();
        }

        context.restoreAuthSystemState();

        // Default canvas size and label.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata[0].label", is("Title")))
                   .andExpect(jsonPath("$.metadata[0].value", is("Public item 1")))
                   .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Original label")));

        String token = getAuthToken(admin.getEmail(), password);

        // The Bitstream update should also remove the manifest from the cache.
        getClient(token).perform(patch("/api/core/bitstreams/" + bitstream2.getID())
            .content(patchRequestBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        // Verify that the updated canvas label is in the manifest.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Test label")));
    }

    @Test
    public void findOneWithCacheEvictionAfterBitstreamRemoval() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                                           .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .enableIIIF()
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withIIIFLabel("Original label")
                    .withName("Bitstream2.png")
                    .withMimeType("image/png")
                    .build();
        }

        context.restoreAuthSystemState();

        // Default canvas size and label.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata[0].label", is("Title")))
                   .andExpect(jsonPath("$.metadata[0].value", is("Public item 1")))
                   .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Original label")));

        String token = getAuthToken(admin.getEmail(), password);

        // The Bitstream deletion should also remove the manifest from the cache.
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream2.getID()))
                        .andExpect(status().isNoContent());

        // Verify that the updated manifest has only a single canvas.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.sequences[0].canvases.length()", Matchers.equalTo(1)));
    }

    @Test
    public void findOneWithCacheEvictionAfterItemUpdate() throws Exception {
        String patchRequestBody =
                "[{\"op\": \"replace\",\"path\": \"/metadata/dc.title/0/value\",\"value\": \"Public item (revised)\"}]";

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                                           .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .enableIIIF()
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .build();
        }

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Bitstream2.png")
                    .withMimeType("image/png")
                    .build();
        }

        context.restoreAuthSystemState();

        // Default canvas size and label.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata[0].label", is("Title")))
                   .andExpect(jsonPath("$.metadata[0].value", is("Public item 1")));

        String token = getAuthToken(admin.getEmail(), password);

        // The Item update should also remove the manifest from the cache.
        getClient(token).perform(patch("/api/core/items/" + publicItem1.getID())
                                .content(patchRequestBody)
                                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // Verify that the updated title is in the manifest.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata[0].value", is("Public item (revised)")));
    }

}
