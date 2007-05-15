/*
 * TableRowIterator.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the results of a database query
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class TableRowIterator
{
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
        this.results = results;
        this.table = table;
        statemt = null;
    }

    /**
     * Finalize -- this method is called when this object is GC-ed.
     */
    public void finalize()
    {
        close();
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
     */
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

        return DatabaseManager.process(results, table);
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
     * operations can be preformed on this object.
     * 
     * @return - A list of all the values returned by the iterator.
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public List toList() throws SQLException
    {
        List resultsList = new ArrayList();

        while (hasNext())
        {
            resultsList.add(next());
        }

        // Close the connection after converting it to a list.
        this.close();
        
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
    }
}
