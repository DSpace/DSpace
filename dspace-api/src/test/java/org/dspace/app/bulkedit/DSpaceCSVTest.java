/*
 * DSpaceCSVTest.java
 *
 * Copyright (c) 2002-2010, Duraspace.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of Duraspace nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.bulkedit;

import java.io.*;

import org.dspace.AbstractUnitTest;
import org.dspace.core.Context;

import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

import org.apache.log4j.Logger;


/**
 * Unit Tests for class DSpaceCSV
 *
 * @author Stuart Lewis
 */
public class DSpaceCSVTest extends AbstractUnitTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(DSpaceCSVTest.class);

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Test the reading and parsing of CSV files
     */
    @Test
    public  void testDSpaceCSV()
    {
        try
        {
            // Test the CSV parsing
            String[] csv = {"id,collection,\"dc.title[en]\",dc.contributor.author,dc.description.abstract",
                            "1,2,Easy line,\"Lewis, Stuart\",A nice short abstract",
                            "2,2,Two authors,\"Lewis, Stuart||Bloggs, Joe\",Two people wrote this item",
                            "3,2,Three authors,\"Lewis, Stuart||Bloggs, Joe||Loaf, Meat\",Three people wrote this item",
                            "4,2,\"Two line\ntitle\",\"Lewis, Stuart\",abstract",
                            "5,2,\"\"\"Embedded quotes\"\" here\",\"Lewis, Stuart\",\"Abstract with\ntwo\nnew lines\"",
                            "6,2,\"\"\"Unbalanced embedded\"\" quotes\"\" here\",\"Lewis, Stuart\",\"Abstract with\ntwo\nnew lines\"",};
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
            // Test the CSV parsing was OK
            Context c = new Context();
            DSpaceCSV dcsv = new DSpaceCSV(new File(filename), c);
            String[] lines = dcsv.getCSVLinesAsStringArray();
            assertThat("testDSpaceCSV Good CSV", lines.length, equalTo(7));
            

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
            // Test the CSV parsing was OK
            try
            {
                dcsv = new DSpaceCSV(new File(filename), c);
                lines = dcsv.getCSVLinesAsStringArray();

                fail("An exception should have been thrown due to bad CSV");
            } catch (Exception e)
            {
                    assertThat("testDSpaceCSV Bad heading CSV", e.getMessage(), equalTo("Unknown metadata element in heading: dc.contributor.foobar"));
            }
            lines = dcsv.getCSVLinesAsStringArray();
            assertThat("testDSpaceCSV Good CSV", lines.length, equalTo(7));


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
            // Test the CSV parsing was OK
            try
            {
                dcsv = new DSpaceCSV(new File(filename), c);
                lines = dcsv.getCSVLinesAsStringArray();

                fail("An exception should have been thrown due to bad CSV");
            } catch (Exception e)
            {
                assertThat("testDSpaceCSV Bad heading CSV", e.getMessage(), equalTo("Unknown metadata schema in heading: dcdc.title"));
            }

            // Delete the test file
            File toDelete = new File(filename);
            toDelete.delete();
        }
        catch (Exception ex) {
            log.error("IO Error while creating test CSV file", ex);
            fail("IO Error while creating test CSV file");
        }
    }
}
