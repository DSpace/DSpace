/*
 * Bitstream.java
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

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;


/**
 * Class representing bitstreams stored in the DSpace system
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Bitstream
{
    /**
     * Get a bitstream from the database.  The bitstream metadata is
     * loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the bitstream
     *   
     * @return  the bitstream, or null if the ID is invalid.
     */
    public static Bitstream find(Context context, int id)
        throws SQLException
    {
        return null;
    }
    

    /**
     * Create a new bitstream, with a new ID.  The checksum and file size
     * are calculated.
     *
     * @param  context   DSpace context object
     * @param  is        the bits to put in the bitstream
     *
     * @return  the newly created bundle
     */
    public static Bitstream create(Context context, InputStream is)
        throws AuthorizeException, IOException
    {
        return null;
    }


    /**
     * Get the name of this bitstream - typically the filename, without
     * any path information
     *
     * @return  the name of the bitstream
     */
    public String getName()
    {
        return null;
    }


    /**
     * Set the name of the bitstream
     *
     * @param  n   the new name of the bitstream
     */
    public void setName(String n)
    {
    }


    /**
     * Get the source of this bitstream - typically the filename with
     * path information (if originally provided) or the name of the tool
     * that generated this bitstream
     *
     * @return  the source of the bitstream
     */
    public String getSource()
    {
        return null;
    }


    /**
     * Set the source of the bitstream
     *
     * @param  n   the new source of the bitstream
     */
    public void setSource(String n)
    {
    }


    /**
     * Get the description of this bitstream - optional free text, typically
     * provided by a user at submission time
     *
     * @return  the description of the bitstream
     */
    public String getDescription()
    {
        return null;
    }


    /**
     * Set the description of the bitstream
     *
     * @param  n   the new description of the bitstream
     */
    public void setDescription(String n)
    {
    }

    
    /**
     * Get the checksum of the content of the bitstream, for integrity checking
     *
     * @return the checksum
     */
    public String getChecksum()
    {
        return null;
    }

    
    /**
     * Get the algorithm used to calculate the checksum
     *
     * @return the algorithm, e.g. "MD5"
     */
    public String getChecksumAlgorithm()
    {
        return null;
    }

    
    /**
     * Set the user's format description.  This implies that the format of the
     * bitstream is uncertain, and the format is set to "unknown."
     *
     * @param desc   the user's description of the format
     */
    public void setUserFormatDescription(String desc)
    {
    }


    /**
     * Get the user's format description.  Returns null if the format is known
     * by the system.
     *
     * @return the user's format description.
     */
    public String getUserFormatDescription()
    {
        return null;
    }
    

    /**
     * Get the description of the format - either the user's or the description
     * of the format defined by the system.
     *
     * @return a description of the format.
     */
    public String getFormatDescription()
    {
        return null;
    }
    

    /**
     * Set the format of the bitstream.  If the user has supplied a type
     * description, it is cleared.  Passing in <code>null</code> sets the
     * type of this bitstream to "unknown".
     *
     * @param  f  the format of this bitstream, or <code>null</code> for
     *            unknown
     */
    public void setFormat(BitstreamFormat f)
    {
    }


    /**
     * Attempt to automatically recognise the bitstream's format.
     * <code>null</code> is returned if the format is unknown.
     *
     * @return  the system's guess of the bitstream's format.
     */
    public BitstreamFormat identifyFormat()
    {
        return null;
    }
    

    /**
     * Update the bitstream metadata.  Note that the content of the bitstream
     * cannot be changed - for that you need to create a new bitstream.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the bitstream.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
    }
    

    /**
     * Retrieve the contents of the bitstream
     *
     * @return   a stream from which the bitstream can be read.
     */
    public InputStream retrieve()
        throws IOException
    {
        return null;
    }


    /**
     * Get the bundles this bitstream appears in
     *
     * @return  <code>List</code> of <code>Bundle</code>s this bitstream
     *          appears in
     */
    public List getBundles()
    {
        return null;
    }
}
