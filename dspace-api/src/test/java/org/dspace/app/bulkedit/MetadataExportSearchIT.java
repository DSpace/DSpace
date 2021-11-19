/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

public class MetadataExportSearchIT extends AbstractIntegrationTestWithDatabase {

    private String subject1 = "subject1";
    private String subject2 = "subject2";
    private int numberItemsSubject1 = 30;
    private int numberItemsSubject2 = 2;
    private Item[] itemsSubject1 = new Item[numberItemsSubject1];
    private Item[] itemsSubject2 = new Item[numberItemsSubject2];
    private String filename = "metadataExportSearch.csv";
    TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();

        for (int i = 0; i < numberItemsSubject1; i++) {
            itemsSubject1[i] = ItemBuilder.createItem(context, collection)
                .withTitle(String.format("%s item %d", subject1, i))
                .withSubject(subject1)
                .build();
        }

        for (int i = 0; i < numberItemsSubject2; i++) {
            itemsSubject2[i] = ItemBuilder.createItem(context, collection)
                .withTitle(String.format("%s item %d", subject2, i))
                .withSubject(subject2)
                .build();
        }
        context.restoreAuthSystemState();
    }

    private void checkItemsPresentInFile(String filename, Item[] items) throws IOException {
        File file = new File(filename);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        String[] lines = fileContent.split("\\r?\\n");
        //length + 1 is because of 1 row extra for the headers
        assertEquals(items.length + 1, lines.length);

        List<String> ids = new ArrayList<>();
        //ignoring the first row as this only contains headers;
        for (int i = 1; i < lines.length; i++) {
            ids.add(lines[i].split(",", 2)[0].replaceAll("\"", ""));
        }

        for (Item item : items) {
            assertTrue(ids.contains(item.getID().toString()));
        }
    }

    @Test
    public void metadateExportSearchQueryTest()
        throws InstantiationException, IllegalAccessException, IOException {
        String[] args = new String[] {"metadata-export-search", "-q", "subject:" + subject1};

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        checkItemsPresentInFile(filename, itemsSubject1);


        args = new String[] {"metadata-export-search", "-q", "subject: " + subject2};

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        checkItemsPresentInFile(filename, itemsSubject2);
    }

    @Test
    public void exportMetadataSearchSpecificContainerTest()
        throws IOException, InstantiationException, IllegalAccessException {
        context.turnOffAuthorisationSystem();
        Community community2 = CommunityBuilder.createCommunity(context).build();
        Collection collection2 = CollectionBuilder.createCollection(context, community2).build();

        int numberItemsDifferentCollection = 15;
        Item[] itemsDifferentCollection = new Item[numberItemsDifferentCollection];
        for (int i = 0; i < numberItemsDifferentCollection; i++) {
            itemsDifferentCollection[i] = ItemBuilder.createItem(context, collection2)
                .withTitle("item different collection " + i)
                .withSubject(subject1)
                .build();
        }

        //creating some items with a different subject to make sure the query still works
        for (int i = 0; i < 5; i++) {
            ItemBuilder.createItem(context, collection2)
                .withTitle("item different collection, different subject " + i)
                .withSubject(subject2)
                .build();
        }
        context.restoreAuthSystemState();

        String[] args =
            new String[] {"metadata-export-search", "-q", "subject: " + subject1, "-s", collection2.getID().toString()};
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        checkItemsPresentInFile(filename, itemsDifferentCollection);
    }
}
