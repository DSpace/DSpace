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
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.io.Files;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class MetadataExportFilteredItemsReportIT extends AbstractIntegrationTestWithDatabase {

    private record ItemKey(String coll, String subject, String author) {}

    private Map<ItemKey, List<Item>> items = new HashMap<>();
    private Map<String, UUID> collUuids = new HashMap<>();
    private String[] collNames = {"coll1", "coll2"};
    private String[] subjects = {"subject1", "subject2"};
    private String[] authors = {"author1", "author2"};
    private int[][] itemCountPerSubjectThenPerAuthor = {{12, 15}, {9, 4}};

    private String filename;
    private Logger logger = org.apache.logging.log4j.LogManager.getLogger();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        filename = configurationService.getProperty("dspace.dir")
            + testProps.get("test.exportcsv").toString();

        TriFunction<Integer, Integer, Integer, String> dateFunction =
                (i, j, k) -> "%04d-%02d-%02d".formatted(2022 + i, 4 + j, k);

        for (int c = 0; c < collNames.length; c++) {
            String collName = collNames[c];
            Collection collection = CollectionBuilder.createCollection(context, community)
                    .withName(collName)
                    .build();
            collUuids.put(collName, collection.getID());
            for (int s = 0; s < subjects.length; s++) {
                String subject = subjects[s];
                for (int a = 0; a < authors.length; a++) {
                    String author = authors[a];
                    for (int k = 0; k < itemCountPerSubjectThenPerAuthor[s][a]; k++) {
                        ItemKey key = new ItemKey(collName, subject, author);
                        Item item = ItemBuilder.createItem(context, collection)
                            .withTitle(String.format("%s item %d", subject, k))
                            .withSubject(subject)
                            .withAuthor(author)
                            .withIssueDate(dateFunction.apply(s, a, k))
                            .build();
                        items.computeIfAbsent(key, aKey -> new ArrayList<>()).add(item);
                    }
                }
            }
        }
        context.restoreAuthSystemState();
    }

    private void checkItemsPresentInFile(String filename, Item... items) throws IOException, CsvException {
        File file = new File(filename);
        Reader reader = Files.newReader(file, Charset.defaultCharset());

        try (CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> lines = csvReader.readAll();
            // length + 1 is because of 1 extra row for the headers
            assertEquals(items.length + 1, lines.size());

            // ignoring the first row as this only contains headers;
            logger.debug("checking content of lines");
            List<String> ids = lines.stream()
                .skip(1)
                .peek(line -> logger.debug(String.join(", ", line)))
                .map(line -> line[0])
                .collect(Collectors.toList());

            for (Item item : items) {
                assertTrue(ids.contains(item.getID().toString()));
            }
        }
    }

    private Item[] allItems() {
        return items.entrySet().stream()
            .map(Map.Entry::getValue)
            .flatMap(List::stream)
            .toArray(Item[]::new);
    }

    private Item[] itemsForSubject(String subject) {
        return items.entrySet().stream()
            .filter(e -> Objects.equals(subject, e.getKey().subject()))
            .map(Map.Entry::getValue)
            .flatMap(List::stream)
            .toArray(Item[]::new);
    }

    private Item[] itemsForCollection(String coll) {
        return items.entrySet().stream()
            .filter(e -> Objects.equals(coll, e.getKey().coll()))
            .map(Map.Entry::getValue)
            .flatMap(List::stream)
            .toArray(Item[]::new);
    }

    @Test
    public void metadataExportFindAllTest() throws Exception {
        int result = runDSpaceScript("metadata-export-filtered-items-report", "-n", filename);
        assertEquals(0, result);

        checkItemsPresentInFile(filename, allItems());
    }

    @Test
    public void metadataExportPerSubjectTest() throws Exception {
        int result = runDSpaceScript("metadata-export-filtered-items-report",
                "-qp", "dc.subject:equals:" + subjects[0], "-n", filename);
        assertEquals(0, result);

        checkItemsPresentInFile(filename, itemsForSubject(subjects[0]));

        result = runDSpaceScript("metadata-export-filtered-items-report",
                "-qp", "dc.subject:equals:" + subjects[1], "-n", filename);
        assertEquals(0, result);

        checkItemsPresentInFile(filename, itemsForSubject(subjects[1]));
    }

    @Test
    public void metadataExportFilteredTest() throws Exception {
        int result = runDSpaceScript("metadata-export-filtered-items-report",
                "-f", "is_item", "-f", "is_discoverable", "-n", filename);
        assertEquals(0, result);

        // All items are discoverable.
        checkItemsPresentInFile(filename, allItems());

        result = runDSpaceScript("metadata-export-filtered-items-report",
                "-f", "has_one_original", "-n", filename);
        assertEquals(0, result);

        // Since no items have any bitstream, none of them can have one single original.
        checkItemsPresentInFile(filename);
    }

    @Test
    public void metadataExportPerCollectionTest() throws Exception {
        UUID uuid = collUuids.get(collNames[0]);
        int result = runDSpaceScript("metadata-export-filtered-items-report",
                "-c", uuid.toString(), "-n", filename);
        assertEquals(0, result);

        checkItemsPresentInFile(filename, itemsForCollection(collNames[0]));

        uuid = collUuids.get(collNames[1]);
        result = runDSpaceScript("metadata-export-filtered-items-report",
                "-c", uuid.toString(), "-n", filename);
        assertEquals(0, result);

        checkItemsPresentInFile(filename, itemsForCollection(collNames[1]));
    }

}
