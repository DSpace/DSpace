/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.file.PathUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.flywaydb.core.internal.util.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic integration testing for the SAF Import feature via CLI {@link ItemImportCLI}.
 * https://wiki.lyrasis.org/display/DSDOC7x/Importing+and+Exporting+Items+via+Simple+Archive+Format
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportCLIIT extends AbstractIntegrationTestWithDatabase {

    private static final String ZIP_NAME = "saf.zip";
    private static final String PDF_NAME = "test.pdf";
    private static final String publicationTitle = "A Tale of Two Cities";
    private static final String personTitle = "Person Test";

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private Collection collection;
    private Path tempDir;
    private Path workDir;
    private static final String TEMP_DIR = ItemImport.TEMP_DIR;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(
                        context, publication, person, "isAuthorOfPublication",
                        "isPublicationOfAuthor", 0, null, 0, null)
                .withCopyToLeft(false).withCopyToRight(true).build();
        context.restoreAuthSystemState();

        tempDir = Files.createTempDirectory("safImportTest");
        File file = new File(configurationService.getProperty("org.dspace.app.batchitemimport.work.dir"));
        if (!file.exists()) {
            Files.createDirectory(Path.of(file.getAbsolutePath()));
        }
        workDir = Path.of(file.getAbsolutePath());
    }

    @After
    @Override
    public void destroy() throws Exception {
        PathUtils.deleteDirectory(tempDir);
        for (Path path : Files.list(workDir).collect(Collectors.toList())) {
            PathUtils.delete(path);
        }
        super.destroy();
    }

    @Test
    public void importItemBySafWithMetadataOnly() throws Exception {
        // create simple SAF
        Path safDir = Files.createDirectory(Path.of(tempDir.toString() + "/test"));
        Path itemDir = Files.createDirectory(Path.of(safDir.toString() + "/item_000"));
        Files.copy(getClass().getResourceAsStream("dublin_core.xml"),
                Path.of(itemDir.toString() + "/dublin_core.xml"));

        String[] args = new String[] { "import", "-a", "-e", admin.getEmail(), "-c", collection.getID().toString(),
                "-s", safDir.toString(), "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-e", admin.getEmail(), "-c", collection.getID().toString(),
                "-s", safDir.toString(), "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-e", admin.getEmail(), "-c", collection.getID().toString(),
                "-s", safDir.toString(), "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-p", "-e", admin.getEmail(),
                "-s", safDir.toString(), "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-p", "-e", admin.getEmail(), "-c",
                collection.getID().toString(), "-s", safDir.toString(), "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

        checkRelationship();
    }

    @Test
    public void importItemByZipSafWithBitstreams() throws Exception {
        // use simple SAF in zip format
        Files.copy(getClass().getResourceAsStream("saf-bitstreams.zip"),
                Path.of(tempDir.toString() + "/" + ZIP_NAME));

        String[] args = new String[] { "import", "-a", "-e", admin.getEmail(), "-c", collection.getID().toString(),
                "-s", tempDir.toString(), "-z", ZIP_NAME, "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

        checkMetadata();
        checkMetadataWithAnotherSchema();
        checkBitstream();

        // confirm that TEMP_DIR still exists
        File workTempDir = new File(workDir + File.separator + TEMP_DIR);
        assertTrue(workTempDir.exists());
    }

    @Test
    public void importItemByZipSafWithRelationships() throws Exception {
        context.turnOffAuthorisationSystem();
        // create collection that contains person
        Collection collectionPerson = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Person")
                .withEntityType("Person")
                .build();
        // create person
        Item person = ItemBuilder.createItem(context, collectionPerson)
                .withTitle(personTitle)
                .build();
        context.restoreAuthSystemState();
        // use simple SAF in zip format
        Files.copy(getClass().getResourceAsStream("saf-relationships.zip"),
                Path.of(tempDir.toString() + "/" + ZIP_NAME));

        String[] args = new String[] { "import", "-a", "-p", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", tempDir.toString(), "-z", ZIP_NAME,
                "-m", tempDir.toString() + "/mapfile.out" };
        perfomImportScript(args);

        checkMetadata();
        checkRelationship();
    }

    @Test
    public void importItemByZipSafInvalidMimetype() throws Exception {
        // use sample PDF file
        Files.copy(getClass().getResourceAsStream("test.pdf"),
                   Path.of(tempDir.toString() + "/" + PDF_NAME));

        String[] args = new String[] { "import", "-a", "-e", admin.getEmail(), "-c", collection.getID().toString(),
                                       "-s", tempDir.toString(), "-z", PDF_NAME, "-m", tempDir.toString()
                                                                                       + "/mapfile.out" };
        try {
            perfomImportScript(args);
        } catch (Exception e) {
            // should throw an exception due to invalid mimetype
            assertEquals(UnsupportedOperationException.class, ExceptionUtils.getRootCause(e).getClass());
        }
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

        String[] args = new String[] { "import", "-a", "-R", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-R", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-R", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-R", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-R", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-a", "-R", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-r", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-r", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-r", "-e", admin.getEmail(),
                "-c", collection.getID().toString(), "-s", safDir.toString(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

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

        String[] args = new String[] { "import", "-d", "-e", admin.getEmail(),
                "-m", mapFile.toString() };
        perfomImportScript(args);

        checkItemDeletion();
    }

    /**
     * Check metadata on imported item
     * @throws Exception
     */
    private void checkMetadata() throws Exception {
        Item item = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next();
        assertEquals(item.getName(), publicationTitle);
        assertEquals(itemService.getMetadata(item, "dc.date.issued"), "1990");
        assertEquals(itemService.getMetadata(item, "dc.title.alternative"), "J'aime les Printemps");
    }

    /**
     * Check metadata on imported item
     * @throws Exception
     */
    private void checkMetadataWithAnotherSchema() throws Exception {
        Item item = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next();
        assertEquals(item.getName(), publicationTitle);
        assertEquals(itemService.getMetadata(item, "dcterms.title"), publicationTitle);
    }

    /**
     * Check bitstreams on imported item
     * @throws Exception
     */
    private void checkBitstream() throws Exception {
        Bitstream bitstream = itemService.findByMetadataField(context, "dc", "title", null, publicationTitle).next()
                .getBundles("ORIGINAL").get(0).getBitstreams().get(0);
        assertEquals(bitstream.getName(), "file1.txt");
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
        assertEquals(author.getID(), relationships.get(0).getRightItem().getID());
        assertEquals(item.getID(), relationships.get(0).getLeftItem().getID());
    }

    private void perfomImportScript(String[] args)
            throws Exception {
        runDSpaceScript(args);
    }
}
