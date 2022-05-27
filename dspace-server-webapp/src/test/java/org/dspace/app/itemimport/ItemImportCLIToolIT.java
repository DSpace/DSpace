/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.file.PathUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic integration testing for the SAF Import feature
 * https://wiki.lyrasis.org/display/DSDOC7x/Importing+and+Exporting+Items+via+Simple+Archive+Format
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportCLIToolIT extends AbstractEntityIntegrationTest {

    private static final String publicationTitle = "A Tale of Two Cities";
    private static final String personTitle = "Person Test";

    @Autowired
    private ItemService itemService;
    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;
    private Collection collection;
    private Path tempDir;

    @Before
    public void setup() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();
        context.restoreAuthSystemState();

        tempDir = Files.createTempDirectory("safImportTest");
    }

    @After
    @Override
    public void destroy() throws Exception {
        PathUtils.deleteDirectory(tempDir);
        super.destroy();
    }

    @Test
    public void importItemBySafWithMetadataOnly() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", tempDir.toString() + "/mapfile.out"));
        perfomImportScript(parameters);

        checkMetadata();
    }

    @Test
    public void importItemBySafWithBitstreams() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add bitstream
        Path contentsFile = Files.createFile(Path.of(itemDir.toString() + "/contents"));
        Files.writeString(contentsFile,
                "file1.txt");
        Path bitstreamFile = Files.createFile(Path.of(itemDir.toString() + "/file1.txt"));
        Files.writeString(bitstreamFile,
                "TEST TEST TEST");

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", tempDir.toString() + "/mapfile.out"));
        perfomImportScript(parameters);

        checkMetadata();
        checkBitstream();
    }

    @Test
    public void importItemBySafWithAnotherMetadataSchema() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add metadata with another schema
        Files.copy(getClass().getResourceAsStream("metadata_dcterms.xml"),
                Path.of(itemDir.toString() + "/metadata_dcterms.xml"));

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", tempDir.toString() + "/mapfile.out"));
        perfomImportScript(parameters);

        checkMetadata();
        checkMetadataWithAnotherSchema();
    }

    @Test
    public void importItemsBySafWithRelationships() throws Exception {
        context.turnOffAuthorisationSystem();
        // create collection that contains person
        Collection collectionPerson = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Person")
                .withEntityType("Person")
                .build();
        context.restoreAuthSystemState();
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path publicationDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.writeString(Path.of(publicationDir.toString() + "/collections"),
                collection.getID().toString());
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(publicationDir.toString() + "/dublin_core.xml"));
        Files.copy(getClass().getResourceAsStream("relationships"),
                Path.of(publicationDir.toString() + "/relationships"));
        Path personDir = Files.createDirectory(Path.of(safDir.toString() + "/item_001"));
        Files.writeString(Path.of(personDir.toString() + "/collections"),
                collectionPerson.getID().toString());
        Files.copy(getClass().getResourceAsStream("dublin_core-person.xml"),
                Path.of(personDir.toString() + "/dublin_core.xml"));

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", tempDir.toString() + "/mapfile.out"));
        perfomImportScript(parameters);

        checkMetadata();
        checkRelationship();
    }

    @Test
    public void importItemsBySafWithRelationshipsByRelationSchema() throws Exception {
        context.turnOffAuthorisationSystem();
        // create collection that contains person
        Collection collectionPerson = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Person")
                .withEntityType("Person")
                .build();
        Item person = ItemBuilder.createItem(context, collectionPerson)
                .withTitle(personTitle)
                .build();
        context.restoreAuthSystemState();
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        Files.writeString(Path.of(itemDir.toString() + "/metadata_relation.xml"),
                "<dublin_core schema=\"relation\">\n" +
                "    <dcvalue element=\"isAuthorOfPublication\">" + person.getID() + "</dcvalue>\n" +
                "</dublin_core>");

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", tempDir.toString() + "/mapfile.out"));
        perfomImportScript(parameters);

        checkRelationship();
    }

    @Test
    public void resumeImportItemBySafWithMetadataOnly() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-R", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
    }

    @Test
    public void resumeImportItemBySafWithBitstreams() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add bitstream
        Path contentsFile = Files.createFile(Path.of(itemDir.toString() + "/contents"));
        Files.writeString(contentsFile,
                "file1.txt");
        Path bitstreamFile = Files.createFile(Path.of(itemDir.toString() + "/file1.txt"));
        Files.writeString(bitstreamFile,
                "TEST TEST TEST");
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-R", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
        checkBitstream();
    }

    @Test
    public void resumeImportItemBySafWithAnotherMetadataSchema() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add metadata with another schema
        Files.copy(getClass().getResourceAsStream("metadata_dcterms.xml"),
                Path.of(itemDir.toString() + "/metadata_dcterms.xml"));
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-R", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
        checkMetadataWithAnotherSchema();
    }

    @Test
    public void resumeImportItemSkippingTheFirstOneBySafWithMetadataOnly()
            throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Another Title")
                .build();
        context.restoreAuthSystemState();
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_001"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-R", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
    }

    @Test
    public void resumeImportItemSkippingTheFirstOneBySafWithBitstreams()
            throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Another Title")
                .build();
        context.restoreAuthSystemState();
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_001"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add bitstream
        Path contentsFile = Files.createFile(Path.of(itemDir.toString() + "/contents"));
        Files.writeString(contentsFile,
                "file1.txt");
        Path bitstreamFile = Files.createFile(Path.of(itemDir.toString() + "/file1.txt"));
        Files.writeString(bitstreamFile,
                "TEST TEST TEST");
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-R", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
        checkBitstream();
    }

    @Test
    public void resumeImportItemSkippingTheFirstOneBySafWithAnotherMetadataSchema()
            throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Another Title")
                .build();
        context.restoreAuthSystemState();
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_001"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add metadata with another schema
        Files.copy(getClass().getResourceAsStream("metadata_dcterms.xml"),
                Path.of(itemDir.toString() + "/metadata_dcterms.xml"));
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-R", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
        checkMetadataWithAnotherSchema();
    }

    @Test
    public void replaceItemBySafWithMetadataOnly() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Another Title")
                .build();
        context.restoreAuthSystemState();

        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-r", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
    }

    @Test
    public void replaceItemBySafWithBitstreams() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Another Title")
                .build();
        context.restoreAuthSystemState();

        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add bitstream
        Path contentsFile = Files.createFile(Path.of(itemDir.toString() + "/contents"));
        Files.writeString(contentsFile,
                "file1.txt");
        Path bitstreamFile = Files.createFile(Path.of(itemDir.toString() + "/file1.txt"));
        Files.writeString(bitstreamFile,
                "TEST TEST TEST");
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-r", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
        checkBitstream();
    }

    @Test
    public void replaceItemBySafWithAnotherMetadataSchema() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Another Title")
                .build();
        context.restoreAuthSystemState();

        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));
        // add metadata with another schema
        Files.copy(getClass().getResourceAsStream("metadata_dcterms.xml"),
                Path.of(itemDir.toString() + "/metadata_dcterms.xml"));
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-r", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-s", safDir.toString()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkMetadata();
        checkMetadataWithAnotherSchema();
    }

    @Test
    public void deleteItemByMapFile() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(publicationTitle)
                .build();
        context.restoreAuthSystemState();
        // add mapfile
        Path mapFile = Files.createFile(Path.of(tempDir.toString() + "/mapfile.out"));
        Files.writeString(mapFile, "item_000 " + item.getHandle());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-d", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-m", mapFile.toString()));
        perfomImportScript(parameters);

        checkItemDeletion();
    }

    /**
     * Check metadata on imported item
     * @throws Exception
     */
    private void checkMetadata() throws Exception {
        Item item = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next();
        getClient().perform(get("/api/core/items/" + item.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", allOf(
                        matchMetadata("dc.title", publicationTitle),
                        matchMetadata("dc.date.issued", "1990"),
                        matchMetadata("dc.title.alternative", "J'aime les Printemps"))));
    }

    /**
     * Check metadata on imported item
     * @throws Exception
     */
    private void checkMetadataWithAnotherSchema() throws Exception {
        Item item = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next();
        getClient().perform(get("/api/core/items/" + item.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", allOf(
                        matchMetadata("dcterms.title", publicationTitle))));
    }

    /**
     * Check bitstreams on imported item
     * @throws Exception
     */
    private void checkBitstream() throws Exception {
        Bitstream bitstream = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next()
                .getBundles("ORIGINAL").get(0).getBitstreams().get(0);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", allOf(
                        matchMetadata("dc.title", "file1.txt"))));
    }

    /**
     * Check deletion of item by mapfile
     * @throws Exception
     */
    private void checkItemDeletion() throws Exception {
        Iterator<Item> itemIterator = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle);
        assertEquals(itemIterator.hasNext(), false);
    }

    /**
     * Check relationships between imported items
     * @throws Exception
     */
    private void checkRelationship() throws Exception {
        Item item = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next();
        Item author = itemService.findByMetadataField(context, "dc", "title", null, personTitle).next();
        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertEquals(1, relationships.size());
        getClient().perform(get("/api/core/relationships/" + relationships.get(0).getID()).param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.leftPlace", is(0)))
                   .andExpect(jsonPath("$._links.rightItem.href", containsString(author.getID().toString())))
                   .andExpect(jsonPath("$.rightPlace", is(0)))
                   .andExpect(jsonPath("$", Matchers.is(RelationshipMatcher.matchRelationship(relationships.get(0)))));
    }

    private void perfomImportScript(LinkedList<DSpaceCommandLineParameter> parameters)
            throws Exception {
        AtomicReference<Integer> idRef = new AtomicReference<>();
        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);
            getClient(token)
                .perform(multipart("/api/system/scripts/import/processes")
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }
}
