/*
 * Community.java
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

// NOTES/ISSUES:
//         Transactionally allocate IDs?  I can see a concurrency problem
//         Throw AuthorizeException at create() or just update()?
//         Load all collections into memory?

/**
 * Class representing a community
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Community
{
    /**
     * Get a community from the database.  Loads in the metadata 
     *
     * @param  context  DSpace context object
     * @param  id       ID of the community
     *
     * @return  the community, or null if the ID is invalid.
     */
    public static Community find(Context context, int id)
        throws SQLException
    {
        return null;
    }
    

    /**
     * Create a new community, with a new ID.  Not inserted in database.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created community
     */
    public static Community create(Context context)
        throws SQLException, AuthorizeException
    {
        return null;
    }

    
    /**
     * Get a list of all communities in the system
     *
     * @param  context  DSpace context object
     *
     * @return  the communities in the system
     */
    public static List getAllCommunities(Context context)
        throws SQLException
    {
        return null;
    }
    
    
    /**
     * Get the value of a metadata field
     *
     * @param  field   the name of the metadata field to get
     *
     * @return  the value of the metadata field
     *
     * @exception IllegalArgumentException   if the requested metadata
     *            field doesn't exist
     */
    public String getMetadata(String field)
    {
        return null;
    }


    /**
     * Set a metadata value
     *
     * @param  field   the name of the metadata field to get
     * @param  value   value to set the field to
     *
     * @exception IllegalArgumentException   if the requested metadata
     *            field doesn't exist
     */
    public void setMetadata(String field, String value)
    {
    }


    /**
     * Get the logo for the community.  <code>null</code> is return if the
     * community does not have a logo.
     *
     * @return the logo of the community, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return null;
    }

    
    /**
     * Give the community a logo.  
     *
     * @param is  stream of a JPEG logo, or <code>null</code> for no logo
     */
    public void setLogo(InputStream is)
    {
    }
    

    /**
     * Update the community metadata (including logo) to the database.
     * Inserts if this is a new community.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Get the collections in this community.  Throws an SQLException because
     * creating a community object won't load in all collections.
     *
     * @return  List of Collection objects
     */
    public List getCollections()
        throws SQLException
    {
        return null;
    }


    /**
     * Add a collection to the community
     *
     * @param c  collection to add
     */
    public void addCollection(Collection c)
        throws SQLException, AuthorizeException
    {

    }


    /**
     * Remove a collection.  Does not delete the collection, just the
     * relationship.
     *
     * @param c  collection to remove
     */
    public void removeCollection(Collection c)
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the community, including the metadata and logo.  Collections
     * are merely disassociated, they are NOT deleted.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the community, and recursively the collections in the community
     * and the contents of those collections.  Collections, items or other
     * objects that are multiply contained (e.g. a collection also in another
     * community) are NOT deleted.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Get recent additions to the community.
     *
     * @param  n  the number of recent additions to retrieve.
     *
     * @return  a list of the most recent additions.
     */
    public List getRecentAdditions( int n )
        throws SQLException
    {
        return null;
    }
}
