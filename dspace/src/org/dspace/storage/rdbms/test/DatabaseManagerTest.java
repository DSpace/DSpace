/*
 * DatabaseManagerTest.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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


package org.dspace.storage.rdbms.test;

import java.sql.*;

import junit.extensions.*;
import junit.framework.*;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.*;

/**
 * Test of DatabaseManager
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class DatabaseManagerTest extends TestCase
{
    private static final String TABLENAME = "TestTable";
    private static final String dropSql = "DROP TABLE " + TABLENAME + ";";

    private static final String NEWLINE = System.getProperty("line.separator");
    private static final String createSql =

        "CREATE TABLE " + TABLENAME       + NEWLINE +
        "("                               + NEWLINE +
        "  Id       INTEGER PRIMARY KEY," + NEWLINE +
        "  Name     VARCHAR(64),"         + NEWLINE +
        "  Tstamp   TIMESTAMP,"           + NEWLINE +
        "  Date     DATE,"                + NEWLINE +
        "  Stuff    VARCHAR(255),"        + NEWLINE +
        "  Amount   INTEGER"              + NEWLINE +
        ");"                              + NEWLINE;

    private static final String column = "foo";
    private static final int testInt = 3;
    private static final String testString = "bar";
    private static final boolean testBool = true;
    private static final java.util.Date testDate = new java.util.Date();

    /**
     * Constructor
     *
     * @param name - The name of the TestCase
     */
    public DatabaseManagerTest (String name)
    {
        super(name);
    }

    /**
     * Test setup -- called before each test
     */
    public void setUp()
    {
        try
        {
            // On every run, we get a new connection and
            // clear everything out of the test table
            Context context = new Context();
            DatabaseManager.updateQuery(context, "delete from " + TABLENAME);
            context.complete();
        }
        catch (Exception e)
        {
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test obtaining a connnection
     */
    public void testConnection()
    {
        try
        {
            assertNotNull("Obtaining connection",
                          DatabaseManager.getConnection());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * JUnit test method
     *
     * This tests basic functionality:
     *    insert, update, delete, find
     */
    public void testSimple()
    {
        Boolean autocommit = null;
        Connection connection = null;
        Context context = null;

        try
        {
            ////////////////////
            // Test data
            ////////////////////

            int id = 1;
            int original = 12;
            int changed = 14;
            String name = "Pudntain";
            Date date = new java.sql.Date
                (new java.util.Date().getTime());
            String stuff = "nee";

            // Make a new row
            TableRow first = createTableRow();

            first.setColumn("id",     id);
            first.setColumn("name",   name);
            first.setColumn("stuff",  stuff);
            // first.setColumn("date",   date);
            first.setColumn("amount", original);

            // Insert it
            context = new Context();
            DatabaseManager.insert(context, first);
            context.complete();
            assertEquals("Row count in TestTable should be one",
                         getTestTableCount(), 1);

            // Find it again
            context = new Context();
            TableRow _first = DatabaseManager.find(context, TABLENAME, id);
            context.complete();

            assertNotNull("Trying to find the inserted record", _first);
            assertEquals("Name fields are equal",
                         name, _first.getStringColumn("name"));
            assertEquals("Stuff fields are equal",
                         stuff, _first.getStringColumn("stuff"));
            assertEquals("Amount fields are equal",
                         original, _first.getIntColumn("amount"));
            // Timestamps, dates, etc are problematic, since
            // the DB tends to truncate them
//             assertEquals("Date fields are equal",
//                          first.getDateColumn("date"),
//                          _first.getDateColumn("date"));
            assertEquals("Inserted row and retrieved row should be equal",
                         first, _first);

            // Change the amount
            context = new Context();
            _first.setColumn("amount", changed);

            // Update the record, using the connection
            DatabaseManager.update(context, _first);
            // Find the record, using the connection
            TableRow _test = DatabaseManager.find(context, TABLENAME, id);

            assertEquals("Value of amount should be updated",
                         _test.getIntColumn("amount"),
                         changed);
            // Rollback
            context.abort();

            // Find the record again, using the connection
            context = new Context();
            TableRow _test2 = DatabaseManager.find(context, TABLENAME, id);
            context.complete();

            assertEquals("Value of amount should be original value",
                         _test2.getIntColumn("amount"),
                         original);

            // Delete the row
            context = new Context();
            DatabaseManager.delete(context, _test2);
            context.complete();

            // Should be gone from the database
            context = new Context();
            TableRow _test3 = DatabaseManager.find(context, TABLENAME, id);
            context.complete();

            assertNull("Row should be missing from database", _test3);
            // No rows left
            assertEquals("Row count in TestTable should be zero",
                         getTestTableCount(),
                         0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
        finally
        {
            try
            {
                if (connection != null)
                {
                    if (autocommit != null)
                        connection.setAutoCommit(autocommit.booleanValue());
                    connection.close();
                }
            }
            catch (SQLException sqle)
            {}
        }
    }

    /**
     * Test querying
     */
    public void testQuery()
    {
        try
        {
            int start = 3;
            int max = start + 5;

            // Make a bunch of rows
            Context context = new Context();
            for (int i = start; i < max; i++)
            {
                TableRow row = createTableRow();
                row.setColumn("id", i);
                DatabaseManager.insert(context, row);
            }
            context.complete();

            // Get all the rows
            context = new Context();
            TableRowIterator iterator = DatabaseManager.query
                (context, TABLENAME, "select * from TestTable order by id");

            // Loop through all entries
            int test = start;

            while (iterator.hasNext())
            {
                TableRow row = (TableRow) iterator.next();

                assertEquals("Checking id",
                             row.getIntColumn("id"),
                             test++);
            }

            // The same thing in reverse order
            iterator = DatabaseManager.query
                (context, TABLENAME, "select * from TestTable order by id desc");

            // Loop through all entries
            test = max - 1;
            while (iterator.hasNext())
            {
                TableRow row = (TableRow) iterator.next();

                assertEquals("Checking id",
                             row.getIntColumn("id"),
                             test--);
            }

            context.complete();
        }
        catch (SQLException sqle)
        {
            sqle.printStackTrace();
            fail("SQLException");
        }
    }

    /**
     * Test nulls
     */
    public void testNulls()
    {
        try
        {
            int id = 1;
            TableRow row = createTableRow();
            row.setColumn("id", id);
            row.setColumn("amount", 12);
            row.setColumn("stuff", "nee");
            Context context = null;
            context = new Context();
            DatabaseManager.insert(context, row);
            context.complete();

            context = new Context();
            TableRow test = DatabaseManager.find(context, TABLENAME, id);
            context.complete();

            assertTrue("Stuff should not be null",
                       ! test.isColumnNull("stuff"));
            assertTrue("Amount should not be null",
                       ! test.isColumnNull("amount"));
            assertNotNull("Stuff should not be null",
                          test.getStringColumn("stuff"));
            test.setColumnNull("amount");
            test.setColumnNull("stuff");
            context = new Context();
            DatabaseManager.update(context, test);
            context.complete();

            // Find it again, and check for null fields
            context = new Context();
            TableRow afterUpdate = DatabaseManager.find(context, TABLENAME, id);
            context.complete();

            assertTrue("Amount should be null",
                       afterUpdate.isColumnNull("amount"));
            assertTrue("Stuff should be null",
                       afterUpdate.isColumnNull("stuff"));
        }
        catch (SQLException sqle)
        {
            sqle.printStackTrace();
            fail("SQLException");
        }
    }

    /**
     * Test primary key
     */
    public void testPrimaryKey()
    {
        try
        {
            assertEquals("Primary key column is id",
                         "id",
                         DatabaseManagerShim.publicGetPrimaryKeyColumn(TABLENAME));
        }
        catch (SQLException sqle)
        {
            sqle.printStackTrace();
            fail("SQLException");
        }
    }

    /**
     * Test behavior of nulls
     */
    public void testTableRowNulls()
    {
        try
        {
            TableRow row = createTableRow2();
            row.setColumnNull(column);
            assertTrue("Column set to null is null",
                       row.isColumnNull(column));

            row.setColumn(column, 1);
            assertTrue("Column set to int is not null",
                       ! row.isColumnNull(column));

            row.setColumn(column, "foo");
            assertTrue("Column set to String is not null",
                       ! row.isColumnNull(column));

            row.setColumn(column, false);
            assertTrue("Column set to boolean is not null",
                       ! row.isColumnNull(column));

            row.setColumn(column, new java.util.Date());
            assertTrue("Column set to date is not null",
                       ! row.isColumnNull(column));

            row.setColumnNull(column);
            assertTrue("Column set to null is null",
                       row.isColumnNull(column));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Ensure that values which are set can be retrieved with get.
     */
    public void testSetAndGet()
    {
        try
        {
            TableRow row = createTableRow2();

            row.setColumn(column, testInt);
            assertEquals("Column returns the value it was set to",
                         row.getIntColumn(column), testInt);

            row.setColumn(column, testString);
            assertEquals("Column returns the value it was set to",
                         row.getStringColumn(column), testString);

            row.setColumn(column, testBool);
            assertEquals("Column returns the value it was set to",
                         row.getBooleanColumn(column), testBool);

            row.setColumn(column, testDate);
            assertEquals("Column returns the value it was set to",
                         row.getDateColumn(column), testDate);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test behavior when there is a type mismatch
     */
    public void testTypeMismatch()
    {
        try
        {
            TableRow row = createTableRow2();
            row.setColumn(column, testInt);
            mismatchBoolean(row, column);
            mismatchDate(row, column);
            // mismatchInt(row, column);
            mismatchString(row, column);

            row.setColumn(column, testString);
            mismatchBoolean(row, column);
            mismatchDate(row, column);
            mismatchInt(row, column);
            // mismatchString(row, column);

            row.setColumn(column, testBool);
            // mismatchBoolean(row, column);
            mismatchDate(row, column);
            mismatchInt(row, column);
            mismatchString(row, column);

            row.setColumn(column, testDate);
            mismatchBoolean(row, column);
            // mismatchDate(row, column);
            mismatchInt(row, column);
            mismatchString(row, column);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test that column names are case-insensitive
     */
    public void testColumnNameCaseSensitivity()
    {
        TableRow row = createTableRow2();
        row.setColumn(column.toUpperCase(), testInt);
        assertEquals("Column returns the value it was set to",
                     row.getIntColumn(column.toLowerCase()), testInt);
        row.setColumn(column.toLowerCase(), testInt);
        assertEquals("Column returns the value it was set to",
                     row.getIntColumn(column.toUpperCase()), testInt);

    }

    ////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////

    private void mismatchString(TableRow row, String column)
    {
        try
        {
            row.getStringColumn(column);
            assertTrue("Should get IllegalArgumentException", false);
        }
        catch (IllegalArgumentException iae) {}
    }

    private void mismatchDate(TableRow row, String column)
    {
        try
        {
            row.getDateColumn(column);
            assertTrue("Should get IllegalArgumentException", false);
        }
        catch (IllegalArgumentException iae) {}
    }

    private void mismatchInt(TableRow row, String column)
    {
        try
        {
            row.getIntColumn(column);
            assertTrue("Should get IllegalArgumentException", false);
        }
        catch (IllegalArgumentException iae) {}
    }

    private void mismatchBoolean(TableRow row, String column)
    {
        try
        {
            row.getBooleanColumn(column);
            assertTrue("Should get IllegalArgumentException", false);
        }
        catch (IllegalArgumentException iae) {}
    }

    private TableRow createTableRow2()
    {
        return new TableRow("madeupname",
                            java.util.Arrays.asList(new String[] { column }));
    }


    /**
     * Return the number of rows in the test table
     */
    private int getTestTableCount()
        throws SQLException
    {
        Connection connection = null;

        try
        {
            connection = DatabaseManager.getConnection();
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery("select count(*) from " + TABLENAME);
            return results.next() ? results.getInt(1) : -1;
        }
        finally
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException sqle) {}
            }
        }
    }

    /**
     * For test purposes, we create our own rows with known ids
     */
    private TableRow createTableRow()
        throws SQLException
    {
        return new TableRow
            (TABLENAME,
             DatabaseManagerShim.publicGetNonPrimaryKeyColumnNames(TABLENAME));
    }

    ////////////////////////////////////////
    // JUnit methods
    ////////////////////////////////////////

    /**
     * Test suite
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(DatabaseManagerTest.class);
        // Wrapper to add setup and cleanup
        TestSetup wrapper = new TestSetup(suite)
            {
                public void setUp()
                {
                    try
                    {
                        DatabaseManager.loadSql(dropSql);
                        DatabaseManager.loadSql(createSql);
                    }
                    catch (SQLException sqle)
                    {
                        sqle.printStackTrace();
                        fail("SQLException");
                    }
                }

                public void tearDown()
                {
                    try
                    {
                        DatabaseManager.loadSql(dropSql);
                    }
                    catch (SQLException sqle)
                    {
                        sqle.printStackTrace();
                        fail("SQLException");
                    }
                }
            };

        return wrapper;
    }

    /**
     * Embedded test harness
     *
     * @param argv - Command-line arguments
     */
    public static void main(String[] argv)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
}
