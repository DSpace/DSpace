/*
 * Community.java
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
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing a community
 * <P>
 * The community's metadata (name, introductory text etc.) is loaded into'
 * memory. Changes to this metadata are only reflected in the database after
 * <code>update</code> is called.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Community extends DSpaceObject
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Community.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow communityRow;

    /** The logo bitstream */
    private Bitstream logo;

    /** Handle, if any */
    private String handle;

    /**
     * Construct a community object from a database row.
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    Community(Context context, TableRow row) throws SQLException
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
            logo = Bitstream.find(ourContext, communityRow
                    .getIntColumn("logo_bitstream_id"));
        }

        // Get our Handle if any
        handle = HandleManager.findHandle(context, this);

        // Cache ourselves
        context.cache(this, row.getIntColumn("community_id"));
    }

    /**
     * Get a community from the database. Loads in the metadata
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the community
     * 
     * @return the community, or null if the ID is invalid.
     */
    public static Community find(Context context, int id) throws SQLException
    {
        // First check the cache
        Community fromCache = (Community) context
                .fromCache(Community.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "community", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_community",
                        "not_found,community_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_community",
                        "community_id=" + id));
            }

            return new Community(context, row);
        }
    }

    /**
     * Create a new community, with a new ID.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the newly created community
     */
    public static Community create(Community parent, Context context)
            throws SQLException, AuthorizeException
    {
        // Only administrators and adders can create communities
        if (!(AuthorizeManager.isAdmin(context) || AuthorizeManager
                .authorizeActionBoolean(context, parent, Constants.ADD)))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        TableRow row = DatabaseManager.create(context, "community");
        Community c = new Community(context, row);
        c.handle = HandleManager.createHandle(context, c);

        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = Group.find(context, 0);

        ResourcePolicy myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        HistoryManager.saveHistory(context, c, HistoryManager.CREATE, context
                .getCurrentUser(), context.getExtraLogInfo());

        log.info(LogManager.getHeader(context, "create_community",
                "community_id=" + row.getIntColumn("community_id"))
                + ",handle=" + c.handle);

        return c;
    }

    /**
     * Get a list of all communities in the system. These are alphabetically
     * sorted by community name.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the communities in the system
     */
    public static Community[] findAll(Context context) throws SQLException
    {
        TableRowIterator tri = DatabaseManager.queryTable(context, "community",
                "SELECT * FROM community ORDER BY name");

        List communities = new ArrayList();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) context.fromCache(
                    Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                communities.add(fromCache);
            }
            else
            {
                communities.add(new Community(context, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Get a list of all top-level communities in the system. These are
     * alphabetically sorted by community name. A top-level community is one
     * without a parent community.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the top-level communities in the system
     */
    public static Community[] findAllTop(Context context) throws SQLException
    {
        // get all communities that are not children
        TableRowIterator tri = DatabaseManager.queryTable(context, "community",
                "SELECT * FROM community WHERE NOT community_id IN "
                        + "(SELECT child_comm_id FROM community2community) "
                        + "ORDER BY name");

        List topCommunities = new ArrayList();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) context.fromCache(
                    Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                topCommunities.add(fromCache);
            }
            else
            {
                topCommunities.add(new Community(context, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        Community[] communityArray = new Community[topCommunities.size()];
        communityArray = (Community[]) topCommunities.toArray(communityArray);

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

    public String getHandle()
    {
        return handle;
    }

    /**
     * Get the value of a metadata field
     * 
     * @param field
     *            the name of the metadata field to get
     * 
     * @return the value of the metadata field
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public String getMetadata(String field)
    {
        return communityRow.getStringColumn(field);
    }

    /**
     * Set a metadata value
     * 
     * @param field
     *            the name of the metadata field to get
     * @param value
     *            value to set the field to
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     * @exception MissingResourceException
     */
    public void setMetadata(String field, String value)throws MissingResourceException
    {
        if ((field.trim()).equals("name") && (value.trim()).equals(""))
        {
            try
            {
                value = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }
        communityRow.setColumn(field, value);
    }

    /**
     * Get the logo for the community. <code>null</code> is return if the
     * community does not have a logo.
     * 
     * @return the logo of the community, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    /**
     * Give the community a logo. Passing in <code>null</code> removes any
     * existing logo. You will need to set the format of the new logo bitstream
     * before it will work, for example to "JPEG". Note that
     * <code>update(/code> will need to be called for the change to take
     * effect.  Setting a logo and not calling <code>update</code> later may
     * result in a previous logo lying around as an "orphaned" bitstream.
     *
     * @param  is   the stream to use as the new logo
     *
     * @return   the new logo bitstream, or <code>null</code> if there is no
     *           logo (<code>null</code> was passed in)
     */
    public Bitstream setLogo(InputStream is) throws AuthorizeException,
            IOException, SQLException
    {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && AuthorizeManager.authorizeActionBoolean(
                ourContext, this, Constants.DELETE)))
        {
            canEdit();
        }

        // First, delete any existing logo
        if (logo != null)
        {
            log.info(LogManager.getHeader(ourContext, "remove_logo",
                    "community_id=" + getID()));
            communityRow.setColumnNull("logo_bitstream_id");
            logo.delete();
            logo = null;
        }

        if (is != null)
        {
            Bitstream newLogo = Bitstream.create(ourContext, is);
            communityRow.setColumn("logo_bitstream_id", newLogo.getID());
            logo = newLogo;

            // now create policy for logo bitstream
            // to match our READ policy
            List policies = AuthorizeManager.getPoliciesActionFilter(
                    ourContext, this, Constants.READ);
            AuthorizeManager.addPolicies(ourContext, policies, newLogo);

            log.info(LogManager.getHeader(ourContext, "set_logo",
                    "community_id=" + getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        return logo;
    }

    /**
     * Update the community metadata (including logo) to the database.
     */
    public void update() throws SQLException, IOException, AuthorizeException
    {
        // Check authorisation
        canEdit();

        HistoryManager.saveHistory(ourContext, this, HistoryManager.MODIFY,
                ourContext.getCurrentUser(), ourContext.getExtraLogInfo());

        log.info(LogManager.getHeader(ourContext, "update_community",
                "community_id=" + getID()));

        DatabaseManager.update(ourContext, communityRow);

        // now re-index this Community
        DSIndexer.reIndexContent(ourContext, this);
    }

    /**
     * Get the collections in this community. Throws an SQLException because
     * creating a community object won't load in all collections.
     * 
     * @return array of Collection objects
     */
    public Collection[] getCollections() throws SQLException
    {
        List collections = new ArrayList();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
        	ourContext,"collection",
            "SELECT collection.* FROM collection, community2collection WHERE " +
            "community2collection.collection_id=collection.collection_id " +
            "AND community2collection.community_id= ? ORDER BY collection.name",
            getID());

        // Make Collection objects
        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Collection fromCache = (Collection) ourContext.fromCache(
                    Collection.class, row.getIntColumn("collection_id"));

            if (fromCache != null)
            {
                collections.add(fromCache);
            }
            else
            {
                collections.add(new Collection(ourContext, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Put them in an array
        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }

    /**
     * Get the immediate sub-communities of this community. Throws an
     * SQLException because creating a community object won't load in all
     * collections.
     * 
     * @return array of Community objects
     */
    public Community[] getSubcommunities() throws SQLException
    {
        List subcommunities = new ArrayList();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                ourContext,"community",
                "SELECT community.* FROM community, community2community WHERE " +
                "community2community.child_comm_id=community.community_id " + 
                "AND community2community.parent_comm_id= ? ORDER BY community.name",
                getID());
        

        // Make Community objects
        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) ourContext.fromCache(
                    Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                subcommunities.add(fromCache);
            }
            else
            {
                subcommunities.add(new Community(ourContext, row));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Put them in an array
        Community[] communityArray = new Community[subcommunities.size()];
        communityArray = (Community[]) subcommunities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Return the parent community of this community, or null if the community
     * is top-level
     * 
     * @return the immediate parent community, or null if top-level
     */
    public Community getParentCommunity() throws SQLException
    {
        Community parentCommunity = null;

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                ourContext,"community",
                "SELECT community.* FROM community, community2community WHERE " +
                "community2community.parent_comm_id=community.community_id " +
                "AND community2community.child_comm_id= ? ",
                getID());
        
        // Make Community object
        if (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) ourContext.fromCache(
                    Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                parentCommunity = fromCache;
            }
            else
            {
                parentCommunity = new Community(ourContext, row);
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        return parentCommunity;
    }

    /**
     * Return an array of parent communities of this community, in ascending
     * order. If community is top-level, return an empty array.
     * 
     * @return an array of parent communities, empty if top-level
     */
    public Community[] getAllParents() throws SQLException
    {
        List parentList = new ArrayList();
        Community parent = getParentCommunity();

        while (parent != null)
        {
            parentList.add(parent);
            parent = parent.getParentCommunity();
        }

        // Put them in an array
        Community[] communityArray = new Community[parentList.size()];
        communityArray = (Community[]) parentList.toArray(communityArray);

        return communityArray;
    }

    /**
     * Create a new collection within this community. The collection is created
     * without any workflow groups or default submitter group.
     * 
     * @return the new collection
     */
    public Collection createCollection() throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Collection c = Collection.create(ourContext);
        addCollection(c);

        return c;
    }

    /**
     * Add an exisiting collection to the community
     * 
     * @param c
     *            collection to add
     */
    public void addCollection(Collection c) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_collection",
                "community_id=" + getID() + ",collection_id=" + c.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,
                "community2collection",
                "SELECT * FROM community2collection WHERE " +
                "community_id= ? AND collection_id= ? ",getID(),c.getID());
        
        if (!tri.hasNext())
        {
            // No existing mapping, so add one
            TableRow mappingRow = DatabaseManager.create(ourContext,
                    "community2collection");

            mappingRow.setColumn("community_id", getID());
            mappingRow.setColumn("collection_id", c.getID());

            DatabaseManager.update(ourContext, mappingRow);
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     * Create a new sub-community within this community.
     * 
     * @return the new community
     */
    public Community createSubcommunity() throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Community c = create(this, ourContext);
        addSubcommunity(c);

        return c;
    }

    /**
     * Add an exisiting community as a subcommunity to the community
     * 
     * @param c
     *            subcommunity to add
     */
    public void addSubcommunity(Community c) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_subcommunity",
                "parent_comm_id=" + getID() + ",child_comm_id=" + c.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,
                "community2community",
                "SELECT * FROM community2community WHERE parent_comm_id= ? "+
                "AND child_comm_id= ? ",getID(), c.getID());
        
        if (!tri.hasNext())
        {
            // No existing mapping, so add one
            TableRow mappingRow = DatabaseManager.create(ourContext,
                    "community2community");

            mappingRow.setColumn("parent_comm_id", getID());
            mappingRow.setColumn("child_comm_id", c.getID());

            DatabaseManager.update(ourContext, mappingRow);
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     * Remove a collection. Any items then orphaned are deleted.
     * 
     * @param c
     *            collection to remove
     */
    public void removeCollection(Collection c) throws SQLException,
            AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_collection",
                "community_id=" + getID() + ",collection_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM community2collection WHERE community_id= ? "+
                "AND collection_id= ? ", getID(), c.getID());

        // Is the community an orphan?
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM community2collection WHERE collection_id= ? ",
                c.getID());

        if (!tri.hasNext())
        {
            //make the right to remove the collection explicit because the
            // implicit relation
            //has been removed. This only has to concern the currentUser
            // because
            //he started the removal process and he will end it too.
            //also add right to remove from the collection to remove it's
            // items.
            AuthorizeManager.addPolicy(ourContext, c, Constants.DELETE,
                    ourContext.getCurrentUser());
            AuthorizeManager.addPolicy(ourContext, c, Constants.REMOVE,
                    ourContext.getCurrentUser());

            // Orphan; delete it
            c.delete();
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     * Remove a subcommunity. Any substructure then orphaned is deleted.
     * 
     * @param c
     *            subcommunity to remove
     */
    public void removeSubcommunity(Community c) throws SQLException,
            AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_subcommunity",
                "parent_comm_id=" + getID() + ",child_comm_id=" + c.getID()));

        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM community2community WHERE parent_comm_id= ? " +
                " AND child_comm_id= ? ", getID(),c.getID());

        // Is the subcommunity an orphan?
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM community2community WHERE child_comm_id= ? ",
                c.getID());

        if (!tri.hasNext())
        {
            //make the right to remove the sub explicit because the implicit
            // relation
            //has been removed. This only has to concern the currentUser
            // because
            //he started the removal process and he will end it too.
            //also add right to remove from the subcommunity to remove it's
            // children.
            AuthorizeManager.addPolicy(ourContext, c, Constants.DELETE,
                    ourContext.getCurrentUser());
            AuthorizeManager.addPolicy(ourContext, c, Constants.REMOVE,
                    ourContext.getCurrentUser());

            // Orphan; delete it
            c.delete();
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     * Delete the community, including the metadata and logo. Collections and
     * subcommunities that are then orphans are deleted.
     */
    public void delete() throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation
        // FIXME: If this was a subcommunity, it is first removed from it's
        // parent.
        // This means the parentCommunity == null
        // But since this is also the case for top-level communities, we would
        // give everyone rights to remove the top-level communities.
        // The same problem occurs in removing the logo
        if (!AuthorizeManager.authorizeActionBoolean(ourContext,
                getParentCommunity(), Constants.REMOVE))
        {
            AuthorizeManager
                    .authorizeAction(ourContext, this, Constants.DELETE);
        }

        // If not a top-level community, have parent remove me; this
        // will call delete() after removing the linkage
        Community parent = getParentCommunity();

        if (parent != null)
        {
            parent.removeSubcommunity(this);

            return;
        }

        log.info(LogManager.getHeader(ourContext, "delete_community",
                "community_id=" + getID()));

        // remove from the search index
        DSIndexer.unIndexContent(ourContext, this);

        HistoryManager.saveHistory(ourContext, this, HistoryManager.REMOVE,
                ourContext.getCurrentUser(), ourContext.getExtraLogInfo());

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove collections
        Collection[] cols = getCollections();

        for (int i = 0; i < cols.length; i++)
        {
            removeCollection(cols[i]);
        }

        // Remove subcommunities
        Community[] comms = getSubcommunities();

        for (int j = 0; j < comms.length; j++)
        {
            removeSubcommunity(comms[j]);
        }

        // Remove the logo
        setLogo(null);

        // Remove all authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Delete community row
        DatabaseManager.delete(ourContext, communityRow);
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Community
     * as this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same
     *         community as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Community))
        {
            return false;
        }

        return (getID() == ((Community) other).getID());
    }

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.COMMUNITY;
    }

    /**
     * return TRUE if context's user can edit community, false otherwise
     * 
     * @return boolean true = current user can edit community
     */
    public boolean canEditBoolean() throws java.sql.SQLException
    {
        try
        {
            canEdit();

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    public void canEdit() throws AuthorizeException, SQLException
    {
        Community[] parents = getAllParents();

        for (int i = 0; i < parents.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.WRITE))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.ADD))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
    }

	/**
     * counts items in this community
     *
     * @return  total items
     */
    public int countItems() throws SQLException
    {       
    	int total = 0;
    	// add collection counts
        Collection[] cols = getCollections();
        for ( int i = 0; i < cols.length; i++)
        {
        	total += cols[i].countItems();
        }
        // add sub-community counts
        Community[] comms = getSubcommunities();
        for ( int j = 0; j < comms.length; j++ )
        {
        	total += comms[j].countItems();
        }
        return total;
    }
}
