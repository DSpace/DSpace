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
    private static String dbms_keyword;

    /** The static variables which represent the DBMS keyword **/
    public static final String DBMS_POSTGRES="postgres";
    public static final String DBMS_ORACLE="oracle";
    public static final String DBMS_H2="h2";

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

            // Get current database schema name
            String schema = DatabaseUtils.getSchemaName(connection);
            
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
            // Initialize our data source
            dataSource = initDataSource();

            // What brand of DBMS do we have?
            Connection connection = dataSource.getConnection();
            DatabaseMetaData meta = connection.getMetaData();
            dbms = meta.getDatabaseProductName();
            log.info("DBMS is '{}'", dbms);
            log.info("DBMS driver version is '{}'", meta.getDatabaseProductVersion());
            
            // Based on our DBMS type, determine how to categorize it
            dbms_keyword = findDbKeyword(meta);
            if(dbms_keyword!=null && dbms_keyword.equals(DBMS_POSTGRES))
            {
                isPostgres = true;
            }
            else if(dbms_keyword!=null && dbms_keyword.equals(DBMS_ORACLE))
            {
                isOracle = true;
            }
            else if(dbms_keyword!=null && dbms_keyword.equals(DBMS_H2))
            {
                // We set "isOracle=true" for H2 simply because it's NOT 100%
                // PostgreSQL compatible. So, code which is highly PostgreSQL
                // specific often may not work properly on H2.
                // I.e. this acts more like a "isNotPostgreSQL" flag
                isOracle = true;
            }
            else
            {
                log.error("DBMS {} is unsupported", dbms);
            }

            // While technically we have one more step to complete (see below),
            // at this point the DatabaseManager class is initialized so that
            // all its static "get" methods will return values
            initialized = true;

            // FINALLY, ensure database schema is up-to-date.
            // If not, upgrade/migrate database. (NOTE: This needs to run LAST
            // as it may need some of the initialized variables set above)
            DatabaseUtils.updateDatabase(dataSource, connection);

            connection.close();
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
     * Initialize just the DataSource for the DatabaseManager.
     * <P>
     * While this is normally called via initialize() to create the globally
     * shared DataSource, it also may be called individually just to test the
     * Database Connection settings. This second use case often needs to avoid
     * a full initialization/migration of the Database, which takes much longer
     * and may not be necessary just for testing a basic connection. See, for
     * example, DatabaseUtils.main().
     *
     * @return initialized DataSource, or null if could not be initialized
     * @throws SQLException if an initialization error occurs
     */
    protected static DataSource initDataSource()
            throws SQLException
    {
        DataSource dSource = null;

        String jndiName = ConfigurationManager.getProperty("db.jndi");
        if (!StringUtils.isEmpty(jndiName))
        {
            try
            {
                javax.naming.Context ctx = new InitialContext();
                javax.naming.Context env = ctx == null ? null : (javax.naming.Context)ctx.lookup("java:/comp/env");
                dSource = (DataSource)(env == null ? null : env.lookup(jndiName));
            }
            catch (Exception e)
            {
                log.error("Error retrieving JNDI context: " + jndiName, e);
            }

            if (dSource != null)
            {
                log.debug("Using JNDI dataSource: " + jndiName);
            }
            else
            {
                log.info("Unable to locate JNDI dataSource: " + jndiName);
            }
        }

        if (dSource == null)
        {
            if (!StringUtils.isEmpty(jndiName))
            {
                log.info("Falling back to creating own Database pool");
            }

            dSource = DataSourceInit.getDatasource();
        }

        return dSource;
    }

    /**
     * Return the "DbKeyword" for a specific database name.
     * <P>
     * This is mostly a utility method for initialize(), but also comes in
     * handy when you want basic info about the Database but *don't* want
     * to actually fully initialize the DatabaseManager (as it will also
     * run all pending DB migrations)
     *
     * @param meta the DatabaseMetaData
     * @return DB Keyword for this database, or null if not found
     * @throws SQLException if an initialization error occurs
     */
    protected static String findDbKeyword(DatabaseMetaData meta)
            throws SQLException
    {
        String prodName = meta.getDatabaseProductName();
        String dbms_lc = prodName.toLowerCase(Locale.ROOT);
        if (dbms_lc.contains("postgresql"))
        {
            return DBMS_POSTGRES;
        }
        else if (dbms_lc.contains("oracle"))
        {
            return DBMS_ORACLE;
        }
        else if (dbms_lc.contains("h2")) // Used for unit testing only
        {
            return DBMS_H2;
        }
        else
        {
            return null;
        }
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
