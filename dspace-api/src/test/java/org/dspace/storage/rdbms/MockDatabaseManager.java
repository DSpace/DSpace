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
import mockit.Invocation;

import mockit.Mock;
import mockit.MockUp;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

import static org.dspace.storage.rdbms.DatabaseManager.getColumnNames;

/**
 * Mocks a DatabaseManager to add some custom logic / queries to support the
 * H2 in-memory database for Unit Testing. By default DSpace does not fully
 * support H2, so this MockDatabaseManager modifies some methods of DatabaseManager
 * which are currently problematic in H2.
 * <P>
 * Any methods which are NOT "mocked" below are just used as-is from the
 * original DatabaseManager class. Also note that, in order to support running
 * Unit Tests on PostgreSQL & Oracle, any H2-specific logic is only executed
 * after verifying your Unit Testing database reports that it is indeed H2.
 *
 * @author pvillega
 * @author tdonohue
 */
public final class MockDatabaseManager
        extends MockUp<DatabaseManager>
{
    // Set our logger to specify the Mock class, so we know which logs are from the "real" vs "mock" class
    private static final Logger log = Logger.getLogger(MockDatabaseManager.class);

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
    public static void setConstraintDeferred(Invocation inv, Context context,
            String constraintName) throws SQLException
    {
        // What type of database is this?
        String databaseType = DatabaseManager.getDbKeyword();

        if(databaseType!=null && !databaseType.equals(DatabaseManager.DBMS_H2))
        {
            // If we are unit testing with a non-H2 database, just proceed to
            // DatabaseManager method of the same name
            inv.proceed(context, constraintName);
        }
        else
        {
            // Otherwise, we'll run slightly customized code in order to support H2
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
    public static void setConstraintImmediate(Invocation inv, Context context,
            String constraintName) throws SQLException
    {
        // What type of database is this?
        String databaseType = DatabaseManager.getDbKeyword();

        if(databaseType!=null && !databaseType.equals(DatabaseManager.DBMS_H2))
        {
            // If we are unit testing with a non-H2 database, just proceed to
            // DatabaseManager method of the same name
            inv.proceed(context, constraintName);
        }
        else
        {
            // Otherwise, we'll run slightly customized code in order to support H2
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
    }

    @Mock
    static TableRow process(Invocation inv, ResultSet results, String table, List<String> pColumnNames) throws SQLException
    {
        return process(inv,null,results,table,pColumnNames);
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
    static TableRow process(Invocation inv, Context context, ResultSet results, String table, List<String> pColumnNames) throws SQLException
    {
        // What type of database is this?
        String databaseType = DatabaseManager.getDbKeyword();
        // Also, is it Oracle-like?
        boolean isOracle = DatabaseManager.isOracle();

        if(databaseType!=null && !databaseType.equals(DatabaseManager.DBMS_H2))
        {
            // If we are unit testing with a non-H2 database, just proceed to
            // DatabaseManager method of the same name
            return inv.proceed(results, table, pColumnNames);
        }
        else
        {
            // Otherwise, we'll run slightly customized code in order to support H2
            log.debug("Mocked process() method for H2 database");

            ResultSetMetaData meta = results.getMetaData();
            int columns = meta.getColumnCount() + 1;

            // If we haven't been passed the column names try to generate them from the metadata / table
            List<String> columnNames = pColumnNames != null ? pColumnNames :
                                            ((table == null) ? getColumnNames(meta) : getColumnNames(context, table));

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

                    case Types.NUMERIC:
                    case Types.DECIMAL:
                        row.setColumn(name, results.getBigDecimal(i));
                        break;

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
}
