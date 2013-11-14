/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

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

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /** The default group of administrators */
    private Group admins;

    // Keys for accessing Community metadata
    public static final String COPYRIGHT_TEXT = "copyright_text";
    public static final String INTRODUCTORY_TEXT = "introductory_text";
    public static final String SHORT_DESCRIPTION = "short_description";
    public static final String SIDEBAR_TEXT = "side_bar_text";

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

        modified = false;
        modifiedMetadata = false;

        admins = groupFromColumn("admin");

        clearDetails();
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
     * Create a new top-level community, with a new ID.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the newly created community
     */
    public static Community create(Community parent, Context context)
            throws SQLException, AuthorizeException
    {
        return create(parent, context, null);
    }

    /**
     * Create a new top-level community, with a new ID.
     *
     * @param context
     *            DSpace context object
     * @param handle the pre-determined Handle to assign to the new community
     *
     * @return the newly created community
     */
    public static Community create(Community parent, Context context, String handle)
            throws SQLException, AuthorizeException
    {
        if (!(AuthorizeManager.isAdmin(context) ||
              (parent != null && AuthorizeManager.authorizeActionBoolean(context, parent, Constants.ADD))))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        TableRow row = DatabaseManager.create(context, "community");
        Community c = new Community(context, row);
        
        try
        {
            c.handle = (handle == null) ?
                       HandleManager.createHandle(context, c) :
                       HandleManager.createHandle(context, c, handle);
        }
        catch(IllegalStateException ie)
        {
            //If an IllegalStateException is thrown, then an existing object is already using this handle
            //Remove the community we just created -- as it is incomplete
            try
            {
                if(c!=null)
                {
                    c.delete();
                }
            } catch(Exception e) { }

            //pass exception on up the chain
            throw ie;
        }

        if(parent != null)
        {
            parent.addSubcommunity(c);
        }

        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = Group.find(context, 0);

        ResourcePolicy myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        context.addEvent(new Event(Event.CREATE, Constants.COMMUNITY, c.getID(), c.handle));

        // if creating a top-level Community, simulate an ADD event at the Site.
        if (parent == null)
        {
            context.addEvent(new Event(Event.ADD, Constants.SITE, Site.SITE_ID, Constants.COMMUNITY, c.getID(), c.handle));
        }

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

        List<Community> communities = new ArrayList<Community>();

        try
        {
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
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

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

        List<Community> topCommunities = new ArrayList<Community>();

        try
        {
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
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

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

    /**
     * @see org.dspace.content.DSpaceObject#getHandle()
     */
    public String getHandle()
    {
        if(handle == null) {
        	try {
				handle = HandleManager.findHandle(this.ourContext, this);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
        }
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
    	String metadata = communityRow.getStringColumn(field);
    	return (metadata == null) ? "" : metadata;
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
        if ((field.trim()).equals("name") 
                && (value == null || value.trim().equals("")))
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
        
        /* 
         * Set metadata field to null if null 
         * and trim strings to eliminate excess
         * whitespace.
         */
        if(value == null)
        {
            communityRow.setColumnNull(field);
        }
        else
        {
            communityRow.setColumn(field, value.trim());
        }
        
        modifiedMetadata = true;
        addDetails(field);
    }

    public String getName()
    {
        return getMetadata("name");
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
            List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(ourContext, this, Constants.READ);
            AuthorizeManager.addPolicies(ourContext, policies, newLogo);

            log.info(LogManager.getHeader(ourContext, "set_logo",
                    "community_id=" + getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        modified = true;
        return logo;
    }

    /**
     * Update the community metadata (including logo) to the database.
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        canEdit();

        log.info(LogManager.getHeader(ourContext, "update_community",
                "community_id=" + getID()));

        DatabaseManager.update(ourContext, communityRow);

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.COMMUNITY, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    /**
     * Create a default administrators group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be administrators.
     * 
     * @return the default group of editors associated with this community
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createAdministrators() throws SQLException, AuthorizeException
    {
        // Check authorisation - Must be an Admin to create more Admins
        AuthorizeUtil.authorizeManageAdminGroup(ourContext, this);

        if (admins == null)
        {
            //turn off authorization so that Community Admins can create Sub-Community Admins
            ourContext.turnOffAuthorisationSystem();
            admins = Group.create(ourContext);
            ourContext.restoreAuthSystemState();
            
            admins.setName("COMMUNITY_" + getID() + "_ADMIN");
            admins.update();
        }

        AuthorizeManager.addPolicy(ourContext, this, Constants.ADMIN, admins);
        
        // register this as the admin group
        communityRow.setColumn("admin", admins.getID());
        
        modified = true;
        return admins;
    }
    
    /**
     * Remove the administrators group, if no group has already been created 
     * then return without error. This will merely dereference the current 
     * administrators group from the community so that it may be deleted 
     * without violating database constraints.
     */
    public void removeAdministrators() throws SQLException, AuthorizeException
    {
        // Check authorisation - Must be an Admin of the parent community (or system admin) to delete Admin group
        AuthorizeUtil.authorizeRemoveAdminGroup(ourContext, this);

        // just return if there is no administrative group.
        if (admins == null)
        {
            return;
        }

        // Remove the link to the community table.
        communityRow.setColumnNull("admin");
        admins = null;
       
        modified = true;
    }

    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * community.
     * <P>
     * The default group of administrators for community 100 is the one called
     * <code>community_100_admin</code>.
     * 
     * @return group of administrators, or <code>null</code> if there is no
     *         default group.
     */
    public Group getAdministrators()
    {
        return admins;
    }

    /**
     * Get the collections in this community. Throws an SQLException because
     * creating a community object won't load in all collections.
     * 
     * @return array of Collection objects
     */
    public Collection[] getCollections() throws SQLException
    {
        List<Collection> collections = new ArrayList<Collection>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
        	ourContext,"collection",
            "SELECT collection.* FROM collection, community2collection WHERE " +
            "community2collection.collection_id=collection.collection_id " +
            "AND community2collection.community_id= ? ORDER BY collection.name",
            getID());

        // Make Collection objects
        try
        {
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
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

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
        List<Community> subcommunities = new ArrayList<Community>();

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                ourContext,"community",
                "SELECT community.* FROM community, community2community WHERE " +
                "community2community.child_comm_id=community.community_id " + 
                "AND community2community.parent_comm_id= ? ORDER BY community.name",
                getID());
        

        // Make Community objects
        try
        {
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
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

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
        try
        {
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
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

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
        List<Community> parentList = new ArrayList<Community>();
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
     * Return an array of collections of this community and its subcommunities
     * 
     * @return an array of collections
     */

    public Collection[] getAllCollections() throws SQLException
    {
        List<Collection> collectionList = new ArrayList<Collection>();
        for (Community subcommunity : getSubcommunities())
        {
            addCollectionList(subcommunity, collectionList);
        }

        for (Collection collection : getCollections())
        {
            collectionList.add(collection);
        }

        // Put them in an array
        Collection[] collectionArray = new Collection[collectionList.size()];
        collectionArray = (Collection[]) collectionList.toArray(collectionArray);

        return collectionArray;

    }
    /**
     * Internal method to process subcommunities recursively
     */
    private void addCollectionList(Community community, List<Collection> collectionList) throws SQLException
    {
        for (Community subcommunity : community.getSubcommunities())
        {
            addCollectionList(subcommunity, collectionList);
        }

        for (Collection collection : community.getCollections())
        {
            collectionList.add(collection);
        }
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
        return createCollection(null);
    }

    /**
     * Create a new collection within this community. The collection is created
     * without any workflow groups or default submitter group.
     *
     * @param handle the pre-determined Handle to assign to the new community
     * @return the new collection
     */
    public Collection createCollection(String handle) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Collection c = Collection.create(ourContext, handle);
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

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("community2collection");

                mappingRow.setColumn("community_id", getID());
                mappingRow.setColumn("collection_id", c.getID());

                ourContext.addEvent(new Event(Event.ADD, Constants.COMMUNITY, getID(), Constants.COLLECTION, c.getID(), c.getHandle()));

                DatabaseManager.insert(ourContext, mappingRow);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
    }

    /**
     * Create a new sub-community within this community.
     * 
     * @return the new community
     */
    public Community createSubcommunity() throws SQLException,
            AuthorizeException
    {
        return createSubcommunity(null);
    }

    /**
     * Create a new sub-community within this community.
     *
     * @param handle the pre-determined Handle to assign to the new community
     * @return the new community
     */
    public Community createSubcommunity(String handle) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Community c = create(this, ourContext, handle);
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

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("community2community");

                mappingRow.setColumn("parent_comm_id", getID());
                mappingRow.setColumn("child_comm_id", c.getID());

                ourContext.addEvent(new Event(Event.ADD, Constants.COMMUNITY, getID(), Constants.COMMUNITY, c.getID(), c.getHandle()));

                DatabaseManager.insert(ourContext, mappingRow);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
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

        // will be the collection an orphan?
        TableRow trow = DatabaseManager.querySingle(ourContext,
                "SELECT COUNT(DISTINCT community_id) AS num FROM community2collection WHERE collection_id= ? ",
                c.getID());
        DatabaseManager.setConstraintDeferred(ourContext, "comm2coll_collection_fk");
        
        if (trow.getLongColumn("num") == 1)
        {
            // Orphan; delete it            
            c.delete();
        }
        
        log.info(LogManager.getHeader(ourContext, "remove_collection",
                "community_id=" + getID() + ",collection_id=" + c.getID()));
        
        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM community2collection WHERE community_id= ? "+
                "AND collection_id= ? ", getID(), c.getID());

        DatabaseManager.setConstraintImmediate(ourContext, "comm2coll_collection_fk");
        
        ourContext.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, getID(), Constants.COLLECTION, c.getID(), c.getHandle()));
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

        // will be the subcommunity an orphan?
        TableRow trow = DatabaseManager.querySingle(ourContext,
                "SELECT COUNT(DISTINCT parent_comm_id) AS num FROM community2community WHERE child_comm_id= ? ",
                c.getID());

        DatabaseManager.setConstraintDeferred(ourContext, "com2com_child_fk");
        if (trow.getLongColumn("num") == 1)
        {
            // Orphan; delete it
            c.rawDelete();
        }

        log.info(LogManager.getHeader(ourContext, "remove_subcommunity",
                "parent_comm_id=" + getID() + ",child_comm_id=" + c.getID()));
        
        // Remove any mappings
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM community2community WHERE parent_comm_id= ? " +
                " AND child_comm_id= ? ", getID(),c.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, getID(), Constants.COMMUNITY, c.getID(), c.getHandle()));
        
        DatabaseManager.setConstraintImmediate(ourContext, "com2com_child_fk");
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
        // will call rawDelete() before removing the linkage
        Community parent = getParentCommunity();

        if (parent != null)
        {
            // remove the subcommunities first
            Community[] subcommunities = getSubcommunities();
            for (int i = 0; i < subcommunities.length; i++)
            {
                subcommunities[i].delete();
            }
            // now let the parent remove the community
            parent.removeSubcommunity(this);

            return;
        }

        rawDelete();        
    }
    
    /**
     * Internal method to remove the community and all its childs from the database without aware of eventually parent  
     */
    private void rawDelete() throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(ourContext, "delete_community",
                "community_id=" + getID()));

        ourContext.addEvent(new Event(Event.DELETE, Constants.COMMUNITY, getID(), getHandle()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove collections
        Collection[] cols = getCollections();

        for (int i = 0; i < cols.length; i++)
        {
            removeCollection(cols[i]);
        }

        // delete subcommunities
        Community[] comms = getSubcommunities();

        for (int j = 0; j < comms.length; j++)
        {
            comms[j].delete();
        }

        // Remove the logo
        setLogo(null);

        // Remove all authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // get rid of the content count cache if it exists
        try
        {
            ItemCounter ic = new ItemCounter(ourContext);
            ic.remove(this);
        }
        catch (ItemCountException e)
        {
            // FIXME: upside down exception handling due to lack of good
            // exception framework
            throw new IllegalStateException(e.getMessage(),e);
        }

        // Remove any Handle
        HandleManager.unbindHandle(ourContext, this);

        // Delete community row
        DatabaseManager.delete(ourContext, communityRow);

        // Remove administrators group - must happen after deleting community
        Group g = getAdministrators();

        if (g != null)
        {
            g.delete();
        }
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

    public int hashCode()
    {
        return new HashCodeBuilder().append(getID()).toHashCode();
    }

    /**
     * Utility method for reading in a group from a group ID in a column. If the
     * column is null, null is returned.
     * 
     * @param col
     *            the column name to read
     * @return the group referred to by that column, or null
     * @throws SQLException
     */
    private Group groupFromColumn(String col) throws SQLException
    {
        if (communityRow.isColumnNull(col))
        {
            return null;
        }

        return Group.find(ourContext, communityRow.getIntColumn(col));
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
    
    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        switch (action)
        {
        case Constants.REMOVE:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = this;
            }
            break;

        case Constants.DELETE:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = getParentCommunity();
            }
            break;
        case Constants.ADD:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation())
            {
                adminObject = this;
            }
            break;
        default:
            adminObject = this;
            break;
        }
        return adminObject;
    }
    
    public DSpaceObject getParentObject() throws SQLException
    {
        Community pCommunity = getParentCommunity();
        if (pCommunity != null)
        {
            return pCommunity;
        }
        else
        {
            return null;
        }       
    }

    @Override
    public void updateLastModified()
    {
        //Also fire a modified event since the community HAS been modified
        ourContext.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, getID(), null));
    }
}
