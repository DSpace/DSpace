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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

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
    /** log4j category */
    private static Category log = Category.getInstance(Community.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow communityRow;

    /** The logo bitstream */
    private Bitstream logo;


    /**
     * Construct a community object from a database row.
     *
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    Community(Context context, TableRow row)
        throws SQLException
    {
        ourContext = context;
        communityRow = row;
        
        // Get the logo bitstream
        if (communityRow.isColumnNull("logo_bitstream_id"))
        {
            logo = null;
        }
        else
        {
            logo = Bitstream.find(ourContext,
                communityRow.getIntColumn("logo_bitstream_id"));
        }
    }


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
        TableRow row = DatabaseManager.find(context,
            "community",
            id);

        if (row==null )
        {
            return null;
        }
        else
        {
            return new Community(context, row);
        }
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
        // FIXME Check authorisation

        TableRow row = DatabaseManager.create(context, "community");
        return new Community(context, row);
    }

    
    /**
     * Get a list of all communities in the system.  These are alphabetically
     * sorted by community name.
     *
     * @param  context  DSpace context object
     *
     * @return  the communities in the system
     */
    public static Community[] getAllCommunities(Context context)
        throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(context,
            "community",
            "SELECT * FROM community ORDER BY name;");
        
        List communities = new ArrayList();
        
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            communities.add(new Community(context, row));
        }

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);
        
        return communityArray;
    }
    
    
    /**
     * Get the internal ID of this collection
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return communityRow.getIntColumn("community_id");
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
        return communityRow.getStringColumn(field);
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
        communityRow.setColumn(field, value);
    }


    /**
     * Get the logo for the community.  <code>null</code> is return if the
     * community does not have a logo.
     *
     * @return the logo of the community, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    
    /**
     * Give the community a logo.  Passing in <code>null</code> removes any
     * existing logo.  Note that <code>update(/code> will need to be called
     * for the change to take effect.  Setting a logo and not calling
     * <code>update</code> later may result in a previous logo lying around
     * as an "orphaned" bitstream.
     *
     * @param  newLogo   the bitstream to use as the new logo
     */
    public void setLogo(Bitstream newLogo)
        throws AuthorizeException, IOException, SQLException
    {
        // FIXME: Check auth

        // First, delete any existing logo
        if (!communityRow.isColumnNull("logo_bitstream_id"))
        {
            logo.delete();
        }

        if (newLogo==null)
        {
            communityRow.setColumnNull("logo_bitstream_id");
        }
        else
        {
            communityRow.setColumn("logo_bitstream_id", newLogo.getID());
        }

        logo = newLogo;
    }
    

    /**
     * Update the community metadata (including logo) to the database.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check auth

        DatabaseManager.update(ourContext, communityRow);
    }


    /**
     * Get the collections in this community.  Throws an SQLException because
     * creating a community object won't load in all collections.
     *
     * @return  List of Collection objects
     */
    public Collection[] getCollections()
        throws SQLException
    {
        List collections = new ArrayList();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "collection",
            "SELECT collection.* FROM collection, community2collection WHERE " +
                "community2collection.collection_id=collection.collection_id AND " +
                "community2collection.community_id=" + getID() + ";");

        // Make Collection objects
        while (tri.hasNext())
        {
            collections.add(new Collection(ourContext, tri.next()));
        }

        // Put them in an array
        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);
        
        return collectionArray;
    }


    /**
     * Add a collection to the community
     *
     * @param c  collection to add
     */
    public void addCollection(Collection c)
        throws SQLException, AuthorizeException
    {
        // FIXME: Check auth

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "community2collection",
            "SELECT * FROM community2collection WHERE community_id=" +
                getID() + " AND collection_id=" + c.getID() + ";");

        if (!tri.hasNext())
        {
            // No existing mapping, so add one
            TableRow mappingRow = DatabaseManager.create(ourContext,
                "community2collection");

            mappingRow.setColumn("community_id", getID());
            mappingRow.setColumn("collection_id", c.getID());

            DatabaseManager.update(ourContext, mappingRow);
        }
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
        // FIXME: Check auth

        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM community2collection WHERE community_id=" +
                getID() + " AND collection_id=" + c.getID() + ";");
    }


    /**
     * Delete the community, including the metadata and logo.  Collections
     * are merely disassociated, they are NOT deleted.
     */
    public void delete()
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME: Check auth

        // Remove any community-collection mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM community2collection WHERE community_id=" +
                getID() + ";");

        // Remove the logo
        setLogo(null);

        // Delete community row
        DatabaseManager.delete(ourContext, communityRow);
    }


    /**
     * Delete the community, and recursively the collections in the community
     * and the contents of those collections.  Collections, items or other
     * objects that are multiply contained (e.g. a collection also in another
     * community) are NOT deleted.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME: Check auth

        // First get collections
        Collection[] collections = getCollections();
        
        // Delete ourselves
        delete();

        // Delete collections if they aren't contained in other collections
        for (int i=0; i<collections.length; i++)
        {
            Community[] communities = collections[i].getCommunities();
            
            if (communities.length == 0)
            {
                // "Orphaned" collection - delete
                collections[i].deleteWithContents();
            }
        }
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
