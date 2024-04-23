/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic integration testing for the SAF Export feature via CLI {@link ItemExportCLI}.
 * https://wiki.lyrasis.org/display/DSDOC7x/Importing+and+Exporting+Items+via+Simple+Archive+Format
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemExportCLIIT extends AbstractIntegrationTestWithDatabase {

    private static final String zipFileName = "saf-export.zip";
    private static final String title = "A Tale of Two Cities";
    private static final String dateIssued = "1990";
    private static final String titleAlternative = "J'aime les Printemps";

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private Collection collection;
    private Path tempDir;
    private Path workDir;

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
        context.restoreAuthSystemState();

        tempDir = Files.createTempDirectory("safExportTest");
        File file = new File(configurationService.getProperty("org.dspace.app.itemexport.work.dir"));
        if (!file.exists()) {
            Files.createDirectory(Path.of(file.getAbsolutePath()));
        }
        workDir = Path.of(file.getAbsolutePath());
    }

    @After
    @Override
    public void destroy() throws Exception {
        PathUtils.deleteOnExit(tempDir);
        for (Path path : Files.list(workDir).collect(Collectors.toList())) {
            PathUtils.deleteOnExit(path);
        }
        super.destroy();
    }

    @Test
    public void exportCollection() throws Exception {
        // create items
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        Item item2 = ItemBuilder.createItem(context, collection)
                .withTitle(title + " 2")
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "COLLECTION",
                "-i", collection.getHandle(), "-d", tempDir.toString(), "-n", "1" };
        perfomExportScript(args);

        checkDir();
    }

    @Test
    public void exportZipCollection() throws Exception {
        // create items
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        Item item2 = ItemBuilder.createItem(context, collection)
                .withTitle(title + " 2")
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "COLLECTION",
                "-i", collection.getHandle(), "-d", tempDir.toString(), "-z", zipFileName, "-n", "1" };
        perfomExportScript(args);

        checkDir();
        checkZip(zipFileName);
    }

    @Test
    public void exportItemWithMetadataOnly() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-n", "1" };
        perfomExportScript(args);

        checkDir();
    }

    @Test
    public void exportItemWithBitstreams() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        // create bitstream
        String bitstreamContent = "TEST TEST TEST";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withMimeType("text/plain")
                    .build();
        }
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-n", "1" };
        perfomExportScript(args);

        checkDir();
    }

    @Test
    public void exportItemWithAnotherMetadataSchema() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .withMetadata("dcterms", "title", "", title)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-n", "1" };
        perfomExportScript(args);

        checkDir();
    }

    @Test
    public void exportZipItemWithBitstreams() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        // create bitstream
        String bitstreamContent = "TEST TEST TEST";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withMimeType("text/plain")
                    .build();
        }
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-z", zipFileName, "-n", "1" };
        perfomExportScript(args);

        checkDir();
        checkZip(zipFileName);
    }

    @Test
    public void migrateCollection() throws Exception {
        // create items
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        Item item2 = ItemBuilder.createItem(context, collection)
                .withTitle(title + " 2")
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "COLLECTION",
                "-i", collection.getHandle(), "-d", tempDir.toString(), "-n", "1", "-m" };
        perfomExportScript(args);

        checkDir();
        checkCollectionMigration();
        checkItemMigration(item1);
        checkItemMigration(item2);
    }

    @Test
    public void migrateItemWithMetadataOnly() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-n", "1", "-m" };
        perfomExportScript(args);

        checkDir();
        checkItemMigration(item);
    }

    @Test
    public void migrateItemWithBitstreams() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .build();
        // create bitstream
        String bitstreamContent = "TEST TEST TEST";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withMimeType("text/plain")
                    .build();
        }
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-n", "1", "-m" };
        perfomExportScript(args);

        checkDir();
        checkItemMigration(item);
    }

    @Test
    public void migrateItemWithAnotherMetadataSchema() throws Exception {
        // create item
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "date", "issued", dateIssued)
                .withMetadata("dc", "title", "alternative", titleAlternative)
                .withMetadata("dcterms", "title", "", title)
                .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "export", "-t", "ITEM",
                "-i", item.getHandle(), "-d", tempDir.toString(), "-n", "1", "-m" };
        perfomExportScript(args);

        checkDir();
        checkItemMigration(item);
    }

    /**
     * Check created export directory
     * @throws Exception
     */
    private void checkDir() throws Exception {
        assertTrue(Files.list(tempDir).findAny().isPresent());
    }

    /**
     * Check created export zip
     * @param zipFileName
     * @throws Exception
     */
    private void checkZip(String zipFileName) throws Exception {
        assertEquals(1,
                Files.list(tempDir)
                .filter(b -> StringUtils.equals(b.getFileName().toString(), zipFileName))
                .count());
    }

    /**
     * Check migration of collection
     * @throws Exception
     */
    private void checkCollectionMigration() throws Exception {
        assertNotNull(collectionService.find(context, collection.getID()));
    }

    /**
     * Check migration of item
     * @param item
     * @throws Exception
     */
    private void checkItemMigration(Item item) throws Exception {
        assertNotNull(itemService.find(context, item.getID()));
    }

    private void perfomExportScript(String[] args)
            throws Exception {
        runDSpaceScript(args);
    }
}
