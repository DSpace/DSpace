/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import mockit.Deencapsulation;
import mockit.Mock;
import mockit.MockUp;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import static org.dspace.storage.rdbms.DatabaseManager.getColumnNames;

/**
 * Mocks a DatabaseManager so unit tests can be run without a real DB connection
 * The code is basically the same as the original DatabaseManager but it
 * establishes a connection to an in-memory database.
 *
 * @author pvillega
 */
public final class MockDatabaseManager
        extends MockUp<DatabaseManager>
{
    // Set our logger to specify the Mock class, so we know which logs are from the "real" vs "mock" class
    private static final Logger log = Logger.getLogger(MockDatabaseManager.class);
    
    // Get the values of private static variables 'isOracle' and 'isPostgres' from 
    // DatabaseManager itself (by using Deencapsulation)
    private static final boolean isOracle = Deencapsulation.getField(DatabaseManager.class, "isOracle");
    private static final boolean isPostgres = Deencapsulation.getField(DatabaseManager.class, "isPostgres");
    
    /**
     * Override/Mock the default "setConstraintDeferred()" method in order to 
     * add some custom H2-specific code (look for the comments with "H2" in them).
     * 
     * Set the constraint check to deferred (commit time)
     *
     * @param context
     *            The context object
     * @param constraintName
     *            the constraint name to deferred
     * @throws SQLException
     */
    @Mock
    public static void setConstraintDeferred(Context context,
            String constraintName) throws SQLException
    {
        log.debug("Mocked setContraintDeferred() method for H2 database");
        
        Statement statement = null;
        try
        {
            statement = context.getDBConnection().createStatement();
            //statement.execute("SET CONSTRAINTS " + constraintName + " DEFERRED");
            // H2 does NOT support "SET CONSTRAINTS" syntax. 
            // Instead it requires the following SQL
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            statement.close();

        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                }
            }
        }
    }
    
    /**
     * Override/Mock the default "setConstraintImmediate()" method in order to 
     * add some custom H2-specific code (look for the comments with "H2" in them).
     * 
     * Set the constraint check to immediate (every query)
     *
     * @param context
     *            The context object
     * @param constraintName
     *            the constraint name to check immediately after every query
     * @throws SQLException
     */
    @Mock
    public static void setConstraintImmediate(Context context,
            String constraintName) throws SQLException
    {
        log.debug("Mocked setContraintImmediate() method for H2 database");
        
        Statement statement = null;
        try
        {
            statement = context.getDBConnection().createStatement();
            //statement.execute("SET CONSTRAINTS " + constraintName + " IMMEDIATE");
            // H2 does NOT support "SET CONSTRAINTS" syntax. 
            // Instead it requires the following SQL
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            statement.close();
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                }
            }
        }
    }
     
    /**
     * Override/Mock the default "process()" method in order to add some custom 
     * H2-specific code (look for the comments with "H2" in them below).
     * 
     * Convert the current row in a ResultSet into a TableRow object.
     *
     * @param results
     *            A ResultSet to process
     * @param table
     *            The name of the table
     * @param pColumnNames
     *            The name of the columns in this resultset
     * @return A TableRow object with the data from the ResultSet
     * @exception SQLException
     *                If a database error occurs
     */
    @Mock
    static TableRow process(ResultSet results, String table, List<String> pColumnNames) throws SQLException
    {
        // Added for H2 debugging
        log.debug("Mocked process() method for H2 database");
        
        ResultSetMetaData meta = results.getMetaData();
        int columns = meta.getColumnCount() + 1;

        // If we haven't been passed the column names try to generate them from the metadata / table
        List<String> columnNames = pColumnNames != null ? pColumnNames :
                                        ((table == null) ? getColumnNames(meta) : getColumnNames(table));

        TableRow row = new TableRow(DatabaseManager.canonicalize(table), columnNames);

        // Process the columns in order
        // (This ensures maximum backwards compatibility with
        // old JDBC drivers)
        for (int i = 1; i < columns; i++)
        {
            String name = meta.getColumnName(i);
            int jdbctype = meta.getColumnType(i);

            // Added for H2 debugging
            log.debug("In mocked process(), column '" + name + "' is of SQL Type " + jdbctype);

            switch (jdbctype)
            {
                case Types.BOOLEAN:
                case Types.BIT:
                    row.setColumn(name, results.getBoolean(i));
                    break;

                case Types.INTEGER:
                case Types.NUMERIC:
                    if (isOracle)
                    {
                        long longValue = results.getLong(i);
                        if (longValue <= (long)Integer.MAX_VALUE)
                        {
                            row.setColumn(name, (int) longValue);
                        }
                        else
                        {
                            row.setColumn(name, longValue);
                        }
                    }
                    else
                    {
                        row.setColumn(name, results.getInt(i));
                    }
                    break;

                case Types.DECIMAL:
                    // For H2 database, NUMBER() fields are mapped to DECIMAL data type.
                    // See: http://www.h2database.com/html/datatypes.html#decimal_type
                    // But, our Oracle schema uses NUMBER() to represent INTEGER values 1 or 0
                    // (which are then mapped to Boolean true/false by TableRow.getBooleanColumn()).
                    // So, for H2 to "act like Oracle", we need to try to convert
                    // DECIMAL types to INTEGER values, so they can be mapped to
                    // corresponding Boolean.
                    if (isOracle)
                    {
                        long longValue = results.getLong(i);
                        // If this value actually a valid Integer, convert it to an int
                        if (longValue <= (long)Integer.MAX_VALUE)
                        {
                            row.setColumn(name, (int) longValue);
                        }
                        else // Otherwise, leave it as a long value
                        {
                            row.setColumn(name, longValue);
                        }
                    }
                    else
                    {
                        row.setColumn(name, results.getLong(i));
                    }
                    break;
                    //END ADDED for H2

                case Types.BIGINT:
                    row.setColumn(name, results.getLong(i));
                    break;

                case Types.DOUBLE:
                    row.setColumn(name, results.getDouble(i));
                    break;

                case Types.CLOB:
                    if (isOracle)
                    {
                        row.setColumn(name, results.getString(i));
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
                    }
                    break;

                case Types.VARCHAR:
                    /*try
                    {
                        byte[] bytes = results.getBytes(i);

                        if (bytes != null)
                        {
                            String mystring = new String(results.getBytes(i), "UTF-8");
                            row.setColumn(name, mystring);
                        }
                        else
                        {
                            row.setColumn(name, results.getString(i));
                        }
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        log.error("Unable to parse text from database", e);
                    }*/
                    
                    // H2 assumes that "getBytes()" should return hexidecimal.
                    // So, the above commented out code will throw a JdbcSQLException:
                    // "Hexadecimal string contains non-hex character"
                    // Instead, we just want to get the value as a string.
                    row.setColumn(name, results.getString(i));
                    // END ADDED for H2
                    break;

                case Types.DATE:
                    row.setColumn(name, results.getDate(i));
                    break;

                case Types.TIME:
                    row.setColumn(name, results.getTime(i));
                    break;

                case Types.TIMESTAMP:
                    row.setColumn(name, results.getTimestamp(i));
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
            }

            // Determines if the last column was null, and sets the tablerow accordingly
            if (results.wasNull())
            {
                row.setColumnNull(name);
            }
        }

        // Now that we've prepped the TableRow, reset the flags so that we can detect which columns have changed
        row.resetChanged();
        return row;
    }
    
}
