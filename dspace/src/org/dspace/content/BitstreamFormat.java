/*
 * BitstreamFormat.java
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

package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing a particular bitstream format.
 * <P>
 * Changes to the bitstream format metadata are only written to the database
 * when <code>update</code> is called.
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class BitstreamFormat
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(BitstreamFormat.class);

    /**
     * The "unknown" support level - for bitstream formats that are unknown
     * to the system
     */
    public static final int UNKNOWN = 0;
    
    /**
     * The "known" support level - for bitstream formats that are known to
     * the system, but not fully supported
     */
    public static final int KNOWN = 1;
    
    /**
     * The "supported" support level - for bitstream formats known to the
     * system and fully supported.
     */
    public static final int SUPPORTED = 2;

    /** Our context */
    private Context bfContext;

    /** The row in the table representing this format */
    private TableRow bfRow;


    /**
     * Class constructor for creating a BitstreamFormat object
     * based on the contents of a DB table row.
     *
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    BitstreamFormat(Context context, TableRow row)
    {
        bfContext = context;
        bfRow = row;
    }


    /**
     * Get a bitstream format from the database.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the bitstream format
     *   
     * @return  the bitstream format, or null if the ID is invalid.
     */
    public static BitstreamFormat find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find(context,
            "bitstreamformatregistry",
            id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_bitstream_format",
                    "not_found,bitstream_format_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_bitstream",
                    "bitstream_format_id=" + id));
            }

            return new BitstreamFormat(context, row);
        }
    }

    
    /**
     * Get the generic "unknown" bitstream format.
     *
     * @param  context  DSpace context object
     *   
     * @return  the "unknown" bitstream format.
     *
     * @throws IllegalStateException  
     *               if the "unknown" bitstream format couldn't be found
     */
    public static BitstreamFormat findUnknown(Context context)
        throws SQLException
    {
        TableRow formatRow = DatabaseManager.findByUnique(context,
            "bitstreamformatregistry", "short_description", "Unknown");

        if (formatRow == null)
        {
            throw new IllegalStateException(
                "No `Unknown' bitstream format in registry");
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_bitstream",
                    "bitstream_format_id=" + formatRow.getIntColumn(
                        "bitstream_format_id")));
            }

            return new BitstreamFormat(context, formatRow);
        }
    }
        


    /**
     * Retrieve all bitstream formats from the registry.
     *
     * @param  context  DSpace context object
     *   
     * @return  the bitstream formats.
     */
    public static BitstreamFormat[] findAll(Context context)
        throws SQLException
    {
        List formats = new ArrayList();

        TableRowIterator tri = DatabaseManager.query(context,
            "bitstreamformatregistry",
            "SELECT * FROM bitstreamformatregistry;");

        while (tri.hasNext())
        {
            formats.add(new BitstreamFormat(context, tri.next()));
        }

        // Return the formats as an array
        BitstreamFormat[] formatArray = new BitstreamFormat[formats.size()];
        formatArray = (BitstreamFormat[]) formats.toArray(formatArray);

        return formatArray;
    }

    
    /**
     * Create a new bitstream format
     *
     * @param  context  DSpace context object
     * @return  the newly created BitstreamFormat
     */
    public static BitstreamFormat create(Context context)
        throws SQLException, AuthorizeException
    {
        // Check authorisation - only administrators can create new formats
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                "Only administrators can create bitstream formats");
        }
        
        // Create a table row
        TableRow row = DatabaseManager.create(context,
            "bitstreamformatregistry");

        log.info(LogManager.getHeader(context,
            "create_bitstream_format",
            "bitstream_format_id=" + row.getIntColumn("bitstream_format_id")));

        return new BitstreamFormat(context, row);
    }


    /**
     * Get the internal identifier of this bitstream format
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return bfRow.getIntColumn("bitstream_format_id");
    }


    /**
     * Get a short (one or two word) description of this bitstream format
     *
     * @return  the short description
     */
    public String getShortDescription()
    {
        return bfRow.getStringColumn("short_description");
    }


    /**
     * Set the short description of the bitstream format
     *
     * @param  s   the new short description
     */
    public void setShortDescription(String s)
    {
        bfRow.setColumn("short_description", s);
    }


    /**
     * Get a description of this bitstream format, including full application
     * or format name
     *
     * @return  the description
     */
    public String getDescription()
    {
        return bfRow.getStringColumn("description");
    }


    /**
     * Set the description of the bitstream format
     *
     * @param  s   the new description
     */
    public void setDescription(String s)
    {
        bfRow.setColumn("description", s);
    }


    /**
     * Get the MIME type of this bitstream format, for example
     * <code>text/plain</code>
     *
     * @return  the MIME type
     */
    public String getMIMEType()
    {
        return bfRow.getStringColumn("mimetype");
    }


    /**
     * Set the MIME type of the bitstream format
     *
     * @param  s   the new MIME type
     */
    public void setMIMEType(String s)
    {
        bfRow.setColumn("mimetype", s);
    }


    /**
     * Get the support level for this bitstream format - one of
     * <code>UNKNOWN</code>, <code>KNOWN</code> or <code>SUPPORTED</code>.
     *
     * @return  the support level
     */
    public int getSupportLevel()
    {
        return bfRow.getIntColumn("support_level");
    }


    /**
     * Set the support level for this bitstream format - one of
     * <code>UNKNOWN</code>, <code>KNOWN</code> or <code>SUPPORTED</code>.
     *
     * @param  sl   the new support level
     */
    public void setSupportLevel(int sl)
    {
        // Sanity check
        if (sl < 0 || sl > 2)
        {
            throw new IllegalArgumentException("Invalid support level");
        }
        else
        {
            bfRow.setColumn("support_level", sl);
        }
    }


    /**
     * Find out if the bitstream format is an internal format - that is,
     * one that is used to store system information, rather than the content
     * of items in the system
     *
     * @return <code>true</code> if the bitstream format is an internal
     *         type
     */
    public boolean isInternal()
    {
        return bfRow.getBooleanColumn("internal");
    }


    /**
     * Set whether the bitstream format is an internal format
     *
     * @param  b    pass in <code>true</code> if the bitstream format is an
     *              internal type
     */
    public void setInternal(boolean b)
    {
        bfRow.setColumn("internal", b);
    }


    /**
     * Update the bitstream format metadata
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // Check authorisation - only administrators can change formats
        if (!AuthorizeManager.isAdmin(bfContext))
        {
            throw new AuthorizeException(
                "Only administrators can modify bitstream formats");
        }

        log.info(LogManager.getHeader(bfContext,
            "update_bitstream_format",
            "bitstream_format_id=" + getID()));

        DatabaseManager.update(bfContext, bfRow);
    }


    /**
     * Delete this bitstream format.  This converts the types of any bitstreams
     * that may have this type to "unknown".  Use this with care!
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
        // Check authorisation - only administrators can delete formats
        if (!AuthorizeManager.isAdmin(bfContext))
        {
            throw new AuthorizeException(
                "Only administrators can delete bitstream formats");
        }

        // Find "unknown" type
        BitstreamFormat unknown = findUnknown(bfContext);

        // Set bitstreams with this format to "unknown"
        int numberChanged = DatabaseManager.updateQuery(bfContext,
            "UPDATE bitstreams SET bitstream_format_id=" + unknown.getID() +
                " WHERE bitstream_format_id=" + getID());

        // Delete this format from database
        DatabaseManager.delete(bfContext, bfRow);

        log.info(LogManager.getHeader(bfContext,
            "delete_bitstream_format",
            "bitstream_format_id=" + getID() + ",bitstreams_changed=" +
                numberChanged));
    }
}
