/*
 * DatabaseManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Executes SQL queries.
 * 
 * @author Peter Breton
 * @author Jim Downing
 * @version $Revision$
 */
public class DatabaseManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DatabaseManager.class);

    /** True if initialization has been done */
    private static boolean initialized = false;

    /** Name to use for the pool */
    private static String poolName = "dspacepool";
    
    /** 
     * This regular expression is used to perform sanity checks 
     * on database names (i.e. tables and columns). 
     * 
     * FIXME: Regular expressions can be slow to solve this in the future we should
     * probably create a system where we don't pass in column and table names to these low
     * level database methods. This approach is highly exploitable for injection 
     * type attacks because we are unable to determine where the input came from. Instead
     * we could pass in static integer constants which are then mapped to their sql name. 
     */
    private static final Pattern DB_SAFE_NAME = Pattern.compile("^[a-zA-Z_1-9]+$");

    /**
     * A map of database column information. The key is the table name, a
     * String; the value is an array of ColumnInfo objects.
     */
    private static Map info = new HashMap();

    /**
     * Protected Constructor to prevent instantiation except by derived classes.
     */
    protected DatabaseManager()
    {
    }
    
    /**
     * Return an iterator with the results of the query. The table parameter
     * indicates the type of result. If table is null, the column names are read
     * from the ResultSetMetaData.
     * 
     * @param context
     *            The context object
     * @param table
     *            The name of the table which results
     * @param query
     *            The SQL query
     * @param parameters
     * 			  A set of SQL parameters to be included in query. The order of 
     * 			  the parameters must correspond to the order of their reference  
     * 			  within the query.
     * @return A TableRowIterator with the results of the query
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRowIterator queryTable(Context context, String table,
            String query, Object... parameters ) throws SQLException
    {
        if (log.isDebugEnabled())
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug("Running query \"" + query + "\"  with parameters: " + sb.toString());
        }
        
        PreparedStatement statement = context.getDBConnection().prepareStatement(query);
        loadParameters(statement,parameters);
        
        TableRowIterator retTRI = new TableRowIterator(statement.executeQuery(),
                canonicalize(table));

        retTRI.setStatement(statement);
        return retTRI;
    }
    
    /**
     * Return an iterator with the results of the query.
     * 
     * @param context
     *            The context object
     * @param query
     *            The SQL query
     * @param parameters
     * 			  A set of SQL parameters to be included in query. The order of 
     * 			  the parameters must correspond to the order of their reference 
     * 			  within the query.
     * @return A TableRowIterator with the results of the query
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRowIterator query(Context context, String query,
            Object... parameters) throws SQLException    
    {
        if (log.isDebugEnabled())
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug("Running query \"" + query + "\"  with parameters: " + sb.toString());
        }

        PreparedStatement statement = context.getDBConnection().prepareStatement(query);
        loadParameters(statement,parameters);
        
        TableRowIterator retTRI = new TableRowIterator(statement.executeQuery());

        
        retTRI.setStatement(statement);
        return retTRI;
    }
    
    /**
     * Return an iterator with the results of executing statement. The table
     * parameter indicates the type of result. If table is null, the column
     * names are read from the ResultSetMetaData. The context is that of the
     * connection which was used to create the statement.
     * 
     * @param statement
     *            The prepared statement to execute.
     * @param table
     *            The name of the table which results
     * @return A TableRowIterator with the results of the query
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRowIterator queryPreparedTable(String table,
            PreparedStatement statement) throws SQLException
    {
        TableRowIterator retTRI = new TableRowIterator(statement.executeQuery(),
                canonicalize(table));

        retTRI.setStatement(statement);
        return retTRI;
    }

    /**
     * Return an iterator with the results of executing statement. The context
     * is that of the connection which was used to create the statement.
     * 
     * @param statement
     *            The prepared statement to execute.
     * @return A TableRowIterator with the results of the query
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRowIterator queryPrepared(PreparedStatement statement)
            throws SQLException
    {
        TableRowIterator retTRI = new TableRowIterator(statement.executeQuery());

        retTRI.setStatement(statement);
        return retTRI;
    }

    /**
     * Return the single row result to this query, or null if no result. If more
     * than one row results, only the first is returned.
     * 
     * @param context
     *            Current DSpace context
     * @param query
     *            The SQL query
     * @param parameters
     * 			  A set of SQL parameters to be included in query. The order of 
     * 			  the parameters must correspond to the order of their reference 
     * 			  within the query.

     * @return A TableRow object, or null if no result
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRow querySingle(Context context, String query,
            Object... parameters) throws SQLException
    {
        TableRowIterator iterator = query(context, query, parameters);

        TableRow retRow = (!iterator.hasNext()) ? null : iterator.next();
        iterator.close();
        return (retRow);
    }
    
    /**
     * Return the single row result to this query, or null if no result. If more
     * than one row results, only the first is returned.
     * 
     * @param context
     *            Current DSpace context
     * @param table
     *            The name of the table which results
     * @param query
     *            The SQL query
     * @param parameters
     * 			  A set of SQL parameters to be included in query. The order of 
     * 			  the parameters must correspond to the order of their reference 
     * 			  within the query.
     * @return A TableRow object, or null if no result
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRow querySingleTable(Context context, String table,
            String query, Object... parameters) throws SQLException
    {
        TableRowIterator iterator = queryTable(context, canonicalize(table), query, parameters);

        TableRow retRow = (!iterator.hasNext()) ? null : iterator.next();
        iterator.close();
        return (retRow);
    }
    
    /**
     * Execute an update, insert or delete query. Returns the number of rows
     * affected by the query.
     * 
     * @param context
     *            Current DSpace context
     * @param query
     *            The SQL query to execute
     * @param parameters
     * 			  A set of SQL parameters to be included in query. The order of 
     * 			  the parameters must correspond to the order of their reference 
     * 			  within the query.
     * @return The number of rows affected by the query.
     * @exception SQLException
     *                If a database error occurs
     */
    public static int updateQuery(Context context, String query,
            Object... parameters) throws SQLException
    {
        PreparedStatement statement = null;

        if (log.isDebugEnabled())
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug("Running query \"" + query + "\"  with parameters: " + sb.toString());
        }

        try
        {        	
        	statement = context.getDBConnection().prepareStatement(query);
        	loadParameters(statement,parameters);
        	
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
                catch (SQLException sqle)
                {
                }
            }
        }
    }
    
    /**
     * Create a new row in the given table, and assigns a unique id.
     * 
     * @param context
     *            Current DSpace context
     * @param table
     *            The RDBMS table in which to create the new row
     * @return The newly created row
     */
    public static TableRow create(Context context, String table)
            throws SQLException
    {
        TableRow row = new TableRow(canonicalize(table), getColumnNames(table));
        insert(context, row);

        return row;
    }

    /**
     * Find a table row by its primary key. Returns the row, or null if no row
     * with that primary key value exists.
     * 
     * @param context
     *            Current DSpace context
     * @param table
     *            The table in which to find the row
     * @param id
     *            The primary key value
     * @return The row resulting from the query, or null if no row with that
     *         primary key value exists.
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRow find(Context context, String table, int id)
            throws SQLException
    {
        String ctable = canonicalize(table);

        return findByUnique(context, ctable, getPrimaryKeyColumn(ctable),
                new Integer(id));
    }

    /**
     * Find a table row by a unique value. Returns the row, or null if no row
     * with that primary key value exists. If multiple rows with the value
     * exist, one is returned.
     * 
     * @param context
     *            Current DSpace context
     * @param table
     *            The table to use to find the object
     * @param column
     *            The name of the unique column
     * @param value
     *            The value of the unique column
     * @return The row resulting from the query, or null if no row with that
     *         value exists.
     * @exception SQLException
     *                If a database error occurs
     */
    public static TableRow findByUnique(Context context, String table,
            String column, Object value) throws SQLException
    {
        String ctable = canonicalize(table);

        if ( ! DB_SAFE_NAME.matcher(ctable).matches())
        	throw new SQLException("Unable to execute select query because table name ("+ctable+") contains non alphanumeric characters.");

        if ( ! DB_SAFE_NAME.matcher(column).matches())
        	throw new SQLException("Unable to execute select query because column name ("+column+") contains non alphanumeric characters.");
        
        String sql = "select * from " + ctable + " where "+ column +" = ? ";

        return querySingleTable(context, ctable, sql, value);
    }

    /**
     * Delete a table row via its primary key. Returns the number of rows
     * deleted.
     * 
     * @param context
     *            Current DSpace context
     * @param table
     *            The table to delete from
     * @param id
     *            The primary key value
     * @return The number of rows deleted
     * @exception SQLException
     *                If a database error occurs
     */
    public static int delete(Context context, String table, int id)
            throws SQLException
    {
        String ctable = canonicalize(table);

        return deleteByValue(context, ctable, getPrimaryKeyColumn(ctable),
                new Integer(id));
    }

    /**
     * Delete all table rows with the given value. Returns the number of rows
     * deleted.
     * 
     * @param context
     *            Current DSpace context
     * @param table
     *            The table to delete from
     * @param column
     *            The name of the column
     * @param value
     *            The value of the column
     * @return The number of rows deleted
     * @exception SQLException
     *                If a database error occurs
     */
    public static int deleteByValue(Context context, String table,
            String column, Object value) throws SQLException
    {
        String ctable = canonicalize(table);

        if ( ! DB_SAFE_NAME.matcher(ctable).matches())
        	throw new SQLException("Unable to execute delete query because table name ("+ctable+") contains non alphanumeric characters.");

        if ( ! DB_SAFE_NAME.matcher(column).matches())
        	throw new SQLException("Unable to execute delete query because column name ("+column+") contains non alphanumeric characters.");
        
        String sql = "delete from "+ctable+" where "+column+" = ? ";

        return updateQuery(context, sql, value);
    }

    /**
     * Obtain an RDBMS connection.
     * 
     * @return A new database connection.
     * @exception SQLException
     *                If a database error occurs, or a connection cannot be
     *                obtained.
     */
    public static Connection getConnection() throws SQLException
    {
        initialize();

        return DriverManager
                .getConnection("jdbc:apache:commons:dbcp:" + poolName);
    }

    /**
     * Release resources associated with this connection.
     * 
     * @param c
     *            The connection to release
     */
    public static void freeConnection(Connection c)
    {
        try
        {
            if (c != null)
            {
                c.close();
            }
        }
        catch (SQLException e)
        {
            log.warn(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a table row object that can be passed into the insert method, not
     * commonly used unless the table has a referential integrity constraint.
     * 
     * @param table
     *            The RDBMS table in which to create the new row
     * @return The newly created row
     * @throws SQLException
     */
    public static TableRow row(String table) throws SQLException
    {
        return new TableRow(canonicalize(table), getColumnNames(table));
    }
    
    /**
     * Insert a table row into the RDBMS.
     * 
     * @param context
     *            Current DSpace context
     * @param row
     *            The row to insert
     * @exception SQLException
     *                If a database error occurs
     */
    public static void insert(Context context, TableRow row)
            throws SQLException
    {
        String table = canonicalize(row.getTable());

        // Get an ID (primary key) for this row by using the "getnextid"
        // SQL function in Postgres, or directly with sequences in Oracle
        String myQuery = "SELECT getnextid('" + table + "') AS result";

        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            myQuery = "SELECT " + table + "_seq" + ".nextval FROM dual";
        }

        Statement statement = context.getDBConnection().createStatement();
        ResultSet rs = statement.executeQuery(myQuery);

        rs.next();

        int newID = rs.getInt(1);
        rs.close();

        statement.close();

        // Set the ID in the table row object
        row.setColumn(getPrimaryKeyColumn(table), newID);

        StringBuffer sql = new StringBuffer().append("INSERT INTO ").append(
                table).append(" ( ");

        ColumnInfo[] info = getColumnInfo(table);

        for (int i = 0; i < info.length; i++)
        {
            sql.append((i == 0) ? "" : ",").append(info[i].getName());
        }

        sql.append(") VALUES ( ");

        // Values to insert
        for (int i = 0; i < info.length; i++)
        {
            sql.append((i == 0) ? "" : ",").append("?");
        }

        // Watch the syntax
        sql.append(")");

        execute(context.getDBConnection(), sql.toString(), Arrays.asList(info),
                row);
    }

    /**
     * Update changes to the RDBMS. Note that if the update fails, the values in
     * the row will NOT be reverted.
     * 
     * @param context
     *            Current DSpace context
     * @param row
     *            The row to update
     * @return The number of rows affected (1 or 0)
     * @exception SQLException
     *                If a database error occurs
     */
    public static int update(Context context, TableRow row) throws SQLException
    {
        String table = canonicalize(row.getTable());

        StringBuffer sql = new StringBuffer().append("update ").append(table)
                .append(" set ");

        List columns = new ArrayList();
        ColumnInfo pk = getPrimaryKeyColumnInfo(table);
        ColumnInfo[] info = getNonPrimaryKeyColumns(table);

        String seperator = "";
        for (int i = 0; i < info.length; i++)
        {
            // Only update this column if it has changed
            if (row.hasColumnChanged(info[i].getName()))
            {
                sql.append(seperator).append(info[i].getName()).append(" = ?");
                columns.add(info[i]);
                seperator = ", ";
            }
        }

        // Only execute the update if there is anything to update
        if (columns.size() > 0)
        {
            sql.append(" where ").append(pk.getName()).append(" = ?");
            columns.add(pk);

            return execute(context.getDBConnection(), sql.toString(), columns, row);
        }

        return 1;
    }

    /**
     * Delete row from the RDBMS.
     * 
     * @param context
     *            Current DSpace context
     * @param row
     *            The row to delete
     * @return The number of rows affected (1 or 0)
     * @exception SQLException
     *                If a database error occurs
     */
    public static int delete(Context context, TableRow row) throws SQLException
    {
        String pk = getPrimaryKeyColumn(row);

        if (row.isColumnNull(pk))
        {
            throw new IllegalArgumentException("Primary key value is null");
        }

        return delete(context, row.getTable(), row.getIntColumn(pk));
    }

    /**
     * Return metadata about a table.
     * 
     * @param table
     *            The name of the table
     * @return An array of ColumnInfo objects
     * @exception SQLException
     *                If a database error occurs
     */
    static ColumnInfo[] getColumnInfo(String table) throws SQLException
    {
        Map cinfo = getColumnInfoInternal(table);

        if (cinfo == null)
        {
            return null;
        }

        Collection info = cinfo.values();

        return (ColumnInfo[]) info.toArray(new ColumnInfo[info.size()]);
    }

    /**
     * Return info about column in table.
     * 
     * @param table
     *            The name of the table
     * @param column
     *            The name of the column
     * @return Information about the column
     * @exception SQLException
     *                If a database error occurs
     */
    static ColumnInfo getColumnInfo(String table, String column)
            throws SQLException
    {
        Map info = getColumnInfoInternal(table);

        return (info == null) ? null : (ColumnInfo) info.get(column);
    }

    /**
     * Return all the columns which are not primary keys.
     * 
     * @param table
     *            The name of the table
     * @return All the columns which are not primary keys, as an array of
     *         ColumnInfo objects
     * @exception SQLException
     *                If a database error occurs
     */
    static ColumnInfo[] getNonPrimaryKeyColumns(String table)
            throws SQLException
    {
        String pk = getPrimaryKeyColumn(table);
        ColumnInfo[] info = getColumnInfo(table);
        ColumnInfo[] results = new ColumnInfo[info.length - 1];
        int rcount = 0;

        for (int i = 0; i < info.length; i++)
        {
            if (!pk.equals(info[i].getName()))
            {
                results[rcount++] = info[i];
            }
        }

        return results;
    }

    /**
     * Return the names of all the columns of the given table.
     * 
     * @param table
     *            The name of the table
     * @return The names of all the columns of the given table, as a List. Each
     *         element of the list is a String.
     * @exception SQLException
     *                If a database error occurs
     */
    protected static List getColumnNames(String table) throws SQLException
    {
        List results = new ArrayList();
        ColumnInfo[] info = getColumnInfo(table);

        for (int i = 0; i < info.length; i++)
        {
            results.add(info[i].getName());
        }

        return results;
    }

    /**
     * Return the names of all the columns of the ResultSet.
     * 
     * @param meta
     *            The ResultSetMetaData
     * @return The names of all the columns of the given table, as a List. Each
     *         element of the list is a String.
     * @exception SQLException
     *                If a database error occurs
     */
    protected static List getColumnNames(ResultSetMetaData meta)
            throws SQLException
    {
        List results = new ArrayList();
        int columns = meta.getColumnCount();

        for (int i = 0; i < columns; i++)
        {
            results.add(meta.getColumnLabel(i + 1));
        }

        return results;
    }

    /**
     * Return the canonical name for a table.
     * 
     * @param table
     *            The name of the table.
     * @return The canonical name of the table.
     */
    static String canonicalize(String table)
    {
        // Oracle expects upper-case table names
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            return (table == null) ? null : table.toUpperCase();
        }

        // default database postgres wants lower-case table names
        return (table == null) ? null : table.toLowerCase();
    }

    ////////////////////////////////////////
    // SQL loading methods
    ////////////////////////////////////////

    /**
     * Load SQL into the RDBMS.
     * 
     * @param sql
     *            The SQL to load.
     * throws SQLException
     *            If a database error occurs
     */
    public static void loadSql(String sql) throws SQLException
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
     * Load SQL from a reader into the RDBMS.
     * 
     * @param r
     *            The Reader from which to read the SQL.
     * @throws SQLException
     *            If a database error occurs
     * @throws IOException
     *            If an error occurs obtaining data from the reader
     */
    public static void loadSql(Reader r) throws SQLException, IOException
    {
        BufferedReader reader = new BufferedReader(r);
        StringBuffer sql = new StringBuffer();
        String SQL = null;

        String line = null;

        Connection connection = null;
        Statement statement = null;

        try
        {
            connection = getConnection();
            connection.setAutoCommit(true);
            statement = connection.createStatement();

            boolean inquote = false;

            while ((line = reader.readLine()) != null)
            {
                // Look for comments
                int commentStart = line.indexOf("--");

                String input = (commentStart != -1) ? line.substring(0,
                        commentStart) : line;

                // Empty line, skip
                if (input.trim().equals(""))
                {
                    continue;
                }

                // Put it on the SQL buffer
                sql.append(input.replace(';', ' ')); // remove all semicolons
                                                     // from sql file!

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
                    {
                        break;
                    }
                }

                // If we are in a quote, keep going
                // Note that this is STILL a simple heuristic that is not
                // guaranteed to be correct
                if (inquote)
                {
                    continue;
                }

                int endMarker = input.indexOf(";", index);

                if (endMarker == -1)
                {
                    continue;
                }

                if (log.isDebugEnabled())
                {
                    log.debug("Running database query \"" + sql + "\"");
                }

                SQL = sql.toString();

                try
                {
                    // Use execute, not executeQuery (which expects results) or
                    // executeUpdate
                    boolean succeeded = statement.execute(SQL);
                }
                catch (SQLWarning sqlw)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Got SQL Warning: " + sqlw, sqlw);
                    }
                }
                catch (SQLException sqle)
                {
                    String msg = "Got SQL Exception: " + sqle;
                    String sqlmessage = sqle.getMessage();

                    // These are Postgres-isms:
                    // There's no easy way to check if a table exists before
                    // creating it, so we always drop tables, then create them
                    boolean isDrop = ((SQL != null) && (sqlmessage != null)
                            && (SQL.toUpperCase().startsWith("DROP")) && (sqlmessage
                            .indexOf("does not exist") != -1));

                    // Creating a view causes a bogus warning
                    boolean isNoResults = ((SQL != null)
                            && (sqlmessage != null)
                            && ((SQL.toUpperCase().startsWith("CREATE VIEW")) || (SQL
                                    .toUpperCase()
                                    .startsWith("CREATE FUNCTION"))) && (sqlmessage
                            .indexOf("No results were returned") != -1));

                    // If the messages are bogus, give them a low priority
                    if (isDrop || isNoResults)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(msg, sqle);
                        }
                    }
                    // Otherwise, we need to know!
                    else
                    {
                        if (log.isEnabledFor(Level.WARN))
                        {
                            log.warn(msg, sqle);
                        }
                    }
                }

                // Reset SQL buffer
                sql = new StringBuffer();
                SQL = null;
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }

            if (statement != null)
            {
                statement.close();
            }
        }
    }

    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////

    /**
     * Convert the current row in a ResultSet into a TableRow object.
     * 
     * @param results
     *            A ResultSet to process
     * @param table
     *            The name of the table
     * @return A TableRow object with the data from the ResultSet
     * @exception SQLException
     *                If a database error occurs
     */
    static TableRow process(ResultSet results, String table)
            throws SQLException
    {
        String dbName =ConfigurationManager.getProperty("db.name");
        ResultSetMetaData meta = results.getMetaData();
        int columns = meta.getColumnCount() + 1;

        List columnNames = (table == null) ? getColumnNames(meta)
                : getColumnNames(table);

        TableRow row = new TableRow(canonicalize(table), columnNames);

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
            else if ((jdbctype == Types.INTEGER) || (jdbctype == Types.NUMERIC)
                    || (jdbctype == Types.DECIMAL))
            {
                // If we are using oracle
                if ("oracle".equals(dbName))
                {
                    // Test the value from the record set. If it can be represented using an int, do so.
                    // Otherwise, store it as long
                    long longValue = results.getLong(i);
                    if (longValue <= (long)Integer.MAX_VALUE)
                        row.setColumn(name, (int)longValue);
                    else
                        row.setColumn(name, longValue);
                }
                else
                    row.setColumn(name, results.getInt(i));
            }
            else if (jdbctype == Types.BIGINT)
            {
                row.setColumn(name, results.getLong(i));
            }
            else if (jdbctype == Types.CLOB && "oracle".equals(dbName))
            {
                // Support CLOBs in place of TEXT columns in Oracle
                row.setColumn(name, results.getString(i));
            }
            else if (jdbctype == Types.VARCHAR)
            {
                try
                {
                    byte[] bytes = results.getBytes(i);

                    if (bytes != null)
                    {
                        String mystring = new String(results.getBytes(i),
                                "UTF-8");
                        row.setColumn(name, mystring);
                    }
                    else
                    {
                        row.setColumn(name, results.getString(i));
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    // do nothing, UTF-8 is built in!
                }
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
                throw new IllegalArgumentException("Unsupported JDBC type: "
                        + jdbctype);
            }

            if (results.wasNull())
            {
                row.setColumnNull(name);
            }
        }

        // Now that we've prepped the TableRow, reset the flags so that we can detect which columns have changed
        row.resetChanged();
        return row;
    }

    /**
     * Return the name of the primary key column. We assume there's only one
     * primary key per table; if there are more, only the first one will be
     * returned.
     * 
     * @param row
     *            The TableRow to return the primary key for.
     * @return The name of the primary key column, or null if the row has no
     *         primary key.
     * @exception SQLException
     *                If a database error occurs
     */
    public static String getPrimaryKeyColumn(TableRow row) throws SQLException
    {
        return getPrimaryKeyColumn(row.getTable());
    }

    /**
     * Return the name of the primary key column in the given table. We assume
     * there's only one primary key per table; if there are more, only the first
     * one will be returned.
     * 
     * @param table
     *            The name of the RDBMS table
     * @return The name of the primary key column, or null if the table has no
     *         primary key.
     * @exception SQLException
     *                If a database error occurs
     */
    protected static String getPrimaryKeyColumn(String table)
            throws SQLException
    {
        ColumnInfo info = getPrimaryKeyColumnInfo(table);

        return (info == null) ? null : info.getName();
    }

    /**
     * Return column information for the primary key column, or null if the
     * table has no primary key. We assume there's only one primary key per
     * table; if there are more, only the first one will be returned.
     * 
     * @param table
     *            The name of the RDBMS table
     * @return A ColumnInfo object, or null if the table has no primary key.
     * @exception SQLException
     *                If a database error occurs
     */
    static ColumnInfo getPrimaryKeyColumnInfo(String table) throws SQLException
    {
        ColumnInfo[] cinfo = getColumnInfo(canonicalize(table));

        for (int i = 0; i < cinfo.length; i++)
        {
            ColumnInfo info = cinfo[i];

            if (info.isPrimaryKey())
            {
                return info;
            }
        }

        return null;
    }

    /**
     * Execute SQL as a PreparedStatement on Connection. Bind parameters in
     * columns to the values in the table row before executing.
     * 
     * @param connection
     *            The SQL connection
     * @param sql
     *            The query to execute
     * @param columns
     *            The columns to bind
     * @param row
     *            The row
     * @return The number of rows affected by the query.
     * @exception SQLException
     *                If a database error occurs
     */
    private static int execute(Connection connection, String sql, List columns,
            TableRow row) throws SQLException
    {
        String dbName =ConfigurationManager.getProperty("db.name");
        PreparedStatement statement = null;

        if (log.isDebugEnabled())
        {
            log.debug("Running query \"" + sql + "\"");
        }

        try
        {
            statement = connection.prepareStatement(sql);
        	
            int count = 0;

            for (Iterator iterator = columns.iterator(); iterator.hasNext();)
            {
                count++;

                ColumnInfo info = (ColumnInfo) iterator.next();
                String column = info.getName();
                int jdbctype = info.getType();

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
                else if ((jdbctype == Types.INTEGER) || (jdbctype == Types.NUMERIC)
                        || (jdbctype == Types.DECIMAL))
                {
                    // If we are using Oracle, we can pass in long values, so always do so.
                    if ("oracle".equals(dbName))
                        statement.setLong(count, row.getLongColumn(column));
                    else
                        statement.setInt(count, row.getIntColumn(column));

                    continue;
                }
                else if (jdbctype == Types.BIGINT)
                {
                    statement.setLong(count, row.getLongColumn(column));
                }
                else if (jdbctype == Types.CLOB && "oracle".equals(dbName))
                {
                    // Support CLOBs in place of TEXT columns in Oracle
                    statement.setString(count, row.getStringColumn(column));

                    continue;
                }
                else if (jdbctype == Types.VARCHAR)
                {
                    statement.setString(count, row.getStringColumn(column));

                    continue;
                }
                else if (jdbctype == Types.DATE)
                {
                    java.sql.Date d = new java.sql.Date(row.getDateColumn(
                            column).getTime());
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
                    Timestamp t = new Timestamp(row.getDateColumn(column)
                            .getTime());
                    statement.setTimestamp(count, t);

                    continue;
                }
                else
                {
                    throw new IllegalArgumentException(
                            "Unsupported JDBC type: " + jdbctype);
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
                catch (SQLException sqle)
                {
                }
            }
        }
    }

    /**
     * Return metadata about a table.
     * 
     * @param table
     *            The name of the table
     * @return An map of info.
     * @exception SQLException
     *                If a database error occurs
     */
    private static Map getColumnInfoInternal(String table) throws SQLException
    {
        String ctable = canonicalize(table);
        Map results = (Map) info.get(ctable);

        if (results != null)
        {
            return results;
        }

        results = retrieveColumnInfo(ctable);
        info.put(ctable, results);

        return results;
    }

    /**
     * Read metadata about a table from the database.
     * 
     * @param table
     *            The RDBMS table.
     * @return A map of information about the columns. The key is the name of
     *         the column, a String; the value is a ColumnInfo object.
     * @exception SQLException
     *                If there is a problem retrieving information from the
     *                RDBMS.
     */
    private static Map retrieveColumnInfo(String table) throws SQLException
    {
        Connection connection = null;

        try
        {
            String schema = ConfigurationManager.getProperty("db.schema");
            connection = getConnection();

            DatabaseMetaData metadata = connection.getMetaData();
            HashMap results = new HashMap();

            int max = metadata.getMaxTableNameLength();
            String tname = (table.length() >= max) ? table
                    .substring(0, max - 1) : table;

            ResultSet pkcolumns = metadata.getPrimaryKeys(null, schema, tname);
            Set pks = new HashSet();

            while (pkcolumns.next())
                pks.add(pkcolumns.getString(4));

            ResultSet columns = metadata.getColumns(null, schema, tname, null);

            while (columns.next())
            {
                String column = columns.getString(4);
                ColumnInfo cinfo = new ColumnInfo();
                cinfo.setName(column);
                cinfo.setType((int) columns.getShort(5));

                if (pks.contains(column))
                {
                    cinfo.setIsPrimaryKey(true);
                }

                results.put(column, cinfo);
            }

            return results;
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    /**
     * Provide a means for a (web) application to cleanly terminate the connection pool.
     * @throws SQLException
     */
    public static synchronized void shutdown() throws SQLException
    {
        if (initialized)
        {
            initialized = false;
            // Get the registered DBCP pooling driver
            PoolingDriver driver = (PoolingDriver)DriverManager.getDriver("jdbc:apache:commons:dbcp:");

            // Close the named pool
            if (driver != null)
                driver.closePool(poolName);
        }
    }

    /**
     * Initialize the DatabaseManager.
     */
    private static synchronized void initialize() throws SQLException
    {
        if (initialized)
        {
            return;
        }

        try
        {
            // Register basic JDBC driver
            Class.forName(ConfigurationManager.getProperty("db.driver"));

            // Register the DBCP driver
            Class.forName("org.apache.commons.dbcp.PoolingDriver");

            // Read pool configuration parameter or use defaults
            // Note we check to see if property is null; getIntProperty returns
            // '0' if the property is not set OR if it is actually set to zero.
            // But 0 is a valid option...
            int maxConnections = ConfigurationManager
                    .getIntProperty("db.maxconnections");

            if (ConfigurationManager.getProperty("db.maxconnections") == null)
            {
                maxConnections = 30;
            }

            int maxWait = ConfigurationManager.getIntProperty("db.maxwait");

            if (ConfigurationManager.getProperty("db.maxwait") == null)
            {
                maxWait = 5000;
            }

            int maxIdle = ConfigurationManager.getIntProperty("db.maxidle");

            if (ConfigurationManager.getProperty("db.maxidle") == null)
            {
                maxIdle = -1;
            }

            boolean useStatementPool = ConfigurationManager.getBooleanProperty("db.statementpool",true);

            // Create object pool
            ObjectPool connectionPool = new GenericObjectPool(null, // PoolableObjectFactory
                    // - set below
                    maxConnections, // max connections
                    GenericObjectPool.WHEN_EXHAUSTED_BLOCK, maxWait, // don't
                                                                     // block
                    // more than 5
                    // seconds
                    maxIdle, // max idle connections (unlimited)
                    true, // validate when we borrow connections from pool
                    false // don't bother validation returned connections
            );

            // ConnectionFactory the pool will use to create connections.
            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                    ConfigurationManager.getProperty("db.url"),
                    ConfigurationManager.getProperty("db.username"),
                    ConfigurationManager.getProperty("db.password"));

            //
            // Now we'll create the PoolableConnectionFactory, which wraps
            // the "real" Connections created by the ConnectionFactory with
            // the classes that implement the pooling functionality.
            //
            String validationQuery = "SELECT 1";

            // Oracle has a slightly different validation query
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                validationQuery = "SELECT 1 FROM DUAL";
            }

            GenericKeyedObjectPoolFactory statementFactory = null;
            if (useStatementPool)
            {
	            // The statement Pool is used to pool prepared statements.
	            GenericKeyedObjectPool.Config statementFactoryConfig = new GenericKeyedObjectPool.Config();
	            // Just grow the pool size when needed. 
	            // 
	            // This means we will never block when attempting to 
	            // create a query. The problem is unclosed statements, 
	            // they can never be reused. So if we place a maximum 
	            // cap on them, then we might reach a condition where 
	            // a page can only be viewed X number of times. The 
	            // downside of GROW_WHEN_EXHAUSTED is that this may 
	            // allow a memory leak to exist. Both options are bad, 
	            // but I'd prefer a memory leak over a failure.
	            //
	            // FIXME: Perhaps this decision should be derived from config parameters?
	            statementFactoryConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
	
	            statementFactory = new GenericKeyedObjectPoolFactory(null,statementFactoryConfig);
            }
            
            PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
                    connectionFactory, connectionPool, statementFactory,
                    validationQuery, // validation query
                    false, // read only is not default for now
                    false); // Autocommit defaults to none

            // Obtain a poolName from the config, default is "dspacepool"
            if (ConfigurationManager.getProperty("db.poolname") != null)
            {
                poolName = ConfigurationManager.getProperty("db.poolname");
            }

            //
            // Finally, we get the PoolingDriver itself...
            //
            PoolingDriver driver = (PoolingDriver)DriverManager.getDriver("jdbc:apache:commons:dbcp:");

            //
            // ...and register our pool with it.
            //
            if (driver != null)
                driver.registerPool(poolName, connectionPool);

            // Old SimplePool init
            //DriverManager.registerDriver(new SimplePool());
            initialized = true;
        }
        catch (SQLException se)
        {
            // Simply throw up SQLExceptions
            throw se;
        }
        catch (Exception e)
        {
            // Need to be able to catch other exceptions. Pretend they are
            // SQLExceptions, but do log
            log.warn("Exception initializing DB pool", e);
            throw new SQLException(e.toString());
        }
    }

	/**
	 * Iterate over the given parameters and add them to the given prepared statement. 
	 * Only a select number of datatypes are supported by the JDBC driver.
	 *
	 * @param statement
	 * 			The unparameterized statement.
	 * @param parameters
	 * 			The parameters to be set on the statement.
	 */
	protected static void loadParameters(PreparedStatement statement, Object[] parameters) 
	throws SQLException{
		
		statement.clearParameters();
		
	    for(int i=0; i < parameters.length; i++)
	    {	
	    	// Select the object we are setting.
	    	Object parameter = parameters[i];
	    	int idx = i+1; // JDBC starts counting at 1.
	    	
	    	if (parameter == null)
	    	{
	    		throw new SQLException("Attempting to insert null value into SQL query.");
	    	}
	    	if (parameter instanceof String)
	    	{
	    		statement.setString(idx,(String) parameters[i]);
	    	}
	    	else if (parameter instanceof Integer)
	    	{
	    		int ii = ((Integer) parameter).intValue();
	    		statement.setInt(idx,ii);
	    	}
	    	else if (parameter instanceof Double)
	    	{
	    		double d = ((Double) parameter).doubleValue();
	    		statement.setDouble(idx,d);
	    	}
	    	else if (parameter instanceof Float)
	    	{
	    		float f = ((Float) parameter).floatValue();
	    		statement.setFloat(idx,f);
	    	}
	    	else if (parameter instanceof Short)
	    	{
	    		short s = ((Short) parameter).shortValue();
	    		statement.setShort(idx,s);
	    	}
	    	else if (parameter instanceof Long)
	    	{
	    		long l = ((Long) parameter).longValue();
	    		statement.setLong(idx,l);
	    	}
	    	else if (parameter instanceof Date)
	    	{
	    		Date date = (Date) parameter;
	    		statement.setDate(idx,date);
	    	}
	    	else if (parameter instanceof Time)
	    	{
	    		Time time = (Time) parameter;
	    		statement.setTime(idx,time);
	    	}
	    	else if (parameter instanceof Timestamp)
	    	{
	    		Timestamp timestamp = (Timestamp) parameter;
	    		statement.setTimestamp(idx,timestamp);
	    	}
	    	else
	    	{
	    		throw new SQLException("Attempting to insert unknown datatype ("+parameter.getClass().getName()+") into SQL statement.");
	    	}        	
	    }
	}

}

/**
 * Represents a column in an RDBMS table.
 */

class ColumnInfo
{
    /** The name of the column */
    private String name;

    /** The JDBC type of the column */
    private int type;

    /** True if this column is a primary key */
    private boolean isPrimaryKey;

    /**
     * Constructor
     */
    ColumnInfo()
    {
    }

    /**
     * Constructor
     */
    ColumnInfo(String name, int type)
    {
        this.name = name;
        this.type = type;
    }

    /**
     * Return the column name.
     * 
     * @return - The column name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the column name
     * 
     * @param v -
     *            The column name
     */
    void setName(String v)
    {
        name = v;
    }

    /**
     * Return the JDBC type. This is one of the constants from java.sql.Types.
     * 
     * @return - The JDBC type
     * @see java.sql.Types
     */
    public int getType()
    {
        return type;
    }

    /**
     * Set the JDBC type. This should be one of the constants from
     * java.sql.Types.
     * 
     * @param v -
     *            The JDBC type
     * @see java.sql.Types
     */
    void setType(int v)
    {
        type = v;
    }

    /**
     * Return true if this column is a primary key.
     * 
     * @return True if this column is a primary key, false otherwise.
     */
    public boolean isPrimaryKey()
    {
        return isPrimaryKey;
    }

    /**
     * Set whether this column is a primary key.
     * 
     * @param v
     *            True if this column is a primary key.
     */
    void setIsPrimaryKey(boolean v)
    {
        this.isPrimaryKey = v;
    }

    /*
     * Return true if this object is equal to other, false otherwise.
     * 
     * @return True if this object is equal to other, false otherwise.
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof ColumnInfo))
        {
            return false;
        }

        ColumnInfo theOther = (ColumnInfo) other;

        return ((name != null) ? name.equals(theOther.name)
                : (theOther.name == null))
                && (type == theOther.type)
                && (isPrimaryKey == theOther.isPrimaryKey);
    }

    /*
     * Return a hashCode for this object.
     * 
     * @return A hashcode for this object.
     */
    public int hashCode()
    {
        return new StringBuffer().append(name).append(type)
                .append(isPrimaryKey).toString().hashCode();
    }
}
