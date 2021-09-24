/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;

public class IIIFControllerIT extends AbstractControllerIntegrationTest {

    public static final String IIIFBundle = "IIIF";

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
    public void findOneIIIFSearchableEntityTypeIT() throws Exception {
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
                    .createBitstream(context, publicItem1, is, IIIFBundle)
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
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(1200)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].images[0].resource.service.@id",
                        Matchers.endsWith(bitstream1.getID().toString())))
                .andExpect(jsonPath("$.sequences[0].canvases[1].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c1")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].label", is("Page 2")))
                .andExpect(jsonPath("$.sequences[0].canvases[1].images[0].resource.service.@id",
                        Matchers.endsWith(bitstream2.getID().toString())))
                .andExpect(jsonPath("$.related.@id",
                        Matchers.containsString("/items/" + publicItem1.getID())));
    }

    @Test
    public void findOneIIIFSearchableWithGlobalConfigIT() throws Exception {
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
                .withEntityType("IIIFSearchable")
                .build();

        String bitstreamContent = "ThisIsSomeText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder
                    .createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream1.jpg")
                    .withMimeType("image/jpeg")
                    .withIIIFLabel("Custom Label")
                    .withIIIFCanvasWidth(3163)
                    .withIIIFCanvasHeight(4220)
                    //.withMimeType("image/jpeg")
                    .build();
        }
        context.restoreAuthSystemState();
        // Expect canvas label, width and height to match bitstream description.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Global 1")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].width", is(2000)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].height", is(3000)))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    public void findOneIIIFSearchableWithInfoJsonIT() throws Exception {
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

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is, IIIFBundle)
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
        }

        String bitstreamContent3 = "ThisIsSomeDummyText3";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent3, CharEncoding.UTF_8)) {
            BitstreamBuilder.
                    createBitstream(context, publicItem1, is, IIIFBundle)
                    .withName("Bitstream3.tiff")
                    .withMimeType("image/tiff")
                    .build();
        }

        context.restoreAuthSystemState();

        // With more than 2 bitstreams in IIIF bundle, the sequence viewing hint should be "paged"
        // unless that has been changed in dspace configuration. This test assumes that DSpace
        // has been configured to return the "individuals" hint for documents to better support
        // search results in Mirador. That is the current dspace.cfg default setting.
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.license", is("https://license.org")))
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.viewingHint", is("individuals")))
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
    public void findOneIIIFEntityTypeIT() throws Exception {
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
                    .withName("Bitstream2.pdf")
                    .withMimeType("application/pdf")
                    .build();
        }

        context.restoreAuthSystemState();

        // Image in the ORIGINAL bundle added as canvas; PDF ignored...
        getClient().perform(get("/iiif/" + publicItem1.getID() + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@context", is("http://iiif.io/api/presentation/2/context.json")))
                .andExpect(jsonPath("$.sequences[0].canvases", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.sequences[0].canvases[0].@id",
                        Matchers.containsString("/iiif/" + publicItem1.getID() + "/canvas/c0")))
                .andExpect(jsonPath("$.sequences[0].canvases[0].label", is("Page 1")))
                .andExpect(jsonPath("$.service").doesNotExist());
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
                .andExpect(jsonPath("$.resources[0].resource.@id",
                        Matchers.containsString(bitstream2.getID() + "/content")));

    }
}
