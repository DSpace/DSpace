/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import com.opencsv.CSVReader;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.junit.Test;


/**
 * Integration tests for {@link DOIMigrator}.
 */
public class DOIMigratorIT extends AbstractIntegrationTestWithDatabase {
    private Item[] createTestItems() {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item[] items = { null, null, null };

        items[0] = ItemBuilder.createItem(context, collection)
                .withIdentifier("10.000/123456")
                .build();
        items[1] = ItemBuilder.createItem(context, collection)
                .withIdentifier("10.1234/56789")
                .build();
        items[2] = ItemBuilder.createItem(context, collection)
                .withDoiIdentifier("10.000/7890")
                .build();
        context.restoreAuthSystemState();

        return items;
    }

    @Test
    public void testDryRunExceptPrefix() throws Exception {
        Item[] items = createTestItems();

        String[] args = {
                "--dry-run",
                "--from", "dc.identifier",
                "--to", "dc.relation.hasversion",
                "--except-prefix", "10.000"
        };
        ByteArrayOutputStream capturedOutputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutputStream));
        DOIMigrator.main(args);

        String capturedOutput = capturedOutputStream.toString("us-ascii");
        Reader capturedOutputReader = new StringReader(capturedOutput);
        CSVReader csvReader = new CSVReader(capturedOutputReader);
        Iterator<String[]> csvIterator = csvReader.iterator();

        String[] headerLine = csvIterator.next();
        // extremely basic sanity check
        assertEquals(headerLine[0], "ItemUUID");

        String[] toMigrateLine = csvIterator.next();
        assertEquals(toMigrateLine[0], items[1].getID().toString());
        assertEquals(toMigrateLine[1], "dc.identifier");
        assertEquals(toMigrateLine[2], "10.1234/56789");
        assertEquals(toMigrateLine[3], "dc.relation.hasversion");
        assertEquals(toMigrateLine[4], "https://doi.org/10.1234/56789");

        // check that the other two items are not marked as migratable
        // (one has the wrong prefix, the other has the wrong metadata
        // field)
        assertFalse(csvIterator.hasNext());
        csvReader.close();
    }

    @Test
    public void testDryRunOnlyPrefix() throws Exception {
        Item[] items = createTestItems();

        String[] args = {
                "--dry-run",
                "--from", "dc.identifier",
                "--to", "dc.identifier.doi",
                "--only-prefix", "10.000"
        };
        ByteArrayOutputStream capturedOutputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutputStream));
        DOIMigrator.main(args);

        String capturedOutput = capturedOutputStream.toString("us-ascii");
        Reader capturedOutputReader = new StringReader(capturedOutput);
        CSVReader csvReader = new CSVReader(capturedOutputReader);
        Iterator<String[]> csvIterator = csvReader.iterator();

        String[] headerLine = csvIterator.next();
        assertEquals(headerLine[0], "ItemUUID");

        String[] toMigrateLine = csvIterator.next();
        assertEquals(toMigrateLine[0], items[0].getID().toString());
        assertEquals(toMigrateLine[1], "dc.identifier");
        assertEquals(toMigrateLine[2], "10.000/123456");
        assertEquals(toMigrateLine[3], "dc.identifier.doi");
        assertEquals(toMigrateLine[4], "https://doi.org/10.000/123456");

        assertFalse(csvIterator.hasNext());
        csvReader.close();
    }

    @Test
    public void testRehydrate() throws Exception {
        Item[] items = createTestItems();
        String inputCSV = "ItemUUID,FromFieldName,FromValue,ToFieldName,ToValue\r\n"
            + items[0].getID().toString()
                + ",dc.identifier,10.000/123456,dc.identifier.doi,https://doi.org/10.000/123456\r\n";
        InputStream inputCSVStream = new ReaderInputStream(new StringReader(inputCSV));

        String[] args = { "--rehydrate" };
        System.setIn(inputCSVStream);
        DOIMigrator.main(args);

        Item changedItem = context.reloadEntity(items[0]);
        List<MetadataValue> changedMetadata = changedItem.getMetadata();
        boolean haveDOIMetadata = false;
        for (MetadataValue metadataValue : changedMetadata) {
            MetadataField field = metadataValue.getMetadataField();
            assertFalse(field.getMetadataSchema().getName().equals("dc")
                    && field.getElement().equals("identifier")
                    && field.getQualifier() == null);
            if (field.getMetadataSchema().getName().equals("dc")
                    && field.getElement().equals("identifier")
                    && field.getQualifier().equals("doi")) {
                assertFalse("This is the only dc.identifier.doi",
                        haveDOIMetadata);
                haveDOIMetadata = true;

                assertEquals(metadataValue.getValue(), "https://doi.org/10.000/123456");
            }
        }
        assertTrue(haveDOIMetadata);
    }
}
