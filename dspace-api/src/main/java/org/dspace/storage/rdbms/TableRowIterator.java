/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the results of a database query
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class TableRowIterator
{
    private static final Logger log = Logger.getLogger(TableRowIterator.class);
    /**
     * Results from a query
     */
    private ResultSet results;

    /**
     * Statement used to submit the query
     */
    private Statement statemt = null;

    /**
     * The name of the RDBMS table
     */
    private String table;

    /**
     * True if there is a next row
     */
    private boolean hasNext = true;

    /**
     * True if we have already advanced to the next row.
     */
    private boolean hasAdvanced = false;

    /**
     * Column names for the results in this table
     */
    List<String> columnNames = null;

    /**
     * Constructor
     * 
     * @param results -
     *            A JDBC ResultSet
     */
    TableRowIterator(ResultSet results)
    {
        this(results, null);
        statemt = null;
    }

    /**
     * Constructor
     * 
     * @param results -
     *            A JDBC ResultSet
     * @param table -
     *            The name of the table
     */
    TableRowIterator(ResultSet results, String table)
    {
        this(results, table, null);
        statemt = null;
    }

    TableRowIterator(ResultSet results, String table, List<String> columnNames)
    {
        this.results = results;
        this.table = table;
        if (columnNames == null)
        {
            try
            {
                this.columnNames = (table == null) ? DatabaseManager.getColumnNames(results.getMetaData()) : DatabaseManager.getColumnNames(table);
            }
            catch (SQLException e)
            {
                this.columnNames = null;
            }
        }
        else
        {
            this.columnNames = Collections.unmodifiableList(columnNames);
        }
        
        statemt = null;
    }

    /**
     * Finalize -- this method is called when this object is GC-ed.
     */
    protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }

    /**
     * setStatement -- this method saves the statement used to do the query. We
     * must keep this so that the statement can be closed when we are finished.
     * 
     * @param st -
     *            The statement used to do the query that created this
     *            TableRowIterator
     */
    public void setStatement(Statement st)
    {
        statemt = st;
    }

    /**
     * Advance to the next row and return it. Returns null if there are no more
     * rows.
     * 
     * @return - The next row, or null if no more rows
     * @exception SQLException -
     *                If a database error occurs while fetching values
     * @deprecated use {@link #next(org.dspace.core.Context)} instead. Pass an existing database connection to this method to prevent opening a new one.
     */
    @Deprecated
    public TableRow next() throws SQLException
    {
        if (results == null)
        {
            return null;
        }

        if (!hasNext())
        {
            return null;
        }

        hasAdvanced = false;

        return DatabaseManager.process(results, table, columnNames);
    }

    /**
     * Advance to the next row and return it. Returns null if there are no more
     * rows.
     *
     * @param context An existing database connection to reuse
     * @return - The next row, or null if no more rows
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public TableRow next(Context context) throws SQLException
    {
        if (results == null)
        {
            return null;
        }

        if (!hasNext())
        {
            return null;
        }

        hasAdvanced = false;

        return DatabaseManager.process(context, results, table, columnNames);
    }

    /**
     * Return true if there are more rows, false otherwise
     * 
     * @return - true if there are more rows, false otherwise
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public boolean hasNext() throws SQLException
    {
        if (results == null)
        {
            close();
            return false;
        }

        if (hasAdvanced)
        {
            return hasNext;
        }

        hasAdvanced = true;
        hasNext = results.next();

        // No more results
        if (!hasNext)
        {
            close();
        }

        return hasNext;
    }

    /**
     * Saves all the values returned by iterator into a list.
     * 
     * As a side effect the result set is closed and no more 
     * operations can be performed on this object.
     * 
     * @return - A list of all the values returned by the iterator.
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public List<TableRow> toList() throws SQLException
    {
        List<TableRow> resultsList = new ArrayList<TableRow>();

        try
        {
            while (hasNext())
            {
                resultsList.add(next());
            }
        }
        finally
        {
            // Close the connection after converting it to a list.
            this.close();
        }
        
        return resultsList;
    }

    /**
     * Close the Iterator and release any associated resources
     */
    public void close()
    {
        try
        {
            if (results != null)
            {
                results.close();
                results = null;
            }
        }
        catch (SQLException sqle)
        {
        }

        // try to close the statement if we have one
        try
        {
            if (statemt != null)
            {
                statemt.close();
                statemt = null;
            }
        }
        catch (SQLException sqle)
        {
        }

        columnNames = null;
    }
}
