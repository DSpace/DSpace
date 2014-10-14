/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes SQL queries.
 *
 * @author Peter Breton
 * @author Jim Downing
 * @version $Revision$
 */
public class DatabaseManager
{
    /** logging category */
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    /** True if initialization has been done */
    private static boolean initialized = false;

    private static Map<String, String> insertSQL = new HashMap<String, String>();

    private static boolean isOracle = false;
    private static boolean isPostgres = false;

    /** DataSource (retrieved from jndi */
    private static DataSource dataSource = null;

    /** Name of the DBMS, as returned by its driver. */
    private static String dbms;

    /** Name of the DBMS, as used in DSpace:  "postgres", "oracle", or "h2". */
    protected static String dbms_keyword;

    /** The static variables which represent the DBMS keyword **/
    public static final String DBMS_POSTGRES="postgres";
    public static final String DBMS_ORACLE="oracle";
    public static final String DBMS_H2="h2";

    /** Name to use for the pool */
    private static String poolName = "dspacepool";
    
    /** Database Status Flags for Flyway setup. Fresh Install vs. pre-4.0 vs */
    private static final int STATUS_PRE_4_0 = -1;
    private static final int STATUS_FRESH_INSTALL = 0;
    private static final int STATUS_NO_FLYWAY = 1;
    private static final int STATUS_FLYWAY = 2;
    

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
    private static final Pattern DB_SAFE_NAME = Pattern.compile("^[a-zA-Z_1-9.]+$");

    /**
     * A map of database column information. The key is the table name, a
     * String; the value is an array of ColumnInfo objects.
     */
    private static Map<String, Map<String, ColumnInfo>> info = new HashMap<String, Map<String, ColumnInfo>>();

    /**
     * Protected Constructor to prevent instantiation except by derived classes.
     */
    protected DatabaseManager()
    {
    }

    public static boolean isOracle()
    {
        // If we have NOT determined whether we are using Postgres
        // or Oracle, then we need to initialize() first
        if(isPostgres==false && isOracle==false)
        {
            try
            {
                initialize();
            } catch (SQLException ex)
            {
                log.error("Failed to initialize the database:  ", ex);
            }
        }
        return isOracle;
    }

    /**
     * Set the constraint check to deferred (commit time)
     *
     * @param context
     *            The context object
     * @param constraintName
     *            the constraint name to deferred
     * @throws SQLException
     */
    public static void setConstraintDeferred(Context context,
            String constraintName) throws SQLException {
        Statement statement = null;
        try
        {
            statement = context.getDBConnection().createStatement();
            statement.execute("SET CONSTRAINTS " + constraintName + " DEFERRED");
            statement.close();
        } catch (SQLException e) {
            log.error("SQL setConstraintDeferred Error - ", e);
            throw e;
        } finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                    log.error("SQL setConstraintDeferred close Error - ",sqle);
                    throw sqle;
                }
            }
        }
    }

    /**
     * Set the constraint check to immediate (every query)
     *
     * @param context
     *            The context object
     * @param constraintName
     *            the constraint name to check immediately after every query
     * @throws SQLException
     */
    public static void setConstraintImmediate(Context context,
            String constraintName) throws SQLException {
        Statement statement = null;
        try
        {
            statement = context.getDBConnection().createStatement();
            statement.execute("SET CONSTRAINTS " + constraintName + " IMMEDIATE");
            statement.close();
        } catch (SQLException e) {
            log.error("SQL setConstraintImmediate Error - ", e);
            throw e;
        } finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                    log.error("SQL setConstraintImmediate Error - ",sqle);
                    throw sqle;
                }
            }
        }
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
    public static TableRowIterator queryTable(Context context, String table, String query, Object... parameters ) throws SQLException {
        if (log.isDebugEnabled())
        {
            StringBuilder sb = new StringBuilder("Running query \"").append(query).append("\"  with parameters: ");
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug(sb.toString());
        }

        PreparedStatement statement = null;
        try
        {
            statement = context.getDBConnection().prepareStatement(query);

            loadParameters(statement, parameters);

            TableRowIterator retTRI = new TableRowIterator(statement.executeQuery(), canonicalize(table));

            retTRI.setStatement(statement);
            return retTRI;
        }
        catch (SQLException sqle)
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException s)
                {
                    log.error("SQL QueryTable close Error - ",s);
                    throw s;
                }
            }
            log.error("SQL QueryTable Error - ",sqle);
            throw sqle;
        }
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
        try
        {
            loadParameters(statement,parameters);

            TableRowIterator retTRI = new TableRowIterator(statement.executeQuery());

            retTRI.setStatement(statement);
            return retTRI;
        }
        catch (SQLException sqle)
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException s)
                {
                    log.error("SQL query exec close Error - ",s);
                    throw s;
                }
            }
            log.error("SQL query exec Error - ",sqle);
            throw sqle;
        }
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
            Object... parameters) throws SQLException {
        TableRow retRow = null;
        TableRowIterator iterator = null;
        try
        {
            iterator = query(context, query, parameters);
            retRow = (!iterator.hasNext()) ? null : iterator.next();
        } catch (SQLException e) {
            log.error("SQL query single Error - ", e);
            throw e;
        } finally
        {
            if (iterator != null)
            {
                iterator.close();
            }
        }

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
            String query, Object... parameters) throws SQLException {
        TableRow retRow = null;
        TableRowIterator iterator = null;
        try {
            iterator = queryTable(context, canonicalize(table), query, parameters);
        } catch (SQLException e) {
            log.error("SQL query singleTable Error - ", e);
            throw e;
        }

        try
        {
            retRow = (!iterator.hasNext()) ? null : iterator.next();
        } catch (SQLException e) {
            log.error("SQL query singleTable Error - ", e);
            throw e;
        } finally
        {
            if (iterator != null)
            {
                iterator.close();
            }
        }
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
    public static int updateQuery(Context context, String query, Object... parameters) throws SQLException {
        PreparedStatement statement = null;

        if (log.isDebugEnabled())
        {
            StringBuilder sb = new StringBuilder("Running query \"").append(query).append("\"  with parameters: ");
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug(sb.toString());
        }

        try
        {
        	statement = context.getDBConnection().prepareStatement(query);
        	loadParameters(statement, parameters);

        	return statement.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL query updateQuery Error - ", e);
            throw e;
        } finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                    log.error("SQL updateQuery Error - ",sqle);
                    throw sqle;
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
    public static TableRow create(Context context, String table) throws SQLException

    {
        try {
            TableRow row = new TableRow(canonicalize(table), getColumnNames(table));
            insert(context, row);
            return row;
        } catch (SQLException e) {
            log.error("SQL create Error - ",e);
            throw e;
        }
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
    public static TableRow find(Context context, String table, int id) throws SQLException

    {
        String ctable = canonicalize(table);

        try {
            return findByUnique(context, ctable, getPrimaryKeyColumn(ctable),
                    Integer.valueOf(id));
        } catch (SQLException e) {
            log.error("SQL find Error - ", e);
            throw e;
        }
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
        String column, Object value) throws SQLException {
        String ctable = canonicalize(table);

        try {
            if ( ! DB_SAFE_NAME.matcher(ctable).matches())
            {
                    throw new SQLException("Unable to execute select query because table name (" + ctable + ") contains non alphanumeric characters.");
            }

            if ( ! DB_SAFE_NAME.matcher(column).matches())
            {
                throw new SQLException("Unable to execute select query because column name (" + column + ") contains non alphanumeric characters.");
            }
            StringBuilder sql = new StringBuilder("select * from ").append(ctable).append(" where ").append(column).append(" = ? ");
            return querySingleTable(context, ctable, sql.toString(), value);
        } catch (SQLException e) {
            log.error("SQL findByUnique Error - ", e);
            throw e;
        }
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
    public static int delete(Context context, String table, int id) throws SQLException
    {
        try {
            String ctable = canonicalize(table);
            return deleteByValue(context, ctable, getPrimaryKeyColumn(ctable),
                    Integer.valueOf(id));
        } catch (SQLException e) {
            log.error("SQL delete Error - ", e);
            throw e;
        }
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
        try {
            String ctable = canonicalize(table);

            if ( ! DB_SAFE_NAME.matcher(ctable).matches())
            {
                throw new SQLException("Unable to execute delete query because table name (" + ctable + ") contains non alphanumeric characters.");
            }

            if ( ! DB_SAFE_NAME.matcher(column).matches())
            {
                throw new SQLException("Unable to execute delete query because column name (" + column + ") contains non alphanumeric characters.");
            }

            StringBuilder sql = new StringBuilder("delete from ").append(ctable).append(" where ").append(column).append(" = ? ");
            return updateQuery(context, sql.toString(), value);
        } catch (SQLException e) {
            log.error("SQL deleteByValue Error - ", e);
            throw e;
        }
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
        DataSource dsource = getDataSource();

        try
        {
            if (dsource != null) {
                return dsource.getConnection();
            }

            return null;
        } catch (SQLException e) {
            log.error("SQL connection Error - ", e);
            throw e;
        }
    }

    public static DataSource getDataSource()
    {
        if(dataSource==null)
        {
            try
            {
                initialize();
            }
            catch (SQLException e)
            {
                log.error("SQL getDataSource Error - ",e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return dataSource;
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
            log.warn(e.getMessage(), e);
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
    public static void insert(Context context, TableRow row) throws SQLException
    {
        int newID;
        if (isPostgres)
        {
            newID = doInsertPostgres(context, row);
        }
        else
        {
            newID = doInsertGeneric(context, row);
        }

        row.setColumn(getPrimaryKeyColumn(row), newID);
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
        String table = row.getTable();

        StringBuilder sql = new StringBuilder().append("update ").append(table)
                .append(" set ");

        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        ColumnInfo pk = getPrimaryKeyColumnInfo(table);
        Collection<ColumnInfo> info = getColumnInfo(table);

        String separator = "";
        for (ColumnInfo col : info)
        {
            // Only update this column if it has changed
            if (!col.isPrimaryKey())
            {
                if (row.hasColumnChanged(col.getName()))
                {
                    sql.append(separator).append(col.getName()).append(" = ?");
                    columns.add(col);
                    separator = ", ";
                }
            }
        }

        // Only execute the update if there is anything to update
        if (columns.size() > 0)
        {
            sql.append(" where ").append(pk.getName()).append(" = ?");
            columns.add(pk);

            return executeUpdate(context.getDBConnection(), sql.toString(), columns, row);
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
        if (null == row.getTable())
        {
            throw new IllegalArgumentException("Row not associated with a table");
        }

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
    static Collection<ColumnInfo> getColumnInfo(String table) throws SQLException
    {
        Map<String, ColumnInfo> cinfo = getColumnInfoInternal(table);

        return (cinfo == null) ? null : cinfo.values();
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
        Map<String, ColumnInfo> info = getColumnInfoInternal(table);

        return (info == null) ? null : info.get(column);
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
    static List<String> getColumnNames(String table) throws SQLException
    {
        List<String> results = new ArrayList<String>();
        Collection<ColumnInfo> info = getColumnInfo(table);

        for (ColumnInfo col : info)
        {
            results.add(col.getName());
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
    static List<String> getColumnNames(ResultSetMetaData meta) throws SQLException
    {
        List<String> results = new ArrayList<String>();
        int columns = meta.getColumnCount();

        for (int i = 0; i < columns; i++)
        {
            results.add(meta.getColumnLabel(i + 1));
        }

        return results;
    }

    /**
     * Return the canonical name for a database object.
     *
     * @param db_object
     *            The name of the database object.
     * @return The canonical name of the database object.
     */
    static String canonicalize(String db_object)
    {
        // Oracle expects upper-case table names, schemas, etc.
        if (isOracle)
        {
            return (db_object == null) ? null : db_object.toUpperCase();
        }

        // default database postgres wants lower-case table names
        return (db_object == null) ? null : db_object.toLowerCase();
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
            log.error("IOE loadSQL Error - ",ioe);
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
        StringBuilder sqlBuilder = new StringBuilder();
        String sql = null;

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

                String input = (commentStart != -1) ? line.substring(0, commentStart) : line;

                // Empty line, skip
                if (input.trim().equals(""))
                {
                    continue;
                }

                // Put it on the SQL buffer
                sqlBuilder.append(input.replace(';', ' ')); // remove all semicolons
                                                     // from sql file!

                // Add a space
                sqlBuilder.append(" ");

                // More to come?
                // Look for quotes
                int index = 0;
                int count = 0;
                int inputlen = input.length();

                while ((index = input.indexOf('\'', count)) != -1)
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

                int endMarker = input.indexOf(';', index);

                if (endMarker == -1)
                {
                    continue;
                }

                sql = sqlBuilder.toString();
                if (log.isDebugEnabled())
                {
                    log.debug("Running database query \"" + sql + "\"");
                }

                try
                {
                    // Use execute, not executeQuery (which expects results) or
                    // executeUpdate
                    statement.execute(sql);
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
                    boolean isDrop = ((sql != null) && (sqlmessage != null)
                            && (sql.toUpperCase().startsWith("DROP"))
                            && (sqlmessage.indexOf("does not exist") != -1));

                    // Creating a view causes a bogus warning
                    boolean isNoResults = ((sql != null)
                            && (sqlmessage != null)
                            && (sql.toUpperCase().startsWith("CREATE VIEW")
                                    || sql.toUpperCase().startsWith("CREATE FUNCTION"))
                            && (sqlmessage.indexOf("No results were returned") != -1));

                    // If the messages are bogus, give them a low priority
                    if (isDrop || isNoResults)
                    {
                        log.debug(msg, sqle);
                    }
                    // Otherwise, we need to know!
                    else
                    {
                        log.warn(msg, sqle);
                    }
                }

                // Reset SQL buffer
                sqlBuilder = new StringBuilder();
                sql = null;
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
    static TableRow process(ResultSet results, String table) throws SQLException
    {
        return process(results, table, null);
    }

    /**
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
    static TableRow process(ResultSet results, String table, List<String> pColumnNames) throws SQLException
    {
        ResultSetMetaData meta = results.getMetaData();
        int columns = meta.getColumnCount() + 1;

        // If we haven't been passed the column names try to generate them from the metadata / table
        List<String> columnNames = pColumnNames != null ? pColumnNames :
                                        ((table == null) ? getColumnNames(meta) : getColumnNames(table));

        TableRow row = new TableRow(canonicalize(table), columnNames);

        // Process the columns in order
        // (This ensures maximum backwards compatibility with
        // old JDBC drivers)
        for (int i = 1; i < columns; i++)
        {
            String name = meta.getColumnName(i);
            int jdbctype = meta.getColumnType(i);

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

                case Types.BIGINT:
                    row.setColumn(name, results.getLong(i));
                    break;

                case Types.NUMERIC:
                case Types.DECIMAL:
                    row.setColumn(name, results.getBigDecimal(i));
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
                    try
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
                    }
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
        Collection<ColumnInfo> cinfo = getColumnInfo(canonicalize(table));

        for (ColumnInfo info : cinfo)
        {
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
    private static void execute(Connection connection, String sql, Collection<ColumnInfo> columns, TableRow row) throws SQLException
    {
        PreparedStatement statement = null;

        if (log.isDebugEnabled())
        {
            log.debug("Running query \"" + sql + "\"");
        }

        try
        {
            statement = connection.prepareStatement(sql);
        	loadParameters(statement, columns, row);
            statement.execute();
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
                    log.error("SQL execute Error - ",sqle);
                    throw sqle;
                }
            }
        }
    }

    private static int executeUpdate(Connection connection, String sql, Collection<ColumnInfo> columns, TableRow row) throws SQLException
    {
        PreparedStatement statement = null;

        if (log.isDebugEnabled())
        {
            log.debug("Running query \"" + sql + "\"");
        }

        try
        {
            statement = connection.prepareStatement(sql);
        	loadParameters(statement, columns, row);
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
                    log.error("SQL executeUpdate Error - ",sqle);
                    throw sqle;
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
    private static Map<String, ColumnInfo> getColumnInfoInternal(String table) throws SQLException
    {
        String ctable = canonicalize(table);
        Map<String, ColumnInfo> results = info.get(ctable);

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
    private static Map<String, ColumnInfo> retrieveColumnInfo(String table) throws SQLException
    {
        Connection connection = null;
        ResultSet pkcolumns = null;
        ResultSet columns = null;

        try
        {
            String schema = canonicalize(ConfigurationManager.getProperty("db.schema"));
            if(StringUtils.isBlank(schema)){
                schema = null;
            }
            String catalog = null;

            int dotIndex = table.indexOf('.');
            if (dotIndex > 0)
            {
                catalog = table.substring(0, dotIndex);
                table = table.substring(dotIndex + 1, table.length());
                log.warn("catalog: " + catalog);
                log.warn("table: " + table);
            }

            connection = getConnection();

            DatabaseMetaData metadata = connection.getMetaData();
            Map<String, ColumnInfo> results = new HashMap<String, ColumnInfo>();

            int max = metadata.getMaxTableNameLength();
            String tname = ((max > 0) && (table.length() >= max)) ? table
                    .substring(0, max - 1) : table;

            pkcolumns = metadata.getPrimaryKeys(catalog, schema, tname);

            Set<String> pks = new HashSet<String>();

            while (pkcolumns.next())
            {
                pks.add(pkcolumns.getString(4));
            }

            columns = metadata.getColumns(catalog, schema, tname, null);

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

            return Collections.unmodifiableMap(results);
        }
        finally
        {
            if (pkcolumns != null)
            {
                try { pkcolumns.close(); } catch (SQLException sqle) { }
            }

            if (columns != null)
            {
                try { columns.close(); } catch (SQLException sqle) { }
            }

            if (connection != null)
            {
                try { connection.close(); } catch (SQLException sqle) { }
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
            dataSource = null;
            initialized = false;
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
            String jndiName = ConfigurationManager.getProperty("db.jndi");
            if (!StringUtils.isEmpty(jndiName))
            {
                try
                {
                    javax.naming.Context ctx = new InitialContext();
                    javax.naming.Context env = ctx == null ? null : (javax.naming.Context)ctx.lookup("java:/comp/env");
                    dataSource = (DataSource)(env == null ? null : env.lookup(jndiName));
                }
                catch (Exception e)
                {
                    log.error("Error retrieving JNDI context: " + jndiName, e);
                }

                if (dataSource != null)
                {
                    log.debug("Using JNDI dataSource: " + jndiName);
                }
                else
                {
                    log.info("Unable to locate JNDI dataSource: " + jndiName);
                }
            }

            if (dataSource == null)
            {
                if (!StringUtils.isEmpty(jndiName))
                {
                    log.info("Falling back to creating own Database pool");
                }

                dataSource = DataSourceInit.getDatasource();
            }

            // What brand of DBMS do we have?
            Connection connection = dataSource.getConnection();
            DatabaseMetaData meta = connection.getMetaData();
            dbms = meta.getDatabaseProductName();
            String dbms_lc = dbms.toLowerCase(Locale.ROOT);
            if (dbms_lc.contains("postgresql"))
            {
                isPostgres = true;
                dbms_keyword = DBMS_POSTGRES;
                log.info("DBMS is PostgreSQL");
            }
            else if (dbms_lc.contains("oracle"))
            {
                isOracle = true;
                dbms_keyword = DBMS_ORACLE;
                log.info("DBMS is Oracle Database");
            }
            else if (dbms_lc.contains("h2")) // Used for unit testing only
            {
                // We set "isOracle=true" for H2 simply because it's NOT 100%
                // PostgreSQL compatible. So, code which is highly PostgreSQL
                // specific often may not work properly on H2.
                // I.e. this acts more like a "isNotPostgreSQL" flag
                isOracle = true;
                dbms_keyword = DBMS_H2;
                log.info("DBMS is H2");
            }
            else
            {
                log.error("DBMS {} is unsupported", dbms);
            }
            log.info("DBMS driver version is '{}'", meta.getDatabaseProductVersion());
            
            // FINALLY, ensure database scheme is up-to-date. If not, upgrade/migrate database.
            // (NOTE: This needs to run LAST as it may need some of the initialized
            // variables set above)
            initializeDatabase(connection);

            connection.close();
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
            throw new SQLException(e.toString(), e);
        }
    }
    
    /**
     * Ensures the current database is up-to-date with regards
     * to the latest DSpace DB schema. If the scheme is not up-to-date,
     * then any necessary database migrations are performed.
     * 
     * @param connection
     *      Database connection
     */
    private static synchronized void initializeDatabase(Connection connection) 
            throws IOException, SQLException
    {
        // Get the name of the Schema that the DSpace Database is using
        String schema = ConfigurationManager.getProperty("db.schema");
        if(StringUtils.isBlank(schema)){
            schema = null;
        }
        
        // Initialize Flyway DB API (http://flywaydb.org/), used to perform DB migrations
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setEncoding("UTF-8");
    
        // Migration scripts are based on DBMS Keyword (see full path below)
        String scriptFolder = dbms_keyword;
        
        // Set location where Flyway will load DB scripts from (based on DB Type)
        // e.g. [dspace.dir]/etc/[dbtype]/
        String scriptPath = ConfigurationManager.getProperty("dspace.dir") +
                            System.getProperty("file.separator") + "etc" +
                            System.getProperty("file.separator") + "migrations" +
                            System.getProperty("file.separator") + scriptFolder;
        
        log.info("Loading Flyway DB scripts from " + scriptPath + " and Package 'org.dspace.storage.rdbms.migration.*'");
        flyway.setLocations("filesystem:" + scriptPath, "classpath:org.dspace.storage.rdbms.migration");
        
        // Get our Database migration status, so we know what to tell Flyway to do
        int status = getDbMigrationStatus(schema, connection, flyway);
        
        // If we have a pre-4.0 Database, we need to exit immediately. There's nothing we can do here
        if(status==STATUS_PRE_4_0)
            throw new SQLException("CANNOT AUTOUPGRADE DSPACE DATABASE, AS IT DOES NOT LOOK TO BE A VALID DSPACE 4.0 DATABASE. " +
                        "Please manually upgrade your database to DSpace 4.0 compatibility.");
        // If this is a fresh install
        else if (status==STATUS_FRESH_INSTALL)
        {
            // Just let Flyway initialize our database
            flyway.init();
        }
        // If we have a valid 4.0 database, but haven't initialized Flyway on it
        else if (status == STATUS_NO_FLYWAY)
        {
            // Initialize the Flyway database table.
            // We are hardcoding the schema version to 4.0 because this should ONLY
            // be encountered on a 4.0 database. After 4.0, all databases should 
            // already have Flyway initialized.
            // (NOTE: Flyway will also create the db.schema, if it doesn't exist)
            flyway.setInitVersion("4.0");
            flyway.setInitDescription("Initial DSpace 4.0 database schema");
            flyway.init();
        }
           
        // Determine pending Database migrations
        MigrationInfo[] pending = flyway.info().pending();
        
        // Log info about pending migrations
        if (pending!=null && pending.length>0) 
        {   
            log.info("Pending DSpace database schema migrations:");
            for (MigrationInfo info : pending)
            {
                log.info("\t" + info.getVersion() + " " + info.getDescription() + " " + info.getType() + " " + info.getState());
            }
        }
        else
            log.info("DSpace database schema is up to date.");

        // Ensure database is on the latest version of the DSpace schema
        flyway.migrate();
    }
    
    /**
     * Determine the migration status of our Database
     * so that we are able to properly migrate it to the latest schema
     * via Flyway
     * 
     * @param schema
     *          Name of the Schema being used by the DSpace database
     * @param connection
     *          Current Database Connection
     * @param flyway
     *          Our Flyway settings
     * @return status flag 
     */
    private static int getDbMigrationStatus(String schema, Connection connection, Flyway flyway)
            throws SQLException
    {
        // Get information about our database. We'll use this to determine DB status.
        DatabaseMetaData meta = connection.getMetaData();
       
        // First, is this a "fresh_install"?  Check for an "item" table.
        ResultSet tables = meta.getTables(null, schema, "item", null);
        if (!tables.next()) 
        {
            tables.close();
            // No "item" table, this is a fresh install of DSpace
            return STATUS_FRESH_INSTALL;
        }
  
        // Second, is this DSpace DB Schema compatible with 4.0?  Check for a "Webapp" table (which was added in 4.0)
        // TODO: If the "Webapp" table is ever removed, then WE NEED TO CHANGE THIS CHECK.
        tables = meta.getTables(null, schema, "Webapp", null);
        if (!tables.next()) 
        {
            tables.close();
            // No "Webapp" table, so this must be a pre-4.0 database
            return STATUS_PRE_4_0;
        }
        
        // Finally, Check if the necessary Flyway table ("schema_version") exists in this database
        tables = meta.getTables(null, schema, flyway.getTable(), null);
        if (!tables.next()) 
        {
            tables.close();
            // No Flyway table, so we need to get Flyway initialized in this database
            return STATUS_NO_FLYWAY;
        }
        
        // IF we get here, we have 4.0 or above compatible database and Flyway is already installed
        return STATUS_FLYWAY;
    }
    
    /**
     * What is the name of our DBMS?
     *
     * @return name returned by the DBMS driver.
     */
    public static String getDbName()
    {
        if (StringUtils.isBlank(dbms))
        {
            try {
                initialize();
            } catch (SQLException ex) {
                log.error("Failed to initialize the database:  ", ex);
            }
        }
        return dbms;
    }

    /**
     * What is the string that we use to name the DBMS brand?
     * <P>
     * This will return one of: DatabaseManager.DBMS_POSTGRES,
     * DatabaseManager.DBMS_ORACLE, or DatabaseManager.DBMS_H2
     *
     * @return a normalized "keyword" for the DBMS brand:  postgres, oracle, h2.
     */
    public static String getDbKeyword()
    {
        if (StringUtils.isBlank(dbms_keyword))
        {
            try {
                initialize();
            } catch (SQLException ex) {
                log.error("Failed to initialize the database:  ", ex);
            }
        }
        return dbms_keyword;
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
	protected static void loadParameters(PreparedStatement statement, Object[] parameters) throws SQLException
    {
		statement.clearParameters();

        int idx = 1;
        for (Object parameter : parameters)
	    {
	    	if (parameter instanceof String)
	    	{
	    		statement.setString(idx,(String) parameter);
	    	}
            else if (parameter instanceof Long)
            {
                statement.setLong(idx,((Long) parameter).longValue());
            }
	    	else if (parameter instanceof Integer)
	    	{
	    		statement.setInt(idx,((Integer) parameter).intValue());
	    	}
            else if (parameter instanceof Short)
            {
                statement.setShort(idx,((Short) parameter).shortValue());
            }
            else if (parameter instanceof Date)
            {
                statement.setDate(idx,(Date) parameter);
            }
            else if (parameter instanceof Time)
            {
                statement.setTime(idx,(Time) parameter);
            }
            else if (parameter instanceof Timestamp)
            {
                statement.setTimestamp(idx,(Timestamp) parameter);
            }
	    	else if (parameter instanceof Double)
	    	{
	    		statement.setDouble(idx,((Double) parameter).doubleValue());
	    	}
	    	else if (parameter instanceof Float)
	    	{
	    		statement.setFloat(idx,((Float) parameter).floatValue());
	    	}
            else if (parameter == null)
            {
                throw new SQLException("Attempting to insert null value into SQL query.");
            }
	    	else
	    	{
	    		throw new SQLException("Attempting to insert unknown datatype ("+parameter.getClass().getName()+") into SQL statement.");
	    	}

            idx++;
	    }
	}

    private static void loadParameters(PreparedStatement statement, Collection<ColumnInfo> columns, TableRow row) throws SQLException
    {
        int count = 0;
        for (ColumnInfo info : columns)
        {
            count++;
            String column = info.getCanonicalizedName();
            int jdbctype = info.getType();

            if (row.isColumnNull(column))
            {
                statement.setNull(count, jdbctype);
            }
            else
            {
                switch (jdbctype)
                {
                    case Types.BOOLEAN:
                    case Types.BIT:
                        statement.setBoolean(count, row.getBooleanColumn(column));
                        break;

                    case Types.INTEGER:
                        if (isOracle)
                        {
                            statement.setLong(count, row.getLongColumn(column));
                        }
                        else
                        {
                            statement.setInt(count, row.getIntColumn(column));
                        }
                        break;

                    case Types.NUMERIC:
                    case Types.DECIMAL:
                        statement.setLong(count, row.getLongColumn(column));
                        // FIXME should be BigDecimal if TableRow supported that
                        break;

                    case Types.BIGINT:
                        statement.setLong(count, row.getLongColumn(column));
                        break;

                    case Types.CLOB:
                        if (isOracle)
                        {
                            // Support CLOBs in place of TEXT columns in Oracle
                            statement.setString(count, row.getStringColumn(column));
                        }
                        else
                        {
                            throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
                        }
                        break;

                    case Types.VARCHAR:
                        statement.setString(count, row.getStringColumn(column));
                        break;

                    case Types.DATE:
                        statement.setDate(count, new java.sql.Date(row.getDateColumn(column).getTime()));
                        break;

                    case Types.TIME:
                        statement.setTime(count, new Time(row.getDateColumn(column).getTime()));
                        break;

                    case Types.TIMESTAMP:
                        statement.setTimestamp(count, new Timestamp(row.getDateColumn(column).getTime()));
                        break;

                    default:
                        throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
                }
            }
        }
    }

    /**
     * Postgres-specific row insert, combining getnextid() and insert into single statement for efficiency
     * @param context
     * @param row
     * @return
     * @throws SQLException
     */
    private static int doInsertPostgres(Context context, TableRow row) throws SQLException
    {
        String table = row.getTable();

        Collection<ColumnInfo> info = getColumnInfo(table);
        Collection<ColumnInfo> params = new ArrayList<ColumnInfo>();

        String primaryKey = getPrimaryKeyColumn(table);
        String sql = insertSQL.get(table);

        boolean firstColumn = true;
        boolean foundPrimaryKey = false;
        if (sql == null)
        {
            // Generate SQL and filter parameter columns
            StringBuilder insertBuilder = new StringBuilder("INSERT INTO ").append(table).append(" ( ");
            StringBuilder valuesBuilder = new StringBuilder(") VALUES ( ");
            for (ColumnInfo col : info)
            {
                if (firstColumn)
                {
                    firstColumn = false;
                }
                else
                {
                    insertBuilder.append(",");
                    valuesBuilder.append(",");
                }

                insertBuilder.append(col.getName());

                if (!foundPrimaryKey && col.isPrimaryKey())
                {
                    valuesBuilder.append("getnextid('").append(table).append("')");
                    foundPrimaryKey = true;
                }
                else
                {
                    valuesBuilder.append('?');
                    params.add(col);
                }
            }

            sql = insertBuilder.append(valuesBuilder.toString()).append(") RETURNING ").append(getPrimaryKeyColumn(table)).toString();
            insertSQL.put(table, sql);
        }
        else
        {
            // Already have SQL, just filter parameter columns
            for (ColumnInfo col : info)
            {
                if (!foundPrimaryKey && col.isPrimaryKey())
                {
                    foundPrimaryKey = true;
                }
                else
                {
                    params.add(col);
                }
            }
        }

        PreparedStatement statement = null;

        if (log.isDebugEnabled())
        {
            log.debug("Running query \"" + sql + "\"");
        }

        ResultSet rs = null;
        try
        {
            statement = context.getDBConnection().prepareStatement(sql);
        	loadParameters(statement, params, row);
            rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
        finally
        {
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException sqle)
                {
                    log.error("SQL doInsertPostgresrs close Error - ",sqle);
                    throw sqle;
                }
            }

            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                    log.error("SQL doInsertPostgres statement close Error - ",sqle);
                    throw sqle;
                }
            }
        }
    }

    /**
     * Generic version of row insertion with separate id get / insert
     * @param context
     * @param row
     * @return
     * @throws SQLException
     */
    private static int doInsertGeneric(Context context, TableRow row) throws SQLException
    {
        int newID = -1;
        String table = row.getTable();
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            // Get an ID (primary key) for this row by using the "getnextid"
            // SQL function in Postgres, or directly with sequences in Oracle
            if (isOracle)
            {
                statement = context.getDBConnection().prepareStatement("SELECT " + table + "_seq" + ".nextval FROM dual");
            }
            else
            {
                statement = context.getDBConnection().prepareStatement("SELECT getnextid(?) AS result");
                loadParameters(statement, new Object[] { table });
            }
            rs = statement.executeQuery();
            rs.next();
            newID = rs.getInt(1);
        }
        finally
        {
            if (rs != null)
            {
                try { rs.close(); } catch (SQLException sqle) { }
            }

            if (statement != null)
            {
                try { statement.close(); } catch (SQLException sqle) { }
            }
        }

        if (newID < 0)
        {
            throw new SQLException("Unable to retrieve sequence ID");
        }

        // Set the ID in the table row object
        row.setColumn(getPrimaryKeyColumn(table), newID);
        Collection<ColumnInfo> info = getColumnInfo(table);

        String sql = insertSQL.get(table);
        if (sql == null)
        {
            StringBuilder sqlBuilder = new StringBuilder().append("INSERT INTO ").append(table).append(" ( ");

            boolean firstColumn = true;
            for (ColumnInfo col : info)
            {
                if (firstColumn)
                {
                    sqlBuilder.append(col.getName());
                    firstColumn = false;
                }
                else
                {
                    sqlBuilder.append(",").append(col.getName());
                }
            }

            sqlBuilder.append(") VALUES ( ");

            // Values to insert
            firstColumn = true;
            for (int i = 0; i < info.size(); i++)
            {
                if (firstColumn)
                {
                    sqlBuilder.append("?");
                    firstColumn = false;
                }
                else
                {
                    sqlBuilder.append(",").append("?");
                }
            }

            // Watch the syntax
            sqlBuilder.append(")");
            sql = sqlBuilder.toString();
            insertSQL.put(table, sql);
        }

        execute(context.getDBConnection(), sql, info, row);
        return newID;
    }

    /**
     * Main method used to perform tests on the database
     *
     * @param args The command line arguments
     */
    public static void main(String[] args)
    {
        // Get something from dspace.cfg to get the log lines out the way
        String url = ConfigurationManager.getProperty("db.url");

        // Try to connect to the database
        System.out.println("\nAttempting to connect to database: ");
        System.out.println(" - URL: " + url);
        System.out.println(" - Driver: " + ConfigurationManager.getProperty("db.driver"));
        System.out.println(" - Username: " + ConfigurationManager.getProperty("db.username"));
        System.out.println(" - Password: " + ConfigurationManager.getProperty("db.password"));
        System.out.println(" - Schema: " + ConfigurationManager.getProperty("db.schema"));
        System.out.println("\nTesting connection...");
        try
        {
            Connection connection = DatabaseManager.getConnection();
            connection.close();
        }
        catch (SQLException sqle)
        {
            System.err.println("\nError: ");
            System.err.println(" - " + sqle);
            System.err.println("\nPlease see the DSpace documentation for assistance.\n");
            System.exit(1);
        }

        System.out.println("Connected successfully!\n");
    }

    public static void applyOffsetAndLimit(StringBuffer query, List<Serializable> params, int offset, int limit){
        if(!isOracle()){
            offsetAndLimitPostgresQuery(query,params,offset,limit);
        }else{
            offsetAndLimitOracleQuery(query,params,offset,limit);
        }
    }

    private static void offsetAndLimitPostgresQuery(StringBuffer query , List<Serializable> params, int offset, int limit){
        query.append(" OFFSET ? LIMIT ?");
        params.add(offset);
        params.add(limit);
    }

    private static void offsetAndLimitOracleQuery(StringBuffer query , List<Serializable> params, int offset, int limit)
    {
        // prepare the LIMIT clause
        if (limit > 0 || offset > 0)
        {
            query.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
            query.append(") ");
        }

        if (limit > 0)
        {
            query.append("rec WHERE rownum<=? ");
            if (offset > 0)
            {
                params.add(Integer.valueOf(limit + offset));
            }
            else
            {
                params.add(Integer.valueOf(limit));
            }
        }

        if (offset > 0)
        {
            query.insert(0, "SELECT * FROM (");
            query.append(") WHERE rnum>?");
            params.add(Integer.valueOf(offset));
        }
    }

}
