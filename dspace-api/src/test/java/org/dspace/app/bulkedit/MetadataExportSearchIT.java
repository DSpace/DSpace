/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.Files;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MetadataExportSearchIT extends AbstractIntegrationTestWithDatabase {

    private String subject1 = "subject1";
    private String subject2 = "subject2";
    private int numberItemsSubject1 = 30;
    private int numberItemsSubject2 = 2;
    private Item[] itemsSubject1 = new Item[numberItemsSubject1];
    private Item[] itemsSubject2 = new Item[numberItemsSubject2];
    private String filename;
    private Collection collection;
    private Logger logger = Logger.getLogger(MetadataExportSearchIT.class);
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private SearchService searchService = SearchUtils.getSearchService();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // dummy search so that the SearchService gets called in a test context first
        DiscoverQuery query = new DiscoverQuery();
        query.setMaxResults(0);
        searchService.search(context, query);

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community).build();
        filename = configurationService.getProperty("dspace.dir")
            + testProps.get("test.exportcsv").toString();


        for (int i = 0; i < numberItemsSubject1; i++) {
            itemsSubject1[i] = ItemBuilder.createItem(context, collection)
                .withTitle(String.format("%s item %d", subject1, i))
                .withSubject(subject1)
                .withIssueDate("2020-09-" + i)
                .build();
        }

        for (int i = 0; i < numberItemsSubject2; i++) {
            itemsSubject2[i] = ItemBuilder.createItem(context, collection)
                .withTitle(String.format("%s item %d", subject2, i))
                .withSubject(subject2)
                .withIssueDate("2021-09-" + i)
                .build();
        }
        context.restoreAuthSystemState();
    }

    private void checkItemsPresentInFile(String filename, Item[] items) throws IOException, CsvException {
        File file = new File(filename);
        Reader reader = Files.newReader(file, Charset.defaultCharset());
        CSVReader csvReader = new CSVReader(reader);


        List<String[]> lines = csvReader.readAll();
        //length + 1 is because of 1 row extra for the headers
        assertEquals(items.length + 1, lines.size());

        List<String> ids = new ArrayList<>();
        //ignoring the first row as this only contains headers;
        logger.debug("checking content of lines");
        for (int i = 1; i < lines.size(); i++) {
            logger.debug(String.join(", ", lines.get(i)));
            ids.add(lines.get(i)[0]);
        }

        for (Item item : items) {
            assertTrue(ids.contains(item.getID().toString()));
        }
    }

    @Test
    public void metadateExportSearchQueryTest() throws Exception {
        int result = runDSpaceScript("metadata-export-search", "-q", "subject:" + subject1, "-n", filename);

        assertEquals(0, result);
        checkItemsPresentInFile(filename, itemsSubject1);


        result = runDSpaceScript("metadata-export-search", "-q", "subject: " + subject2, "-n", filename);

        assertEquals(0, result);
        checkItemsPresentInFile(filename, itemsSubject2);
    }

    @Test
    public void exportMetadataSearchSpecificContainerTest() throws Exception {
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

        int result = runDSpaceScript(
            "metadata-export-search", "-q", "subject: " + subject1, "-s", collection2.getID().toString(), "-n", filename
        );

        assertEquals(0, result);
        checkItemsPresentInFile(filename, itemsDifferentCollection);
    }

    @Test
    public void exportMetadataSearchFilter() throws Exception {
        int result = runDSpaceScript("metadata-export-search", "-f", "subject,equals=" + subject1, "-n", filename);

        assertEquals(0, result);
        checkItemsPresentInFile(filename, itemsSubject1);
    }

    @Test
    public void exportMetadataSearchFilterDate() throws Exception {
        int result = runDSpaceScript(
            "metadata-export-search", "-f", "dateIssued,equals=[2000 TO 2020]", "-n", filename
        );

        assertEquals(0, result);
        checkItemsPresentInFile(filename, itemsSubject1);
    }

    @Test
    public void exportMetadataSearchMultipleFilters() throws Exception {
        int result = runDSpaceScript(
            "metadata-export-search", "-f", "subject,equals=" + subject1, "-f",
            "title,equals=" + String.format("%s item %d", subject1, 0), "-n", filename
        );

        assertEquals(0, result);
        Item[] expectedResult = Arrays.copyOfRange(itemsSubject1, 0, 1);
        checkItemsPresentInFile(filename, expectedResult);
    }

    @Test
    public void exportMetadataSearchEqualsFilterTest()
        throws Exception {
        context.turnOffAuthorisationSystem();
        Item wellBeingItem = ItemBuilder.createItem(context, collection)
            .withTitle("test item well-being")
            .withSubject("well-being")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("test item financial well-being")
            .withSubject("financial well-being")
            .build();

        context.restoreAuthSystemState();

        int result = runDSpaceScript("metadata-export-search", "-f", "subject,equals=well-being", "-n", filename);

        assertEquals(0, result);
        Item[] expectedResult = new Item[] {wellBeingItem};
        checkItemsPresentInFile(filename, expectedResult);
    }

    @Test
    public void exportMetadataSearchInvalidDiscoveryQueryTest() throws Exception {
        int result = runDSpaceScript("metadata-export-search", "-q", "blabla", "-n", filename);

        assertEquals(0, result);
        Item[] items = {};
        checkItemsPresentInFile(filename, items);
    }

    @Test
    public void exportMetadataSearchNoResultsTest() throws Exception {
        int result = runDSpaceScript(
            "metadata-export-search", "-f", "subject,equals=notExistingSubject", "-n", filename
        );

        assertEquals(0, result);
        Item[] items = {};
        checkItemsPresentInFile(filename, items);
    }

    @Test
    public void exportMetadataSearchNonExistinFacetsTest() throws Exception {
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] {"metadata-export-search", "-f", "nonExisting,equals=" + subject1, "-f",
            "title,equals=" + String.format("%s item %d", subject1, 0), "-n", filename};
        int result = ScriptLauncher.handleScript(
            args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl
        );

        assertEquals(0, result);  // exception should be handled, so the script should finish with 0

        Exception exception = testDSpaceRunnableHandler.getException();
        assertNotNull(exception);
        assertEquals("nonExisting is not a valid search filter", exception.getMessage());
    }
}
