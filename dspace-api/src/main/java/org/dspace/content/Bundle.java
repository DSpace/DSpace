/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing bundles of bitstreams stored in the DSpace system
 * <P>
 * The corresponding Bitstream objects are loaded into memory. At present, there
 * is no metadata associated with bundles - they are simple containers. Thus,
 * the <code>update</code> method doesn't do much yet. Creating, adding or
 * removing bitstreams has instant effect in the database.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Bundle extends DSpaceObject
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(Bundle.class);

    /** The table row corresponding to this bundle */
    private final TableRow bundleRow;

    /** The bitstreams in this bundle */
    private List<Bitstream> bitstreams;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /**
     * Construct a bundle object with the given table row
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    Bundle(Context context, TableRow row) throws SQLException
    {
        super(context);

        // Ensure that my TableRow is typed.
        if (null == row.getTable())
            row.setTable("bundle");

        bundleRow = row;
        bitstreams = new ArrayList<Bitstream>();
        String bitstreamOrderingField  = ConfigurationManager.getProperty("webui.bitstream.order.field");
        String bitstreamOrderingDirection   = ConfigurationManager.getProperty("webui.bitstream.order.direction");

        if (bitstreamOrderingField == null)
        {
            bitstreamOrderingField = "sequence_id";
        }

        if (bitstreamOrderingDirection == null)
        {
            bitstreamOrderingDirection = "ASC";
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT bitstream.*,bundle2bitstream.bitstream_order FROM bitstream, bundle2bitstream WHERE");
        query.append(" bundle2bitstream.bitstream_id=bitstream.bitstream_id AND");
        query.append(" bundle2bitstream.bundle_id= ?");
        query.append(" ORDER BY ");
        query.append(bitstreamOrderingField);
        query.append(" ");
        query.append(bitstreamOrderingDirection);

        // Get bitstreams
        TableRowIterator tri = DatabaseManager.query(
                ourContext,
                query.toString(),
                bundleRow.getIntColumn("bundle_id"));

        try
        {
            while (tri.hasNext())
            {
                TableRow r = tri.next();

                // First check the cache
                Bitstream fromCache = (Bitstream) context.fromCache(
                        Bitstream.class, r.getIntColumn("bitstream_id"));

                if (fromCache != null)
                {
                    bitstreams.add(fromCache);
                }
                else
                {
                    //Since bitstreams can be ordered by a column in bundle2bitstream
                    //We cannot use queryTable & so we need to add our table later on
                    r.setTable("bitstream");
                    bitstreams.add(new Bitstream(ourContext, r));
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

        // Cache ourselves
        context.cache(this, row.getIntColumn("bundle_id"));

        modified = false;
    }

    /**
     * Get a bundle from the database. The bundle and bitstream metadata are all
     * loaded into memory.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the bundle
     * 
     * @return the bundle, or null if the ID is invalid.
     */
    public static Bundle find(Context context, int id) throws SQLException
    {
        // First check the cache
        Bundle fromCache = (Bundle) context.fromCache(Bundle.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "bundle", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_bundle",
                        "not_found,bundle_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_bundle",
                        "bundle_id=" + id));
            }

            return new Bundle(context, row);
        }
    }

    /**
     * Create a new bundle, with a new ID. This method is not public, since
     * bundles need to be created within the context of an item. For this
     * reason, authorisation is also not checked; that is the responsibility of
     * the caller.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the newly created bundle
     */
    static Bundle create(Context context) throws SQLException
    {
        // Create a table row
        TableRow row = DatabaseManager.create(context, "bundle");

        log.info(LogManager.getHeader(context, "create_bundle", "bundle_id="
                + row.getIntColumn("bundle_id")));

        // if we ever use the identifier service for bundles, we should
        // create the bundle before we create the Event and should add all 
        // identifiers to it.
        context.addEvent(new Event(Event.CREATE, Constants.BUNDLE, row.getIntColumn("bundle_id"), null));

        return new Bundle(context, row);
    }

    /**
     * Get the internal identifier of this bundle
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return bundleRow.getIntColumn("bundle_id");
    }

    /**
     * Get the name of the bundle
     * 
     * @return name of the bundle (ORIGINAL, TEXT, THUMBNAIL) or NULL if not set
     */
    public String getName()
    {
        return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
    }

    /**
     * Set the name of the bundle
     * 
     * @param name
     *            string name of the bundle (ORIGINAL, TEXT, THUMBNAIL) are the
     *            values currently used
     */
    public void setName(String name)
    {
        setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "title", null, null, name);
    }

    /**
     * Get the primary bitstream ID of the bundle
     * 
     * @return primary bitstream ID or -1 if not set
     */
    public int getPrimaryBitstreamID()
    {
        return bundleRow.getIntColumn("primary_bitstream_id");
    }

    /**
     * Set the primary bitstream ID of the bundle
     * 
     * @param bitstreamID
     *            int ID of primary bitstream (e.g. index html file)
     */
    public void setPrimaryBitstreamID(int bitstreamID)
    {
        bundleRow.setColumn("primary_bitstream_id", bitstreamID);
        modified = true;
    }

    /**
     * Unset the primary bitstream ID of the bundle
     */
    public void unsetPrimaryBitstreamID()
    {
    	bundleRow.setColumnNull("primary_bitstream_id");
    }
    
    public String getHandle()
    {
        // No Handles for bundles
        return null;
    }

    /**
     * @param name
     *            name of the bitstream you're looking for
     * 
     * @return the bitstream or null if not found
     */
    public Bitstream getBitstreamByName(String name)
    {
        Bitstream target = null;

        Iterator i = bitstreams.iterator();

        while (i.hasNext())
        {
            Bitstream b = (Bitstream) i.next();

            if (name.equals(b.getName()))
            {
                target = b;

                break;
            }
        }

        return target;
    }

    /**
     * Get the bitstreams in this bundle
     * 
     * @return the bitstreams
     */
    public Bitstream[] getBitstreams()
    {
        Bitstream[] bitstreamArray = new Bitstream[bitstreams.size()];
        bitstreamArray = (Bitstream[]) bitstreams.toArray(bitstreamArray);

        return bitstreamArray;
    }

    /**
     * Get the items this bundle appears in
     * 
     * @return array of <code>Item</code> s this bundle appears in
     */
    public Item[] getItems() throws SQLException
    {
        List<Item> items = new ArrayList<Item>();

        // Get items
        TableRowIterator tri = DatabaseManager.queryTable(
        		ourContext, "item",
                "SELECT item.* FROM item, item2bundle WHERE " +
                "item2bundle.item_id=item.item_id AND " +
                "item2bundle.bundle_id= ? ",
                bundleRow.getIntColumn("bundle_id"));

        try
        {
            while (tri.hasNext())
            {
                TableRow r = (TableRow) tri.next();

                // Used cached copy if there is one
                Item fromCache = (Item) ourContext.fromCache(Item.class, r
                        .getIntColumn("item_id"));

                if (fromCache != null)
                {
                    items.add(fromCache);
                }
                else
                {
                    items.add(new Item(ourContext, r));
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

        Item[] itemArray = new Item[items.size()];
        itemArray = (Item[]) items.toArray(itemArray);

        return itemArray;
    }

    /**
     * Create a new bitstream in this bundle.
     * 
     * @param is
     *            the stream to read the new bitstream from
     * 
     * @return the newly created bitstream
     */
    public Bitstream createBitstream(InputStream is) throws AuthorizeException,
            IOException, SQLException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Bitstream b = Bitstream.create(ourContext, is);

        // FIXME: Set permissions for bitstream
        addBitstream(b);

        return b;
    }

    /**
     * Create a new bitstream in this bundle. This method is for registering
     * bitstreams.
     *
     * @param assetstore corresponds to an assetstore in dspace.cfg
     * @param bitstreamPath the path and filename relative to the assetstore 
     * @return  the newly created bitstream
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream registerBitstream(int assetstore, String bitstreamPath)
        throws AuthorizeException, IOException, SQLException
    {
        // check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Bitstream b = Bitstream.register(ourContext, assetstore, bitstreamPath);

        // FIXME: Set permissions for bitstream

        addBitstream(b);
        return b;
    }

    /**
     * Add an existing bitstream to this bundle
     * 
     * @param b
     *            the bitstream to add
     */
    public void addBitstream(Bitstream b) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_bitstream", "bundle_id="
                + getID() + ",bitstream_id=" + b.getID()));

        // First check that the bitstream isn't already in the list
        for (int i = 0; i < bitstreams.size(); i++)
        {
            Bitstream existing = (Bitstream) bitstreams.get(i);

            if (b.getID() == existing.getID())
            {
                // Bitstream is already there; no change
                return;
            }
        }

        // Add the bitstream object
        bitstreams.add(b);

        ourContext.addEvent(new Event(Event.ADD, Constants.BUNDLE, getID(), 
                Constants.BITSTREAM, b.getID(), String.valueOf(b.getSequenceID()),
                getIdentifiers(ourContext)));

        // copy authorization policies from bundle to bitstream
        // FIXME: multiple inclusion is affected by this...
        AuthorizeManager.inheritPolicies(ourContext, this, b);

        //Determine the current highest bitstream order in our bundle2bitstream table 
        //This will always append a newly added bitstream as the last one 
        int bitstreamOrder = 0;  //bitstream order starts at '0' index
        TableRow tableRow = DatabaseManager.querySingle(ourContext, "SELECT MAX(bitstream_order) as max_value FROM bundle2bitstream WHERE bundle_id=?", getID());
        if(tableRow != null){
            bitstreamOrder = tableRow.getIntColumn("max_value") + 1;
        }

        // Add the mapping row to the database
        TableRow mappingRow = DatabaseManager.row("bundle2bitstream");
        mappingRow.setColumn("bundle_id", getID());
        mappingRow.setColumn("bitstream_id", b.getID());
        mappingRow.setColumn("bitstream_order", bitstreamOrder);
        DatabaseManager.insert(ourContext, mappingRow);
    }

    /**
     * Changes bitstream order according to the array
     * @param bitstreamIds the identifiers in the order they are to be set
     * @throws SQLException when an SQL error has occurred (querying DSpace)
     * @throws AuthorizeException If the user can't make the changes
     */
    public void setOrder(int bitstreamIds[]) throws AuthorizeException, SQLException {
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        //Map the bitstreams of the bundle by identifier
        Map<Integer, Bitstream> bitstreamMap = new HashMap<Integer, Bitstream>();
        for (Bitstream bitstream : bitstreams) {
            bitstreamMap.put(bitstream.getID(), bitstream);
        }

        //We need to also reoder our cached bitstreams list
        bitstreams = new ArrayList<Bitstream>();
        for (int i = 0; i < bitstreamIds.length; i++) {
            int bitstreamId = bitstreamIds[i];

            //TODO: take into account the asc & desc ! from the dspace.cfg
            TableRow row = DatabaseManager.querySingleTable(ourContext, "bundle2bitstream",
                    "SELECT * FROM bundle2bitstream WHERE bitstream_id= ? ", bitstreamId);

            if(row == null){
                //This should never occur but just in case
                log.warn(LogManager.getHeader(ourContext, "Invalid bitstream id while changing bitstream order", "Bundle: " + getID() + ", bitstream id: " + bitstreamId));
            }else{
                row.setColumn("bitstream_order", i);
                DatabaseManager.update(ourContext, row);
            }

            // Place the bitstream in the list of bitstreams in this bundle
            bitstreams.add(bitstreamMap.get(bitstreamId));
        }

        //The order of the bitstreams has changed, ensure that we update the last modified of our item
        Item owningItem = (Item) getParentObject();
        if(owningItem != null)
        {
            owningItem.updateLastModified();
            owningItem.update();

        }
    }

    /**
     * Remove a bitstream from this bundle - the bitstream is only deleted if
     * this was the last reference to it
     * <p>
     * If the bitstream in question is the primary bitstream recorded for the
     * bundle the primary bitstream field is unset in order to free the
     * bitstream from the foreign key constraint so that the
     * <code>cleanup</code> process can run normally.
     * 
     * @param b
     *            the bitstream to remove
     */
    public void removeBitstream(Bitstream b) throws AuthorizeException,
            SQLException, IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_bitstream",
                "bundle_id=" + getID() + ",bitstream_id=" + b.getID()));

        // Remove from internal list of bitstreams
        ListIterator li = bitstreams.listIterator();

        while (li.hasNext())
        {
            Bitstream existing = (Bitstream) li.next();

            if (b.getID() == existing.getID())
            {
                // We've found the bitstream to remove
                li.remove();
            }
        }

        ourContext.addEvent(new Event(Event.REMOVE, Constants.BUNDLE, getID(), 
                Constants.BITSTREAM, b.getID(), String.valueOf(b.getSequenceID()),
                getIdentifiers(ourContext)));

        //Ensure that the last modified from the item is triggered !
        Item owningItem = (Item) getParentObject();
        if(owningItem != null)
        {
            owningItem.updateLastModified();
            owningItem.update();

        }

        // In the event that the bitstream to remove is actually
        // the primary bitstream, be sure to unset the primary
        // bitstream.
        if (b.getID() == getPrimaryBitstreamID()) 
        {
            unsetPrimaryBitstreamID();
        }
        
        // Delete the mapping row
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM bundle2bitstream WHERE bundle_id= ? "+
                "AND bitstream_id= ? ", 
                getID(), b.getID());

        // If the bitstream is orphaned, it's removed
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM bundle2bitstream WHERE bitstream_id= ? ",
                b.getID());

        try
        {
            if (!tri.hasNext())
            {
                // The bitstream is an orphan, delete it
                b.delete();
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
     * Update the bundle metadata
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        //AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        log.info(LogManager.getHeader(ourContext, "update_bundle", "bundle_id="
                + getID()));

        DatabaseManager.update(ourContext, bundleRow);

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.BUNDLE, getID(),
                    null, getIdentifiers(ourContext)));
            modified = false;
        }
        if (modifiedMetadata)
        {
            updateMetadata();
            clearDetails();
        }
    }

    /**
     * Delete the bundle. Bitstreams contained by the bundle are removed first;
     * this may result in their deletion, if deleting this bundle leaves them as
     * orphans.
     */
    void delete() throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(ourContext, "delete_bundle", "bundle_id="
                + getID()));

        ourContext.addEvent(new Event(Event.DELETE, Constants.BUNDLE, getID(), 
                getName(), getIdentifiers(ourContext)));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove bitstreams
        Bitstream[] bs = getBitstreams();

        for (int i = 0; i < bs.length; i++)
        {
            removeBitstream(bs[i]);
        }

        // remove our authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Remove ourself
        DatabaseManager.delete(ourContext, bundleRow);

        removeMetadataFromDatabase();
    }

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.BUNDLE;
    }
    
    /**
     * remove all policies on the bundle and its contents, and replace them with
     * the DEFAULT_BITSTREAM_READ policies belonging to the collection.
     * 
     * @param c
     *            Collection
     * @throws java.sql.SQLException
     *             if an SQL error or if no default policies found. It's a bit
     *             draconian, but default policies must be enforced.
     * @throws AuthorizeException
     */
    public void inheritCollectionDefaultPolicies(Collection c)
            throws java.sql.SQLException, AuthorizeException
    {
        List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(ourContext, c,
                Constants.DEFAULT_BITSTREAM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        Iterator<ResourcePolicy> i = policies.iterator();

        if (!i.hasNext())
        {
            throw new java.sql.SQLException("Collection " + c.getID()
                    + " has no default bitstream READ policies");
        }

        while (i.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) i.next();
            rp.setAction(Constants.READ);
        }

        replaceAllBitstreamPolicies(policies);
    }
    
    /**
     * remove all of the policies for the bundle and bitstream contents and replace
     * them with a new list of policies
     * 
     * @param newpolicies -
     *            this will be all of the new policies for the bundle and
     *            bitstream contents
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void replaceAllBitstreamPolicies(List<ResourcePolicy> newpolicies)
            throws SQLException, AuthorizeException
    {
        if (bitstreams != null && bitstreams.size() > 0)
        {
            for (Bitstream bs : bitstreams)
            {
                // change bitstream policies
                AuthorizeManager.removeAllPolicies(ourContext, bs);
                AuthorizeManager.addPolicies(ourContext, newpolicies, bs);
            }
        }
        // change bundle policies
        AuthorizeManager.removeAllPolicies(ourContext, this);
        AuthorizeManager.addPolicies(ourContext, newpolicies, this);
    }

    public List<ResourcePolicy> getBundlePolicies() throws SQLException
    {
        return AuthorizeManager.getPolicies(ourContext, this);
    }

    public List<ResourcePolicy> getBitstreamPolicies() throws SQLException
    {
        List<ResourcePolicy> list = new ArrayList<ResourcePolicy>();
        if (bitstreams != null && bitstreams.size() > 0)
        {
            for (Bitstream bs : bitstreams)
            {
                list.addAll(AuthorizeManager.getPolicies(ourContext, bs));
            }
        }
        return list;
    }
    
    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        Item[] items = getItems();
        Item item = null;
        Collection collection = null;
        Community community = null;
        if (items != null && items.length > 0)
        {
            item = items[0];
            collection = item.getOwningCollection();
            if (collection != null)
            {
                Community[] communities = collection.getCommunities();
                if (communities != null && communities.length > 0)
                {
                    community = communities[0];
                }
            }
        }
        switch (action)
        {
        case Constants.REMOVE:
            if (AuthorizeConfiguration.canItemAdminPerformBitstreamDeletion())
            {
                adminObject = item;
            }
            else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamDeletion())
            {
                adminObject = collection;
            }
            else if (AuthorizeConfiguration
                    .canCommunityAdminPerformBitstreamDeletion())
            {
                adminObject = community;
            }
            break;
        case Constants.ADD:
            if (AuthorizeConfiguration.canItemAdminPerformBitstreamCreation())
            {
                adminObject = item;
            }
            else if (AuthorizeConfiguration
                    .canCollectionAdminPerformBitstreamCreation())
            {
                adminObject = collection;
            }
            else if (AuthorizeConfiguration
                    .canCommunityAdminPerformBitstreamCreation())
            {
                adminObject = community;
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
        Item[] items = getItems();
       
        if (items != null && (items.length > 0 && items[0] != null))
        {
            return items[0];
        }
        else
        {
            return null;
        }
    }

    @Override
    public void updateLastModified()
    {

    }
}
