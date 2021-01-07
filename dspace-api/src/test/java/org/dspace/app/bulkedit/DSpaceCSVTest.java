/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.junit.Test;


/**
 * Unit Tests for class DSpaceCSV
 *
 * @author Stuart Lewis
 */
public class DSpaceCSVTest extends AbstractUnitTest {
    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DSpaceCSVTest.class);

    /**
     * Test the reading and parsing of CSV files
     */
    @Test
    public void testDSpaceCSV() {
        try {
            // Test the CSV parsing
            String[] csv = {"id,collection,\"dc.title[en]\",dc.contributor.author,dc.description.abstract",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,Easy line,\"Lewis, Stuart\",A nice short abstract",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,Two authors,\"Lewis, Stuart||Bloggs, Joe\",Two people wrote " +
                    "this item",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,Three authors,\"Lewis, Stuart||Bloggs, Joe||Loaf, Meat\"," +
                    "Three people wrote this item",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,\"Two line\n\ntitle\",\"Lewis, Stuart\",abstract",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,\"Empty lines\n\nshould work too (DS-3245).\",\"Lewis, " +
                    "Stuart\",abstract",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,\"\"\"Embedded quotes\"\" here\",\"Lewis, Stuart\",\"Abstract" +
                    " with\ntwo\nnew lines\"",
                "+,56599ad5-c7d2-4ac3-8354-a1f277d5a31f,\"\"\"Unbalanced embedded\"\" quotes\"\" here\",\"Lewis, " +
                    "Stuart\",\"Abstract with\ntwo\nnew lines\"",};
            // Write the string to a file
            String filename = "test.csv";
            BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(filename), "UTF-8"));
            for (String csvLine : csv) {
                out.write(csvLine + "\n");
            }
            out.flush();
            out.close();
            out = null;

            // Test the CSV parsing was OK
            DSpaceCSV dcsv = new DSpaceCSV(FileUtils.openInputStream(new File(filename)), context);
            String[] lines = dcsv.getCSVLinesAsStringArray();
            assertThat("testDSpaceCSV Good CSV", lines.length, equalTo(8));

            // Check the new lines are OK
            List<DSpaceCSVLine> csvLines = dcsv.getCSVLines();
            DSpaceCSVLine line = csvLines.get(5);
            List<String> value = new ArrayList<String>();
            value.add("Abstract with\ntwo\nnew lines");
            assertThat("testDSpaceCSV New lines", line.valueToCSV(value, dcsv.valueSeparator),
                       equalTo("\"Abstract with\ntwo\nnew lines\""));
            line = null;

            // Test the CSV parsing with a bad heading element value
            csv[0] = "id,collection,\"dc.title[en]\",dc.contributor.foobar[en-US],dc.description.abstract";
            // Write the string to a file
            filename = "test.csv";
            out = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(filename), "UTF-8"));
            for (String csvLine : csv) {
                out.write(csvLine + "\n");
            }
            out.flush();
            out.close();
            out = null;

            // Test the CSV parsing was OK
            try {
                dcsv = new DSpaceCSV(FileUtils.openInputStream(new File(filename)), context);
                lines = dcsv.getCSVLinesAsStringArray();

                fail("An exception should have been thrown due to bad CSV");
            } catch (Exception e) {
                assertThat("testDSpaceCSV Bad heading CSV", e.getMessage(),
                           equalTo("Unknown metadata element in column 4: dc.contributor.foobar"));
            }
            lines = dcsv.getCSVLinesAsStringArray();
            assertThat("testDSpaceCSV Good CSV", lines.length, equalTo(8));


            // Test the CSV parsing with a bad heading schema value
            csv[0] = "id,collection,\"dcdc.title[en]\",dc.contributor[en-US],dc.description.abstract";
            // Write the string to a file
            filename = "test.csv";
            out = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(filename), "UTF-8"));
            for (String csvLine : csv) {
                out.write(csvLine + "\n");
            }
            out.flush();
            out.close();
            out = null;

            // Test the CSV parsing was OK
            try {
                dcsv = new DSpaceCSV(FileUtils.openInputStream(new File(filename)), context);
                lines = dcsv.getCSVLinesAsStringArray();

                fail("An exception should have been thrown due to bad CSV");
            } catch (Exception e) {
                assertThat("testDSpaceCSV Bad heading CSV", e.getMessage(),
                           equalTo("Unknown metadata schema in column 3: dcdc.title"));
            }

            // Delete the test file
            File toDelete = new File(filename);
            toDelete.delete();
            toDelete = null;

            // Nullify resources so JUnit will clean them up
            dcsv = null;
            lines = null;
        } catch (Exception ex) {
            log.error("IO Error while creating test CSV file", ex);
            fail("IO Error while creating test CSV file");
        }
    }
}
