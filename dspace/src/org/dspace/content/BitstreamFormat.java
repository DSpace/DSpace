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

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;


/**
 * Class representing a particular bitstream format
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class BitstreamFormat
{
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
            "bitstreamtyperegistry",
            id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new BitstreamFormat(context, row);
        }
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
        // FIXME: Check authorisation 
        
        // Create a table row
        TableRow row = DatabaseManager.create(context, "bitstreamtyperegistry");
        return new BitstreamFormat(context, row);
    }


    /**
     * Get the internal identifier of this bitstream format
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return bfRow.getIntColumn("bitstream_type_id");
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
        // FIXME: Check authorisation

        DatabaseManager.update(bfContext, bfRow);
    }
}
