/*
 * DCType.java
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

package org.dspace.administer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing a particular Dublin Core metadata type, with various
 * utility methods.  In general, only used for manipulating the registry of
 * Dublin Core types in the system, so most users will not need this.
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class DCType
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DCType.class);

    /** Our context */
    private Context ourContext;

    /** The row in the table representing this type */
    private TableRow typeRow;


    /**
     * Class constructor for creating a BitstreamFormat object
     * based on the contents of a DB table row.
     *
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    DCType(Context context, TableRow row)
    {
        ourContext = context;
        typeRow = row;
    }


    /**
     * Get a bitstream format from the database.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the bitstream format
     *
     * @return  the bitstream format, or null if the ID is invalid.
     */
    public static DCType find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find(context,
            "dctyperegistry",
            id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_dc_type",
                    "not_found,dc_type_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_dc_type",
                    "dc_type_id=" + id));
            }

            return new DCType(context, row);
        }
    }


    /**
     * Find a given Dublin Core type.  Returns <code>null</code> if the
     * Dublin Core type doesn't exist.
     *
     * @param   context    the DSpace context to use
     * @param   element    the element to find
     * @param   qualifier  the qualifier, or <code>null</code> to find an
     *                     unqualified type
     *
     * @return the Dublin Core type, or <code>null</code> if there isn't a
     *         corresponding type in the registry
     */
    public static DCType findByElement(Context context, String element,
        String qualifier)
        throws SQLException
    {
        String sql = "SELECT * FROM dctyperegistry WHERE element LIKE '" +
            element + "' AND qualifier";

        if (qualifier == null)
        {
            sql = sql + " is null;";
        }
        else
        {
            sql = sql + " LIKE '" + qualifier + "';";
        }

        TableRowIterator tri = DatabaseManager.query(context,
            "dctyperegistry",
            sql);

        // Return the first matching element (if two match there's a problem,
        // but not one dealt with here)
        if (tri.hasNext())
        {
            return new DCType(context, tri.next());
        }
        else
        {
            // No match means there's no corresponding element
            return null;
        }
    }


    /**
     * Retrieve all Dublin Core types from the registry
     *
     * @return  an array of all the Dublin Core types
     */
    public static DCType[] findAll(Context context)
        throws SQLException
    {
        List dcTypes = new ArrayList();

        // Get all the dctyperegistry rows
        TableRowIterator tri = DatabaseManager.query(context,
            "dctyperegistry",
            "SELECT * FROM dctyperegistry ORDER BY element, qualifier;");

        // Make into DC Type objects
        while (tri.hasNext())
        {
            dcTypes.add(new DCType(context, tri.next()));
        }

        // Convert list into an array
        DCType[] typeArray = new DCType[dcTypes.size()];
        typeArray = (DCType[]) dcTypes.toArray(typeArray);

        // Return the array
        return typeArray;
    }


    /**
     * Create a new Dublin Core type
     *
     * @param  context  DSpace context object
     * @return  the newly created DCType
     */
    public static DCType create(Context context)
        throws SQLException, AuthorizeException
    {
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                "Only administrators may modify the Dublin Core registry");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "dctyperegistry");

        log.info(LogManager.getHeader(context,
            "create_dc_type",
            "dc_type_id=" + row.getIntColumn("dc_type_id")));

        return new DCType(context, row);
    }

    
    /**
     * Delete this DC type.  This won't work if there are any DC values
     * in the database of this type - they need to be updated first.
     * An <code>SQLException</code> (referential integrity violation)
     * will be thrown in this case.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(ourContext))
        {
            throw new AuthorizeException(
                "Only administrators may modify the Dublin Core registry");
        }
        
        log.info(LogManager.getHeader(ourContext,
            "delete_dc_type",
            "dc_type_id=" + getID()));

        DatabaseManager.delete(ourContext, typeRow);
    }


    /**
     * Get the internal identifier of this bitstream format
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return typeRow.getIntColumn("dc_type_id");
    }


    /**
     * Get the DC element
     *
     * @return  the element
     */
    public String getElement()
    {
        return typeRow.getStringColumn("element");
    }


    /**
     * Set the DC element
     *
     * @param  s   the new element
     */
    public void setElement(String s)
    {
        typeRow.setColumn("element", s);
    }


    /**
     * Get the DC qualifier, if any.
     *
     * @return  the DC qualifier, or <code>null</code> if this is an
     *          unqualified element
     */
    public String getQualifier()
    {
        return typeRow.getStringColumn("qualifier");
    }


    /**
     * Set the DC qualifier
     *
     * @param s  the DC qualifier, or <code>null</code> if this is an
     *           unqualified element
     */
    public void setQualifier(String s)
    {
        typeRow.setColumn("qualifier", s);
    }


    /**
     * Get the scope note - information about the DC type and its use
     *
     * @return  the scope note
     */
    public String getScopeNote()
    {
        return typeRow.getStringColumn("scope_note");
    }


    /**
     * Set the scope note
     *
     * @param  s   the new scope note
     */
    public void setScopeNote(String s)
    {
        typeRow.setColumn("scope_note", s);
    }


    /**
     * Update the bitstream format metadata
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // Check authorisation: Only admins may update DC types
        if (!AuthorizeManager.isAdmin(ourContext))
        {
            throw new AuthorizeException(
                "Only administrators may modiffy the Dublin Core registry");
        }

        log.info(LogManager.getHeader(ourContext,
            "update_dc_type",
            "dc_type_id=" + getID()));

        DatabaseManager.update(ourContext, typeRow);
    }
}
