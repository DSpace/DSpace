/*
 * DatabaseManager.java
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


package org.dspace.storage.rdbms;

import java.io.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;

import org.apache.log4j.*;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Manages RDBMS
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class DatabaseManager
{
    /**
     * log4j category
     */
    private static Category log = Category.getInstance(DatabaseManager.class);

    private static final String JDBC_URL_PROPERTY = "db.url";

    private static String jdbcUrl = ConfigurationManager.getProperty(JDBC_URL_PROPERTY);
    private static String jdbcUserName = ConfigurationManager.getProperty("db.username");
    private static String jdbcPassword = ConfigurationManager.getProperty("db.password");

    /**
     * True if initialization has been done
     */
    private static boolean initialized = false;

    /**
     * A map of unique ids.
     * The key is the table name; the value is an Integer which is the
     * highest value assigned.
     */
    private static Map ids = new HashMap();

    /**
     * A map of database column information.
     * The key is the table name, a String; the value is
     * an array of ColumnInfo objects.
     */
    private static Map info = new HashMap();

    /**
     * Return an iterator with the results of QUERY.
     * The type of result is given by TABLE.
     *
     * @param context - The context object
     * @param table - The name of the table which results
     * @param query - The SQL query
     * @return - A TableRowIterator with the results of the query
     * @exception SQLException - If a database error occurs
     */
    public static TableRowIterator query(Context context,
                                         String table,
                                         String query)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Running query \"" + query + "\"");

        Statement statement = context.getDBConnection().createStatement();
        return new TableRowIterator(statement.executeQuery(query),
                                    canonicalize(table));
    }

    /**
     * Return the single row result to this query, null if no result.
     * If more than one row results, only the first is returned.
     *
     * @param context - The context object
     * @param table - The name of the table which results
     * @param query - The SQL query
     * @return - A TableRow object, or null if no result
     * @exception SQLException - If a database error occurs
     */
    public static TableRow querySingle( Context context,
                                        String table,
                                        String query )
        throws SQLException
    {
        TableRowIterator iterator = query(context, canonicalize(table), query);
        return (! iterator.hasNext()) ? null : iterator.next();
    }

    /**
     * Perform an update, insert or delete query - returns the number of rows
     * affected.
     *
     * @param context - The context object
     * @param query - The SQL query
     * @return - The number of rows affected.
     * @exception SQLException - If a database error occurs
     */
    public static int updateQuery( Context context, String query )
        throws SQLException
    {
        Statement statement = null;

        if (log.isDebugEnabled())
            log.debug("Running query \"" + query + "\"");

        try
        {
            statement = context.getDBConnection().createStatement();
            return statement.executeUpdate(query);
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle) {}
            }
        }
    }

    /**
     * Create a new row in the given table.  An ID is assigned
     */
    public static TableRow create( Context context, String table )
        throws SQLException
    {
        TableRow row = new TableRow(canonicalize(table),
                                    getNonPrimaryKeyColumnNames(table));
        assignId(row);
        insert(context, row);
        return row;
    }

    /**
     * Find a table row by its primary key
     *
     * @param context - The context object
     * @param table - The table in which to find the row
     * @param id - The primary key value
     * @return - The row resulting from the query, or null
     * @exception SQLException - If a database error occurs
     */
    public static TableRow find( Context context, String table, int id )
        throws SQLException
    {
        String ctable = canonicalize(table);
        return findByUnique(context,
                            ctable,
                            getPrimaryKeyColumn(ctable),
                            Integer.toString(id));
    }

    /**
     * Find a table row by a unique value
     *
     * @param context - The context object
     * @param table - The table to use to find the object
     * @param column - The name of the unique column
     * @param value - The value of the unique column
     * @return - The row resulting from the query, or null
     * @exception SQLException - If a database error occurs
     */
    public static TableRow findByUnique( Context context,
                                         String table,
                                         String column,
                                         String value)
        throws SQLException
    {
        String ctable = canonicalize(table);
        // Need a pair of single quote marks:
        // MessageFormat treats this: '{2}' as the literal {2}
        String sql = MessageFormat.format
            ("select * from {0} where {1} = ''{2}''",
             new Object [] {ctable, column, value }
             );

        return querySingle(context, ctable, sql);
    }

    /**
     * Delete by primary key
     *
     * @param context - The context object
     * @param table - The table to delete from
     * @param id - The primary key value
     * @return - The number of rows deleted
     * @exception SQLException - If a database error occurs
     */
    private static int delete( Context context, String table, int id )
        throws SQLException
    {
        String ctable = canonicalize(table);
        return deleteByValue(context,
                             ctable,
                             getPrimaryKeyColumn(ctable),
                             Integer.toString(id));
    }

    /**
     * Delete a table row by a value (best if it's unique!!)
     *
     * @param context - The context object
     * @param table - The table to delete from
     * @param column - The name of the (unique) column
     * @param value - The value of the (unique) column
     * @return - The number of rows deleted
     * @exception SQLException - If a database error occurs
     */
    public static int deleteByValue( Context context,
                                      String table,
                                      String column,
                                      String value)
        throws SQLException
    {
        String ctable = canonicalize(table);
        // Need a pair of single quote marks:
        // MessageFormat treats this: '{2}' as the literal {2}
        String sql = MessageFormat.format
            ("delete from {0} where {1} = ''{2}''",
             new Object [] {ctable, column, value }
             );

        return updateQuery(context, sql);
    }

    /**
     * Obtain an RDBMS connection
     */
    public static Connection getConnection()
        throws SQLException
    {
        initialize();
        return DriverManager.getConnection(jdbcUrl, jdbcUserName, jdbcPassword);
    }

    /**
     * Release resources associated with this connection.
     */
    public static void freeConnection( Connection c )
    {
        try
        {
            c.close();
        }
        catch (SQLException e) {}
    }

    /**
     * Insert ROW into the RDBMS
     *
     * @param context - The context object
     * @param row - The row to insert
     * @exception SQLException - If a database error occurs
     */
    public static void insert(Context context, TableRow row)
        throws SQLException
    {
        String table = canonicalize(row.getTable());

        StringBuffer sql = new StringBuffer()
            .append("insert into ")
            .append(table)
            .append(" ( ");

        ColumnInfo[] info = getColumnInfo(table);

        for (int i = 0; i < info.length; i++ )
        {
            sql.append(i == 0 ? "" : ", ").append(info[i].getName());
        }

        sql.append(") values ( ");

        // Values to insert
        for (int i = 0; i < info.length; i++ )
        {
            sql.append(i == 0 ? "" : ", ").append("?");
        }

        // Watch the syntax
        sql.append(")");

        execute(context.getDBConnection(),
                sql.toString(),
                Arrays.asList(info),
                row);
    }

    /**
     * Update changes to the RDBMS
     *
     * @param context - The context object
     * @param row - The row to update
     * @return - The number of rows affected (1 or 0)
     * @exception SQLException - If a database error occurs
     */
    public static void update(Context context, TableRow row)
        throws SQLException
    {
        String table = canonicalize(row.getTable());

        StringBuffer sql = new StringBuffer()
            .append("update ")
            .append(table)
            .append(" set ");

        ColumnInfo pk     = getPrimaryKeyColumnInfo(table);
        ColumnInfo[] info = getNonPrimaryKeyColumns(table);

        for (int i = 0; i < info.length; i++ )
        {
            sql.append(i == 0 ? "" : ", ")
                .append(info[i].getName())
                .append(" = ?");
        }

        sql.append(" where ")
            .append(pk.getName())
            .append(" = ?");

        List columns = new ArrayList();
        columns.addAll(Arrays.asList(info));
        columns.add(pk);

        execute(context.getDBConnection(),
                sql.toString(),
                columns,
                row);
    }

    /**
     * Delete ROW from the RDBMS
     *
     * @param context - The context object
     * @param row - The row to delete
     * @return - The number of rows affected (1 or 0)
     * @exception SQLException - If a database error occurs
     */
    public static void delete(Context context, TableRow row)
        throws SQLException
    {
        String pk = getPrimaryKeyColumn(row);
        if (row.isColumnNull(pk))
            throw new IllegalArgumentException("Primary key value is null");

        delete(context, row.getTable(), row.getIntColumn(pk));
    }

    /**
     * Return metadata about a table
     *
     * @param context - The context object
     * @param table - The name of the table
     * @return - An array of ColumnInfo objects
     * @exception SQLException - If a database error occurs
     */
    static ColumnInfo[] getColumnInfo( String table )
        throws SQLException
    {
        Map cinfo = getColumnInfoInternal(table);
        if (cinfo == null)
            return null;

        Collection info = cinfo.values();
        return (ColumnInfo[]) info.toArray(new ColumnInfo[info.size()]);
    }

    /**
     * Return info about COLUMN in TABLE.
     */
    static ColumnInfo getColumnInfo( String table, String column)
        throws SQLException
    {
        Map info = getColumnInfoInternal(table);
        return info == null ? null : (ColumnInfo) info.get(column);
    }

    /**
     * Return all the columns which are not primary keys
     *
     * @param context - The context object
     * @param table - The name of the table
     * @return - An array of ColumnInfo objects
     * @exception SQLException - If a database error occurs
     */
    static ColumnInfo[] getNonPrimaryKeyColumns ( String table )
        throws SQLException
    {
        String pk = getPrimaryKeyColumn(table);
        ColumnInfo[] info = getColumnInfo(table);
        ColumnInfo[] results = new ColumnInfo[info.length - 1];
        int rcount = 0;
        for (int i = 0; i < info.length; i++ )
        {
            if (! pk.equals(info[i].getName()))
                results[rcount++] = info[i];
        }

        return results;
    }

    /**
     * Return a list of all the columns which are not primary keys
     */
    protected static List getNonPrimaryKeyColumnNames ( String table )
        throws SQLException
    {
        List results = new ArrayList();
        ColumnInfo[] info = getColumnInfo(table);
        for (int i = 0; i < info.length; i++ )
        {
            results.add(info[i].getName());
        }

        return results;
    }

    /**
     * Return the canonical name for TABLE.
     *
     * @param table - The name of the table.
     * @return - The canonical name of the table.
     */
    static String canonicalize(String table)
    {
        return table.toLowerCase();
    }

    ////////////////////////////////////////
    // SQL loading methods
    ////////////////////////////////////////

    /**
     * Load SQL into the RDBMS.
     */
    public static void loadSql(String sql)
        throws SQLException
    {
        try
        {
            loadSql(new StringReader(sql));
        }
        catch (IOException ioe)
        {
        }
    }

    /**
     * Load SQL from READER into the RDBMS.
     */
    public static void loadSql(Reader r)
        throws SQLException, IOException
    {
        BufferedReader reader = new BufferedReader(r);
        StringBuffer sql = new StringBuffer();
        String SQL = null;

        String line = null;

        Statement statement = getConnection().createStatement();
        boolean inquote = false;

        while ((line = reader.readLine()) != null)
        {
            // Look for comments
            int commentStart = line.indexOf("--");

            String input = (commentStart != -1) ?
                line.substring(0, commentStart) : line;
            // Empty line, skip
            if (input.trim().equals(""))
                continue;
            // Put it on the SQL buffer
            sql.append(input);
            // Add a space
            sql.append(" ");
            // More to come?

            // Look for quotes
            int index = 0;
            int count = 0;
            int inputlen = input.length();

            while ((index = input.indexOf("'", count)) != -1)
            {
                // Flip the value of inquote
                inquote = !inquote;

                // Move the index
                count = index + 1;
                // Make sure we do not exceed the string length
                if (count >= inputlen)
                    break;
            }

            // If we are in a quote, keep going
            // Note that this is STILL a simple heuristic that is not
            // guaranteed to be correct
            if (inquote)
                continue;

            int endMarker = input.indexOf(";", index);

            if (endMarker == -1)
                continue;

            if (log.isInfoEnabled())
                log.info("Running database query \"" + sql + "\"");

            SQL = sql.toString();

            try
            {
                // Use execute, not executeQuery (which expects results) or
                // executeUpdate
                boolean succeeded = statement.execute(SQL);
            }
            catch (SQLWarning sqlw)
            {
                if (log.isInfoEnabled())
                    log.info("Got SQL Warning: " + sqlw, sqlw);
            }
            catch (SQLException sqle)
            {
                String msg = "Got SQL Exception: " + sqle;
                String sqlmessage = sqle.getMessage();

                // These are Postgres-isms:

                // There's no easy way to check if a table exists before
                // creating it, so we always drop tables, then create them
                boolean isDrop =
                    ((SQL != null) &&
                        (sqlmessage != null) &&
                        (SQL.toUpperCase().startsWith("DROP")) &&
                        (sqlmessage.indexOf("does not exist") != -1));

                // Creating a view causes a bogus warning
                boolean isNoResults =
                    ((SQL != null) &&
                        (sqlmessage != null) &&
                        ((SQL.toUpperCase().startsWith("CREATE VIEW")) ||
                         ((SQL.toUpperCase().startsWith("CREATE FUNCTION")))) &&
                     (sqlmessage.indexOf("No results were returned") != -1));

                // If the messages are bogus, give them a low priority
                if (isDrop || isNoResults)
                {
                    if (log.isDebugEnabled())
                        log.debug(msg, sqle);
                }
                // Otherwise, we need to know!
                else
                {
                    if (log.isEnabledFor(Priority.WARN))
                        log.warn(msg, sqle);
                }
            }
            // Reset SQL buffer
            sql = new StringBuffer();
            SQL = null;
        }

        statement.close();
    }

    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////

    /**
     * Convert the current row from RESULTS into a TableRow object.
     */
    static TableRow process (ResultSet results, String table)
        throws SQLException
    {
        TableRow row = new TableRow(canonicalize(table),
                                    getNonPrimaryKeyColumnNames(table));

        ResultSetMetaData meta = results.getMetaData();
        int columns = meta.getColumnCount() + 1;

        // Process the columns in order
        // (This ensures maximum backwards compatibility with
        // old JDBC drivers)
        for (int i = 1; i < columns; i++)
        {
            String name = meta.getColumnName(i);
            int jdbctype = meta.getColumnType(i);

            if (jdbctype == Types.BIT)
            {
                row.setColumn(name, results.getBoolean(i));
            }
            else if (jdbctype == Types.INTEGER)
            {
                row.setColumn(name, results.getInt(i));
            }
            else if (jdbctype == Types.VARCHAR)
            {
                row.setColumn(name, results.getString(i));
            }
            else if (jdbctype == Types.DATE)
            {
                row.setColumn(name, results.getDate(i));
            }
            else if (jdbctype == Types.TIME)
            {
                row.setColumn(name, results.getTime(i));
            }
            else if (jdbctype == Types.TIMESTAMP)
            {
                row.setColumn(name, results.getTimestamp(i));
            }
            else
            {
                throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
            }

            if (results.wasNull())
            {
                row.setColumnNull(name);
            }

        }

        return row;
    }

    /**
     * Return the name of the primary key column.
     */
    static String getPrimaryKeyColumn(TableRow row)
        throws SQLException
    {
        return getPrimaryKeyColumn(row.getTable());
    }

    /**
     * Return the name of the primary key column.
     * We assume there's only one!
     */
    protected static String getPrimaryKeyColumn(String table)
        throws SQLException
    {
        ColumnInfo info = getPrimaryKeyColumnInfo(table);
        return info == null ? null : info.getName();
    }

    /**
     * Return the name of the primary key column.
     * We assume there's only one!
     */
    static ColumnInfo getPrimaryKeyColumnInfo(String table)
        throws SQLException
    {
        ColumnInfo[] cinfo = getColumnInfo(canonicalize(table));
        for (int i = 0; i < cinfo.length; i++ )
        {
            ColumnInfo info = cinfo[i];
            if (info.isPrimaryKey())
                return info;
        }

        return null;
    }

    /**
     * Assign an ID to row
     */
    private static synchronized void assignId (TableRow row)
        throws SQLException
    {
        String table = canonicalize(row.getTable());
        String pk = getPrimaryKeyColumn(table);
        Integer id = (Integer) ids.get(table);
        int current_id = id == null ? -1 : id.intValue();

        if (id == null)
        {
            String sql = MessageFormat.format
                ("select max({0}) from {1}",
                 new Object[] { pk, table} );

            Statement statement = null;
            try
            {
                statement = getConnection().createStatement();
                ResultSet results = statement.executeQuery(sql);
                current_id = results.next() ? results.getInt(1): -1;
            }
            finally
            {
                if (statement != null)
                {
                    try
                    {
                        statement.close();
                    }
                    catch (SQLException sqle) {}
                }
            }
        }

        int new_id = current_id + 1;
        row.setColumn(pk, new_id);
        ids.put(table, new Integer(new_id));
    }

    /**
     * Execute SQL as a PreparedStatement.
     * Bind parameters in COLUMNS first.
     *
     * @param connection - The SQL connection
     * @param sql - The query to execute
     * @param columns - The columns to bind
     * @param row - The row
     * @return - The number of rows affected
     * @exception SQLException - If a database error occurs
     */
    private static int execute(Connection connection,
                               String sql,
                               List columns,
                               TableRow row)
        throws SQLException
    {
        PreparedStatement statement = null;

        if (log.isDebugEnabled())
            log.debug("Running query \"" + sql + "\"");

        try
        {
            statement = connection.prepareStatement(sql);

            int count = 0;
            for (Iterator iterator = columns.iterator(); iterator.hasNext(); )
            {
                count++;
                ColumnInfo info = (ColumnInfo) iterator.next();
                String column   = info.getName();
                int jdbctype    = info.getType();

                if (row.isColumnNull(column))
                {
                    statement.setNull(count, jdbctype);
                    continue;
                }
                else if (jdbctype == Types.BIT)
                {
                    statement.setBoolean(count, row.getBooleanColumn(column));
                    continue;
                }
                else if (jdbctype == Types.INTEGER)
                {
                    statement.setInt(count, row.getIntColumn(column));
                    continue;
                }
                else if (jdbctype == Types.VARCHAR)
                {
                    statement.setString(count, row.getStringColumn(column));
                    continue;
                }
                else if (jdbctype == Types.DATE)
                {
                    java.sql.Date d = new java.sql.Date(row.getDateColumn(column).getTime());
                    statement.setDate(count, d);
                    continue;
                }
                else if (jdbctype == Types.TIME)
                {
                    Time t = new Time(row.getDateColumn(column).getTime());
                    statement.setTime(count, t);
                    continue;
                }
                else if (jdbctype == Types.TIMESTAMP)
                {
                    Timestamp t = new Timestamp(row.getDateColumn(column).getTime());
                    statement.setTimestamp(count, t);
                    continue;
                }
                else
                {
                    throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
                }
            }

            return statement.executeUpdate();
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle) {}
            }
        }
    }

    /**
     * Return metadata about a table
     *
     * @param table - The name of the table
     * @return - An map of info
     * @exception SQLException - If a database error occurs
     */
    private static Map getColumnInfoInternal( String table )
        throws SQLException
    {
        String ctable = canonicalize(table);
        Map results = (Map) info.get(ctable);

        if (results != null)
            return results;

        results = retrieveColumnInfo(ctable);
        info.put(ctable, results);
        return results;
    }

    /**
     * Read metadata about a table from the database
     */
    private static Map retrieveColumnInfo ( String table )
        throws SQLException
    {
        DatabaseMetaData metadata = getConnection().getMetaData();
        HashMap results = new HashMap();

        // Find all the primary keys
        ResultSet pkcolumns = metadata.getPrimaryKeys(null, null, table);
        Set pks = new HashSet();
        while (pkcolumns.next())
            pks.add(pkcolumns.getString(4));

        // Then all the column info
        ResultSet columns = metadata.getColumns(null, null, table, null);
        while (columns.next())
        {
            String column = columns.getString(4);
            ColumnInfo cinfo = new ColumnInfo();
            cinfo.setName(column);
            cinfo.setType((int) columns.getShort(5));
            if (pks.contains(column))
                cinfo.setIsPrimaryKey(true);
            results.put(column, cinfo);
        }

        return results;
    }

    /**
     * Initialize the DatabaseManager
     */
    private static void initialize()
        throws SQLException
    {
        if (initialized)
            return;

        if (jdbcUrl == null)
            throw new IllegalStateException("Configuration property \"" + JDBC_URL_PROPERTY + "\" not found");

        DriverManager.registerDriver(new SimplePool());
        initialized = true;
    }
}
