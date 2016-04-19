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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.event.Event;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersioningService;

/**
 * Class representing an item in DSpace.
 * <P>
 * This class holds in memory the item Dublin Core metadata, the bundles in the
 * item, and the bitstreams in those bundles. When modifying the item, if you
 * modify the Dublin Core or the "in archive" flag, you must call
 * <code>update</code> for the changes to be written to the database.
 * Creating, adding or removing bundles or bitstreams has immediate effect in
 * the database.
 *
 * @author Robert Tansley
 * @author Martin Hald
 * @version $Revision$
 */
public class Item extends DSpaceObject
{
    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    /** log4j category */
    private static final Logger log = Logger.getLogger(Item.class);

    /** The table row corresponding to this item */
    private final TableRow itemRow;

    /** The e-person who submitted this item */
    private EPerson submitter;

    /** The bundles in this item - kept in sync with DB */
    private List<Bundle> bundles;


    /** Handle, if any */
    private String handle;

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    private boolean modified;

    /**
     * Construct an item with the given table row
     *
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     * @throws SQLException
     */
    Item(Context context, TableRow row) throws SQLException
    {
        super(context);

        // Ensure that my TableRow is typed.
        if (null == row.getTable())
            row.setTable("item");

        itemRow = row;
        modified = false;
        clearDetails();

        // Get our Handle if any
        handle = HandleManager.findHandle(context, this);

        // Cache ourselves
        context.cache(this, row.getIntColumn("item_id"));
    }


    /**
     * Get an item from the database. The item, its Dublin Core metadata, and
     * the bundle and bitstream metadata are all loaded into memory.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            Internal ID of the item
     * @return the item, or null if the internal ID is invalid.
     * @throws SQLException
     */
    public static Item find(Context context, int id) throws SQLException
    {
        // First check the cache
        Item fromCache = (Item) context.fromCache(Item.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "item", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_item",
                        "not_found,item_id=" + id));
            }

            return null;
        }

        // not null, return item
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_item", "item_id="
                    + id));
        }

        return new Item(context, row);
    }

    /**
     * Create a new item, with a new internal ID. This method is not public,
     * since items need to be created as workspace items. Authorisation is the
     * responsibility of the caller.
     *
     * @param context
     *            DSpace context object
     * @return the newly created item
     * @throws SQLException
     * @throws AuthorizeException
     */
    static Item create(Context context) throws SQLException, AuthorizeException
    {
        TableRow row = DatabaseManager.create(context, "item");
        Item i = new Item(context, row);

        // set discoverable to true (default)
        i.setDiscoverable(true);

        // Call update to give the item a last modified date. OK this isn't
        // amazingly efficient but creates don't happen that often.
        context.turnOffAuthorisationSystem();
        i.update();
        context.restoreAuthSystemState();

        context.addEvent(new Event(Event.CREATE, Constants.ITEM, i.getID(), 
                null, i.getIdentifiers(context)));

        log.info(LogManager.getHeader(context, "create_item", "item_id="
                + row.getIntColumn("item_id")));

        return i;
    }

    /**
     * Get all the items in the archive. Only items with the "in archive" flag
     * set are included. The order of the list is indeterminate.
     *
     * @param context
     *            DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException
     */
    public static ItemIterator findAll(Context context) throws SQLException
    {
        String myQuery = "SELECT * FROM item WHERE in_archive='1'";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }
    
    /**
     * Get all "final" items in the archive, both archived ("in archive" flag) or
     * withdrawn items are included. The order of the list is indeterminate.
     *
     * @param context
     *            DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException
     */
	public static ItemIterator findAllUnfiltered(Context context) throws SQLException
    {
        String myQuery = "SELECT * FROM item WHERE in_archive='1' or withdrawn='1'";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
	}

    /**
     * Find all the items in the archive by a given submitter. The order is
     * indeterminate. Only items with the "in archive" flag set are included.
     *
     * @param context
     *            DSpace context object
     * @param eperson
     *            the submitter
     * @return an iterator over the items submitted by eperson
     * @throws SQLException
     */
    public static ItemIterator findBySubmitter(Context context, EPerson eperson)
            throws SQLException
    {
        String myQuery = "SELECT * FROM item WHERE in_archive='1' AND submitter_id="
                + eperson.getID();

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }

    public static ItemIterator findByMetadataFieldAuthority(Context context, String mdString, String authority) throws SQLException, AuthorizeException, IOException {
        String[] elements = getElementsFilled(mdString);
        String schema = elements[0], element = elements[1], qualifier = elements[2];
        MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null) {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null) {
            throw new IllegalArgumentException(
                    "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }

        String query = "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' " +
                "AND item.item_id = metadatavalue.resource_id AND metadatavalue.resource_type_id=2 AND metadata_field_id = ?";
        TableRowIterator rows = null;
        if (Item.ANY.equals(authority)) {
            rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID());
        } else {
            query += " AND metadatavalue.authority = ?";
            rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(), authority);
        }
        return new ItemIterator(context, rows);
    }


    /**
     * Retrieve the list of Items submitted by eperson, ordered by recently submitted, optionally limitable
     * @param context
     * @param eperson
     * @param limit a positive integer to limit, -1 or null for unlimited
     * @return
     * @throws SQLException
     */
    public static ItemIterator findBySubmitterDateSorted(Context context, EPerson eperson, Integer limit) throws SQLException
    {
        String querySorted =    "SELECT item.item_id, item.submitter_id, item.in_archive, item.withdrawn, " +
                "item.owning_collection, item.last_modified, metadatavalue.text_value " +
                "FROM item, metadatafieldregistry, metadatavalue " +
                "WHERE metadatafieldregistry.metadata_field_id = metadatavalue.metadata_field_id AND " +
                "  metadatavalue.resource_id = item.item_id AND " +
                "  metadatavalue.resource_type_id = ? AND " +
                "  metadatafieldregistry.element = 'date' AND " +
                "  metadatafieldregistry.qualifier = 'accessioned' AND " +
                "  item.submitter_id = ? AND ";
        if(DatabaseManager.isOracle()) {
            querySorted += " item.in_archive = 1 " +
                    "ORDER BY cast(substr(metadatavalue.text_value,1,100) as varchar2(100)) desc";
        } else {
            querySorted += " item.in_archive = true " +
                    "ORDER BY metadatavalue.text_value desc";
        }

        TableRowIterator rows;

        if(limit != null && limit > 0) {
            if(DatabaseManager.isOracle()) {
                //Oracle syntax for limit
                // select * from ( SUBQUERY ) where ROWNUM <= 5;
                querySorted = "SELECT * FROM (" + querySorted + ") WHERE ROWNUM <= ?";
            } else {
                querySorted += " limit ?";
            }
            rows = DatabaseManager.query(context, querySorted, Constants.ITEM, eperson.getID(), limit);
        } else {
            rows = DatabaseManager.query(context, querySorted, Constants.ITEM, eperson.getID());
        }

        return new ItemIterator(context, rows);

    }

    /**
     * Get the internal ID of this item. In general, this shouldn't be exposed
     * to users
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return itemRow.getIntColumn("item_id");
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
     * Find out if the item is part of the main archive
     *
     * @return true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return itemRow.getBooleanColumn("in_archive");
    }

    /**
     * Find out if the item has been withdrawn
     *
     * @return true if the item has been withdrawn
     */
    public boolean isWithdrawn()
    {
        return itemRow.getBooleanColumn("withdrawn");
    }

    /**
     * Find out if the item is discoverable
     *
     * @return true if the item is discoverable
     */
    public boolean isDiscoverable()
    {
        return itemRow.getBooleanColumn("discoverable");
    }

    /**
     * Get the date the item was last modified, or the current date if
     * last_modified is null
     *
     * @return the date the item was last modified, or the current date if the
     *         column is null.
     */
    public Date getLastModified()
    {
        Date myDate = itemRow.getDateColumn("last_modified");

        if (myDate == null)
        {
            myDate = new Date();
        }

        return myDate;
    }

    /**
     * Method that updates the last modified date of the item
     */
    public void updateLastModified()
    {
        try {
            Date lastModified = new Timestamp(new Date().getTime());
            itemRow.setColumn("last_modified", lastModified);
            DatabaseManager.updateQuery(ourContext, "UPDATE item SET last_modified = ? WHERE item_id= ? ", lastModified, getID());
            //Also fire a modified event since the item HAS been modified
            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), null, getIdentifiers(ourContext)));
        } catch (SQLException e) {
            log.error(LogManager.getHeader(ourContext, "Error while updating last modified timestamp", "Item: " + getID()));
        }
    }

    /**
     * Set the "is_archived" flag. This is public and only
     * <code>WorkflowItem.archive()</code> should set this.
     *
     * @param isArchived
     *            new value for the flag
     */
    public void setArchived(boolean isArchived)
    {
        itemRow.setColumn("in_archive", isArchived);
        modified = true;
    }

    /**
     * Set the "discoverable" flag. This is public and only
     *
     * @param discoverable
     *            new value for the flag
     */
    public void setDiscoverable(boolean discoverable)
    {
        itemRow.setColumn("discoverable", discoverable);
        modified = true;
    }

    /**
     * Set the owning Collection for the item
     *
     * @param c
     *            Collection
     */
    public void setOwningCollection(Collection c)
    {
        itemRow.setColumn("owning_collection", c.getID());
        modified = true;
    }

    /**
     * Get the owning Collection for the item
     *
     * @return Collection that is the owner of the item
     * @throws SQLException
     */
    public Collection getOwningCollection() throws java.sql.SQLException
    {
        Collection myCollection = null;

        // get the collection ID
        int cid = itemRow.getIntColumn("owning_collection");

        myCollection = Collection.find(ourContext, cid);

        return myCollection;
    }

        // just get the collection ID for internal use
    private int getOwningCollectionID()
    {
        return itemRow.getIntColumn("owning_collection");
    }

    /**
     * Get the e-person that originally submitted this item
     *
     * @return the submitter
     */
    public EPerson getSubmitter() throws SQLException
    {
        if (submitter == null && !itemRow.isColumnNull("submitter_id"))
        {
            submitter = EPerson.find(ourContext, itemRow
                    .getIntColumn("submitter_id"));
        }
        return submitter;
    }

    /**
     * Set the e-person that originally submitted this item. This is a public
     * method since it is handled by the WorkspaceItem class in the ingest
     * package. <code>update</code> must be called to write the change to the
     * database.
     *
     * @param sub
     *            the submitter
     */
    public void setSubmitter(EPerson sub)
    {
        submitter = sub;

        if (submitter != null)
        {
            itemRow.setColumn("submitter_id", submitter.getID());
        }
        else
        {
            itemRow.setColumnNull("submitter_id");
        }
        modified = true;
    }

    /**
     * See whether this Item is contained by a given Collection.
     * @param collection
     * @return true if {@code collection} contains this Item.
     * @throws SQLException
     */
    public boolean isIn(Collection collection) throws SQLException
    {
        TableRow tr = DatabaseManager.querySingle(ourContext,
                "SELECT COUNT(*) AS count" +
                " FROM collection2item" +
                " WHERE collection_id = ? AND item_id = ?",
                collection.getID(), itemRow.getIntColumn("item_id"));
        return tr.getLongColumn("count") > 0;
    }

    /**
     * Get the collections this item is in. The order is indeterminate.
     *
     * @return the collections this item is in, if any.
     * @throws SQLException
     */
    public Collection[] getCollections() throws SQLException
    {
        List<Collection> collections = new ArrayList<Collection>();

        // Get collection table rows
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,"collection",
                        "SELECT collection.* FROM collection, collection2item WHERE " +
                        "collection2item.collection_id=collection.collection_id AND " +
                        "collection2item.item_id= ? ",
                        itemRow.getIntColumn("item_id"));

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

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }

    /**
     * Get the communities this item is in. Returns an unordered array of the
     * communities that house the collections this item is in, including parent
     * communities of the owning collections.
     *
     * @return the communities this item is in.
     * @throws SQLException
     */
    public Community[] getCommunities() throws SQLException
    {
        List<Community> communities = new ArrayList<Community>();

        // Get community table rows
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,"community",
                        "SELECT community.* FROM community, community2item " +
                        "WHERE community2item.community_id=community.community_id " +
                        "AND community2item.item_id= ? ",
                        itemRow.getIntColumn("item_id"));

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Community owner = (Community) ourContext.fromCache(Community.class,
                        row.getIntColumn("community_id"));

                if (owner == null)
                {
                    owner = new Community(ourContext, row);
                }

                communities.add(owner);

                // now add any parent communities
                Community[] parents = owner.getAllParents();
                communities.addAll(Arrays.asList(parents));
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
     * Get the bundles in this item.
     *
     * @return the bundles in an unordered array
     */
    public Bundle[] getBundles() throws SQLException
    {
        if (bundles == null)
        {
                bundles = new ArrayList<Bundle>();
                // Get bundles
                TableRowIterator tri = DatabaseManager.queryTable(ourContext, "bundle",
                                        "SELECT bundle.* FROM bundle, item2bundle WHERE " +
                                        "item2bundle.bundle_id=bundle.bundle_id AND " +
                                        "item2bundle.item_id= ? ",
                        itemRow.getIntColumn("item_id"));

            try
            {
                while (tri.hasNext())
                {
                    TableRow r = tri.next();

                    // First check the cache
                    Bundle fromCache = (Bundle) ourContext.fromCache(Bundle.class,
                                                r.getIntColumn("bundle_id"));

                    if (fromCache != null)
                    {
                        bundles.add(fromCache);
                    }
                    else
                    {
                        bundles.add(new Bundle(ourContext, r));
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
        }
        
        Bundle[] bundleArray = new Bundle[bundles.size()];
        bundleArray = (Bundle[]) bundles.toArray(bundleArray);

        return bundleArray;
    }

    /**
     * Get the bundles matching a bundle name (name corresponds roughly to type)
     *
     * @param name
     *            name of bundle (ORIGINAL/TEXT/THUMBNAIL)
     *
     * @return the bundles in an unordered array
     */
    public Bundle[] getBundles(String name) throws SQLException
    {
        List<Bundle> matchingBundles = new ArrayList<Bundle>();

        // now only keep bundles with matching names
        Bundle[] bunds = getBundles();
        for (int i = 0; i < bunds.length; i++ )
        {
            if (name.equals(bunds[i].getName()))
            {
                matchingBundles.add(bunds[i]);
            }
        }

        Bundle[] bundleArray = new Bundle[matchingBundles.size()];
        bundleArray = (Bundle[]) matchingBundles.toArray(bundleArray);

        return bundleArray;
    }

    /**
     * Create a bundle in this item, with immediate effect
     *
     * @param name
     *            bundle name (ORIGINAL/TEXT/THUMBNAIL)
     * @return the newly created bundle
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Bundle createBundle(String name) throws SQLException,
            AuthorizeException
    {
        if ((name == null) || "".equals(name))
        {
            throw new SQLException("Bundle must be created with non-null name");
        }

        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Bundle b = Bundle.create(ourContext);
        b.setName(name);
        b.update();

        addBundle(b);

        return b;
    }

    /**
     * Add an existing bundle to this item. This has immediate effect.
     *
     * @param b
     *            the bundle to add
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void addBundle(Bundle b) throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        // Check it's not already there
        Bundle[] bunds = getBundles();
        for (int i = 0; i < bunds.length; i++)
        {
            if (b.getID() == bunds[i].getID())
            {
                // Bundle is already there; no change
                return;
            }
        }

        // now add authorization policies from owning item
        // hmm, not very "multiple-inclusion" friendly
        AuthorizeManager.inheritPolicies(ourContext, this, b);

        // Add the bundle to in-memory list
        bundles.add(b);

        // Insert the mapping
        TableRow mappingRow = DatabaseManager.row("item2bundle");
        mappingRow.setColumn("item_id", getID());
        mappingRow.setColumn("bundle_id", b.getID());
        DatabaseManager.insert(ourContext, mappingRow);

        ourContext.addEvent(new Event(Event.ADD, Constants.ITEM, getID(), 
                Constants.BUNDLE, b.getID(), b.getName(), 
                getIdentifiers(ourContext)));
    }

    /**
     * Remove a bundle. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * @param b
     *            the bundle to remove
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeBundle(Bundle b) throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        // Remove from internal list of bundles
        Bundle[] bunds = getBundles();
        
        for (int i = 0; i < bunds.length; i++)
        {
            if (b.getID() == bunds[i].getID())
            {
                // We've found the bundle to remove
                bundles.remove(bunds[i]);
                break;
            }
        }

        // Remove mapping from DB
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM item2bundle WHERE item_id= ? " +
                "AND bundle_id= ? ",
                getID(), b.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.ITEM, getID(), 
                Constants.BUNDLE, b.getID(), b.getName(), getIdentifiers(ourContext)));

        // If the bundle is orphaned, it's removed
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM item2bundle WHERE bundle_id= ? ",
                b.getID());

        try
        {
            if (!tri.hasNext())
            {
                //make the right to remove the bundle explicit because the implicit
                // relation
                //has been removed. This only has to concern the currentUser
                // because
                //he started the removal process and he will end it too.
                //also add right to remove from the bundle to remove it's
                // bitstreams.
                AuthorizeManager.addPolicy(ourContext, b, Constants.DELETE,
                        ourContext.getCurrentUser());
                AuthorizeManager.addPolicy(ourContext, b, Constants.REMOVE,
                        ourContext.getCurrentUser());

                // The bundle is an orphan, delete it
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
     * Create a single bitstream in a new bundle. Provided as a convenience
     * method for the most common use.
     *
     * @param is
     *            the stream to create the new bitstream from
     * @param name
     *            is the name of the bundle (ORIGINAL, TEXT, THUMBNAIL)
     * @return Bitstream that is created
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream createSingleBitstream(InputStream is, String name)
            throws AuthorizeException, IOException, SQLException
    {
        // Authorisation is checked by methods below
        // Create a bundle
        Bundle bnd = createBundle(name);
        Bitstream bitstream = bnd.createBitstream(is);
        addBundle(bnd);

        // FIXME: Create permissions for new bundle + bitstream
        return bitstream;
    }

    /**
     * Convenience method, calls createSingleBitstream() with name "ORIGINAL"
     *
     * @param is
     *            InputStream
     * @return created bitstream
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream createSingleBitstream(InputStream is)
            throws AuthorizeException, IOException, SQLException
    {
        return createSingleBitstream(is, "ORIGINAL");
    }

    /**
     * Get all non-internal bitstreams in the item. This is mainly used for
     * auditing for provenance messages and adding format.* DC values. The order
     * is indeterminate.
     *
     * @return non-internal bitstreams.
     */
    public Bitstream[] getNonInternalBitstreams() throws SQLException
    {
        List<Bitstream> bitstreamList = new ArrayList<Bitstream>();

        // Go through the bundles and bitstreams picking out ones which aren't
        // of internal formats
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] bitstreams = bunds[i].getBitstreams();

            for (int j = 0; j < bitstreams.length; j++)
            {
                if (!bitstreams[j].getFormat().isInternal())
                {
                    // Bitstream is not of an internal format
                    bitstreamList.add(bitstreams[j]);
                }
            }
        }

        return bitstreamList.toArray(new Bitstream[bitstreamList.size()]);
    }

    /**
     * Remove just the DSpace license from an item This is useful to update the
     * current DSpace license, in case the user must accept the DSpace license
     * again (either the item was rejected, or resumed after saving)
     * <p>
     * This method is used by the org.dspace.submit.step.LicenseStep class
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeDSpaceLicense() throws SQLException, AuthorizeException,
            IOException
    {
        // get all bundles with name "LICENSE" (these are the DSpace license
        // bundles)
        Bundle[] bunds = getBundles("LICENSE");

        for (int i = 0; i < bunds.length; i++)
        {
            // FIXME: probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            removeBundle(bunds[i]);
        }
    }

    /**
     * Remove all licenses from an item - it was rejected
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeLicenses() throws SQLException, AuthorizeException,
            IOException
    {
        // Find the License format
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(ourContext,
                "License");
        int licensetype = bf.getID();

        // search through bundles, looking for bitstream type license
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            boolean removethisbundle = false;

            Bitstream[] bits = bunds[i].getBitstreams();

            for (int j = 0; j < bits.length; j++)
            {
                BitstreamFormat bft = bits[j].getFormat();

                if (bft.getID() == licensetype)
                {
                    removethisbundle = true;
                }
            }

            // probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            if (removethisbundle)
            {
                removeBundle(bunds[i]);
            }
        }
    }

    /**
     * Update the item "in archive" flag and Dublin Core metadata in the
     * database
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        // only do write authorization if user is not an editor
        if (!canEdit())
        {
            AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        }

        log.info(LogManager.getHeader(ourContext, "update_item", "item_id="
                + getID()));

        // Set sequence IDs for bitstreams in item
        int sequence = 0;
        Bundle[] bunds = getBundles();

        // find the highest current sequence number
        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] streams = bunds[i].getBitstreams();

            for (int k = 0; k < streams.length; k++)
            {
                if (streams[k].getSequenceID() > sequence)
                {
                    sequence = streams[k].getSequenceID();
                }
            }
        }

        // start sequencing bitstreams without sequence IDs
        sequence++;

        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] streams = bunds[i].getBitstreams();

            for (int k = 0; k < streams.length; k++)
            {
                if (streams[k].getSequenceID() < 0)
                {
                    streams[k].setSequenceID(sequence);
                    sequence++;
                    streams[k].update();
                    modified = true;
                }
            }
        }

        if (modifiedMetadata || modified)
        {
            // Set the last modified date
            itemRow.setColumn("last_modified", new Date());

            // Make sure that withdrawn and in_archive are non-null
            if (itemRow.isColumnNull("in_archive"))
            {
                itemRow.setColumn("in_archive", false);
            }

            if (itemRow.isColumnNull("withdrawn"))
            {
                itemRow.setColumn("withdrawn", false);
            }

            if (itemRow.isColumnNull("discoverable"))
            {
                itemRow.setColumn("discoverable", false);
            }


            DatabaseManager.update(ourContext, itemRow);

            if (modifiedMetadata) {
                updateMetadata();
                clearDetails();
            }

            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                    null, getIdentifiers(ourContext)));
            modified = false;
        }
    }


    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void withdraw() throws SQLException, AuthorizeException, IOException
    {
        // Check permission. User either has to have REMOVE on owning collection
        // or be COLLECTION_EDITOR of owning collection
        AuthorizeUtil.authorizeWithdrawItem(ourContext, this);

        String timestamp = DCDate.getCurrent().toString();

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();

        // Build some provenance data while we're at it.
        StringBuilder prov = new StringBuilder();

        prov.append("Item withdrawn by ").append(e.getFullName()).append(" (")
                .append(e.getEmail()).append(") on ").append(timestamp).append("\n")
                .append("Item was in collections:\n");

        Collection[] colls = getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            prov.append(colls[i].getMetadata("name")).append(" (ID: ").append(colls[i].getID()).append(")\n");
        }

        // Set withdrawn flag. timestamp will be set; last_modified in update()
        itemRow.setColumn("withdrawn", true);

        // in_archive flag is now false
        itemRow.setColumn("in_archive", false);

        prov.append(InstallItem.getBitstreamProvenanceMessage(this));

        addDC("description", "provenance", "en", prov.toString());

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                "WITHDRAW", getIdentifiers(ourContext)));

        // remove all authorization policies, saving the custom ones
        AuthorizeManager.removeAllPoliciesByDSOAndTypeNotEqualsTo(ourContext, this, ResourcePolicy.TYPE_CUSTOM);

        // Write log
        log.info(LogManager.getHeader(ourContext, "withdraw_item", "user="
                + e.getEmail() + ",item_id=" + getID()));
    }


    /**
     * Reinstate a withdrawn item
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void reinstate() throws SQLException, AuthorizeException,
            IOException
    {
        // check authorization
        AuthorizeUtil.authorizeReinstateItem(ourContext, this);

        String timestamp = DCDate.getCurrent().toString();

        // Check permission. User must have ADD on all collections.
        // Build some provenance data while we're at it.
        Collection[] colls = getCollections();

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();
        StringBuilder prov = new StringBuilder();
        prov.append("Item reinstated by ").append(e.getFullName()).append(" (")
                .append(e.getEmail()).append(") on ").append(timestamp).append("\n")
                .append("Item was in collections:\n");

        for (int i = 0; i < colls.length; i++)
        {
            prov.append(colls[i].getMetadata("name")).append(" (ID: ").append(colls[i].getID()).append(")\n");
        }
        
        // Clear withdrawn flag
        itemRow.setColumn("withdrawn", false);

        // in_archive flag is now true
        itemRow.setColumn("in_archive", true);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        prov.append(InstallItem.getBitstreamProvenanceMessage(this));

        addDC("description", "provenance", "en", prov.toString());

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                "REINSTATE", getIdentifiers(ourContext)));

        // authorization policies
        if (colls.length > 0)
        {
            // FIXME: not multiple inclusion friendly - just apply access
            // policies from first collection
            // remove the item's policies and replace them with
            // the defaults from the collection
            inheritCollectionDefaultPolicies(colls[0]);
        }

        // Write log
        log.info(LogManager.getHeader(ourContext, "reinstate_item", "user="
                + e.getEmail() + ",item_id=" + getID()));
    }

    /**
     * Delete (expunge) the item. Bundles and bitstreams are also deleted if
     * they are not also included in another item. The Dublin Core metadata is
     * deleted.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    void delete() throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation here. If we don't, it may happen that we remove the
        // metadata but when getting to the point of removing the bundles we get an exception
        // leaving the database in an inconsistent state
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        ourContext.addEvent(new Event(Event.DELETE, Constants.ITEM, getID(), 
                getHandle(), getIdentifiers(ourContext)));

        log.info(LogManager.getHeader(ourContext, "delete_item", "item_id="
                + getID()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove from browse indices, if appropriate
        /** XXX FIXME
         ** Although all other Browse index updates are managed through
         ** Event consumers, removing an Item *must* be done *here* (inline)
         ** because otherwise, tables are left in an inconsistent state
         ** and the DB transaction will fail.
         ** Any fix would involve too much work on Browse code that
         ** is likely to be replaced soon anyway.   --lcs, Aug 2006
         **
         ** NB Do not check to see if the item is archived - withdrawn /
         ** non-archived items may still be tracked in some browse tables
         ** for administrative purposes, and these need to be removed.
         **/
//               FIXME: there is an exception handling problem here
        try
        {
//               Remove from indices
            IndexBrowse ib = new IndexBrowse(ourContext);
            ib.itemRemoved(this);
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new SQLException(e.getMessage(), e);
        }

        // Delete the Dublin Core
        removeMetadataFromDatabase();

        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            removeBundle(bunds[i]);
        }

        // remove all of our authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);
        
        // Remove any Handle
        HandleManager.unbindHandle(ourContext, this);
        
        // remove version attached to the item
        removeVersion();


        // Finally remove item row
        DatabaseManager.delete(ourContext, itemRow);
    }
    
    private void removeVersion() throws AuthorizeException, SQLException
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(versioningService.getVersion(ourContext, this)!=null)
        {
            versioningService.removeVersion(ourContext, this);
        }else{
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            try {
                identifierService.delete(ourContext, this);
            } catch (IdentifierException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Remove item and all its sub-structure from the context cache.
     * Useful in batch processes where a single context has a long,
     * multi-item lifespan
     */
    public void decache() throws SQLException
    {
        // Remove item and it's submitter from cache
        ourContext.removeCached(this, getID());
        if (submitter != null)
        {
                ourContext.removeCached(submitter, submitter.getID());
        }
        // Remove bundles & bitstreams from cache if they have been loaded
        if (bundles != null)
        {
                Bundle[] bunds = getBundles();
                for (int i = 0; i < bunds.length; i++)
                {
                        ourContext.removeCached(bunds[i], bunds[i].getID());
                        Bitstream[] bitstreams = bunds[i].getBitstreams();
                        for (int j = 0; j < bitstreams.length; j++)
                        {
                                ourContext.removeCached(bitstreams[j], bitstreams[j].getID());
                        }
                }
        }
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Item as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     * @return <code>true</code> if object passed in represents the same item
     *         as this object
     */
     @Override
     public boolean equals(Object obj)
     {
         if (obj == null)
         {
             return false;
         }
         if (getClass() != obj.getClass())
         {
             return false;
         }
         final Item other = (Item) obj;
         if (this.getType() != other.getType())
         {
             return false;
         }
         if (this.getID() != other.getID())
         {
             return false;
         }

         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 5;
         hash = 71 * hash + (this.itemRow != null ? this.itemRow.hashCode() : 0);
         return hash;
     }




    /**
     * Return true if this Collection 'owns' this item
     *
     * @param c
     *            Collection
     * @return true if this Collection owns this item
     */
    public boolean isOwningCollection(Collection c)
    {
        int owner_id = itemRow.getIntColumn("owning_collection");

        if (c.getID() == owner_id)
        {
            return true;
        }

        // not the owner
        return false;
    }

    /**
     * return type found in Constants
     *
     * @return int Constants.ITEM
     */
    public int getType()
    {
        return Constants.ITEM;
    }

    /**
     * remove all of the policies for item and replace them with a new list of
     * policies
     *
     * @param newpolicies -
     *            this will be all of the new policies for the item and its
     *            contents
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void replaceAllItemPolicies(List<ResourcePolicy> newpolicies) throws SQLException,
            AuthorizeException
    {
        // remove all our policies, add new ones
        AuthorizeManager.removeAllPolicies(ourContext, this);
        AuthorizeManager.addPolicies(ourContext, newpolicies, this);
    }

    /**
     * remove all of the policies for item's bitstreams and bundles and replace
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
        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];
            mybundle.replaceAllBitstreamPolicies(newpolicies);
        }
    }

    /**
     * remove all of the policies for item's bitstreams and bundles that belong
     * to a given Group
     *
     * @param g
     *            Group referenced by policies that needs to be removed
     * @throws SQLException
     */
    public void removeGroupPolicies(Group g) throws SQLException
    {
        // remove Group's policies from Item
        AuthorizeManager.removeGroupPolicies(ourContext, this, g);

        // remove all policies from bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                // remove bitstream policies
                AuthorizeManager.removeGroupPolicies(ourContext, bs[j], g);
            }

            // change bundle policies
            AuthorizeManager.removeGroupPolicies(ourContext, mybundle, g);
        }
    }

    /**
     * remove all policies on an item and its contents, and replace them with
     * the DEFAULT_ITEM_READ and DEFAULT_BITSTREAM_READ policies belonging to
     * the collection.
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
        adjustItemPolicies(c);
        adjustBundleBitstreamPolicies(c);

        log.debug(LogManager.getHeader(ourContext, "item_inheritCollectionDefaultPolicies",
                                                   "item_id=" + getID()));
    }

    public void adjustBundleBitstreamPolicies(Collection c) throws SQLException, AuthorizeException {

        List<ResourcePolicy> defaultCollectionPolicies = AuthorizeManager.getPoliciesActionFilter(ourContext, c, Constants.DEFAULT_BITSTREAM_READ);

        if (defaultCollectionPolicies.size() < 1){
            throw new SQLException("Collection " + c.getID()
                    + " (" + c.getHandle() + ")"
                    + " has no default bitstream READ policies");
        }

        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();
        for (int i = 0; i < bunds.length; i++){
            Bundle mybundle = bunds[i];

            // if come from InstallItem: remove all submission/workflow policies
            AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, mybundle, ResourcePolicy.TYPE_SUBMISSION);
            AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, mybundle, ResourcePolicy.TYPE_WORKFLOW);

            List<ResourcePolicy> policiesBundleToAdd = filterPoliciesToAdd(defaultCollectionPolicies, mybundle);
            AuthorizeManager.addPolicies(ourContext, policiesBundleToAdd, mybundle);

            for(Bitstream bitstream : mybundle.getBitstreams()){
                // if come from InstallItem: remove all submission/workflow policies
                AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, bitstream, ResourcePolicy.TYPE_SUBMISSION);
                AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, bitstream, ResourcePolicy.TYPE_WORKFLOW);

                List<ResourcePolicy> policiesBitstreamToAdd = filterPoliciesToAdd(defaultCollectionPolicies, bitstream);
                AuthorizeManager.addPolicies(ourContext, policiesBitstreamToAdd, bitstream);
            }
        }
    }

    public void adjustItemPolicies(Collection c) throws SQLException, AuthorizeException {
        // read collection's default READ policies
        List<ResourcePolicy> defaultCollectionPolicies = AuthorizeManager.getPoliciesActionFilter(ourContext, c, Constants.DEFAULT_ITEM_READ);

        // MUST have default policies
        if (defaultCollectionPolicies.size() < 1)
        {
            throw new SQLException("Collection " + c.getID()
                    + " (" + c.getHandle() + ")"
                    + " has no default item READ policies");
        }

        // if come from InstallItem: remove all submission/workflow policies
        AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, this, ResourcePolicy.TYPE_SUBMISSION);
        AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, this, ResourcePolicy.TYPE_WORKFLOW);

        // add default policies only if not already in place
        List<ResourcePolicy> policiesToAdd = filterPoliciesToAdd(defaultCollectionPolicies, this);
        AuthorizeManager.addPolicies(ourContext, policiesToAdd, this);
    }

    private List<ResourcePolicy> filterPoliciesToAdd(List<ResourcePolicy> defaultCollectionPolicies, DSpaceObject dso) throws SQLException {
        List<ResourcePolicy> policiesToAdd = new ArrayList<ResourcePolicy>();
        for (ResourcePolicy rp : defaultCollectionPolicies){
            rp.setAction(Constants.READ);
            // if an identical policy is already in place don't add it
            if(!AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(ourContext, dso, rp)){
                rp.setRpType(ResourcePolicy.TYPE_INHERITED);
                policiesToAdd.add(rp);
            }
        }
        return policiesToAdd;
    }

    /**
     * Moves the item from one collection to another one
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move (Collection from, Collection to) throws SQLException, AuthorizeException, IOException
    {
        // Use the normal move method, and default to not inherit permissions
        this.move(from, to, false);
    }

    /**
     * Moves the item from one collection to another one
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move (Collection from, Collection to, boolean inheritDefaultPolicies) throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation on the item before that the move occur
        // otherwise we will need edit permission on the "target collection" to archive our goal
        // only do write authorization if user is not an editor
        if (!canEdit())
        {
            AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        }
        
        // Move the Item from one Collection to the other
        to.addItem(this);
        from.removeItem(this);

        // If we are moving from the owning collection, update that too
        if (isOwningCollection(from))
        {
            // Update the owning collection
            log.info(LogManager.getHeader(ourContext, "move_item",
                                          "item_id=" + getID() + ", from " +
                                          "collection_id=" + from.getID() + " to " +
                                          "collection_id=" + to.getID()));
            setOwningCollection(to);

            // If applicable, update the item policies
            if (inheritDefaultPolicies)
            {
                log.info(LogManager.getHeader(ourContext, "move_item",
                         "Updating item with inherited policies"));
                inheritCollectionDefaultPolicies(to);
            }

            // Update the item
            ourContext.turnOffAuthorisationSystem();
            update();
            ourContext.restoreAuthSystemState();
        }
        else
        {
            // Although we haven't actually updated anything within the item
            // we'll tell the event system that it has, so that any consumers that
            // care about the structure of the repository can take account of the move

            // Note that updating the owning collection above will have the same effect,
            // so we only do this here if the owning collection hasn't changed.
            
            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                    null, getIdentifiers(ourContext)));
        }
    }
    
    /**
     * Check the bundle ORIGINAL to see if there are any uploaded files
     *
     * @return true if there is a bundle named ORIGINAL with one or more
     *         bitstreams inside
     * @throws SQLException
     */
    public boolean hasUploadedFiles() throws SQLException
    {
        Bundle[] bundles = getBundles("ORIGINAL");
        if (bundles.length == 0)
        {
            // if no ORIGINAL bundle,
            // return false that there is no file!
            return false;
        }
        else
        {
            Bitstream[] bitstreams = bundles[0].getBitstreams();
            if (bitstreams.length == 0)
            {
                // no files in ORIGINAL bundle!
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the collections this item is not in.
     *
     * @return the collections this item is not in, if any.
     * @throws SQLException
     */
    public Collection[] getCollectionsNotLinked() throws SQLException
    {
        Collection[] allCollections = Collection.findAll(ourContext);
        Collection[] linkedCollections = getCollections();
        Collection[] notLinkedCollections = new Collection[allCollections.length - linkedCollections.length];

        if ((allCollections.length - linkedCollections.length) == 0)
        {
                return notLinkedCollections;
        }
        
        int i = 0;
                 
        for (Collection collection : allCollections)
        {
                 boolean alreadyLinked = false;
                         
                 for (Collection linkedCommunity : linkedCollections)
                 {
                         if (collection.getID() == linkedCommunity.getID())
                         {
                                 alreadyLinked = true;
                                 break;
                         }
                 }
                         
                 if (!alreadyLinked)
                 {
                         notLinkedCollections[i++] = collection;
                 }
        }
        
        return notLinkedCollections;
    }

    /**
     * return TRUE if context's user can edit item, false otherwise
     *
     * @return boolean true = current user can edit item
     * @throws SQLException
     */
    public boolean canEdit() throws java.sql.SQLException
    {
        // can this person write to the item?
        if (AuthorizeManager.authorizeActionBoolean(ourContext, this,
                Constants.WRITE))
        {
            return true;
        }

        // is this collection not yet created, and an item template is created
        if (getOwningCollection() == null)
        {
            return true;
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        if (getOwningCollection().canEditBoolean(false))
        {
            return true;
        }

        return false;
    }
    
    public String getName()
    {
        return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
    }

    /**
     * Returns an iterator of Items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     *
     */
    public static ItemIterator findByMetadataField(Context context,
               String schema, String element, String qualifier, String value)
          throws SQLException, AuthorizeException, IOException
    {
        MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException(
                    "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }
        
        String query = "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' "+
                       "AND item.item_id = metadatavalue.resource_id AND metadata_field_id = ? AND resource_type_id = ?";
        TableRowIterator rows = null;
        if (Item.ANY.equals(value))
        {
                rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(), Constants.ITEM);
        }
        else
        {
                query += " AND metadatavalue.text_value = ?";
                rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(),  Constants.ITEM, value);
        }
        return new ItemIterator(context, rows);
     }
    
    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        Collection collection = getOwningCollection();
        Community community = null;
        if (collection != null)
        {
            Community[] communities = collection.getCommunities();
            if (communities != null && communities.length > 0)
            {
                community = communities[0];
            }
        }
        else
        {
            // is a template item?
            TableRow qResult = DatabaseManager.querySingle(ourContext,
                       "SELECT collection_id FROM collection " +
                       "WHERE template_item_id = ?",getID());
            if (qResult != null)
            {
                collection = Collection.find(ourContext, qResult.getIntColumn("collection_id"));
                Community[] communities = collection.getCommunities();
                if (communities != null && communities.length > 0)
                {
                    community = communities[0];
                }
            }
        }
        
        switch (action)
        {
            case Constants.ADD:
                // ADD a cc license is less general than add a bitstream but we can't/won't
                // add complex logic here to know if the ADD action on the item is required by a cc or
                // a generic bitstream so simply we ignore it.. UI need to enforce the requirements.
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamCreation())
                {
                    adminObject = this;
                }
                else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamCreation())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformBitstreamCreation())
                {
                    adminObject = community;
                }
                break;
            case Constants.REMOVE:
                // see comments on ADD action, same things...
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamDeletion())
                {
                    adminObject = this;
                }
                else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamDeletion())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformBitstreamDeletion())
                {
                    adminObject = community;
                }
                break;
            case Constants.DELETE:
                if (getOwningCollection() != null)
                {
                    if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminPerformItemDeletion())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                break;
            case Constants.WRITE:
                // if it is a template item we need to check the
                // collection/community admin configuration
                if (getOwningCollection() == null)
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                else
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
        Collection ownCollection = getOwningCollection();
        if (ownCollection != null)
        {
            return ownCollection;
        }
        else
        {
            // is a template item?
            TableRow qResult = DatabaseManager.querySingle(ourContext,
                       "SELECT collection_id FROM collection " +
                       "WHERE template_item_id = ?",getID());
            if (qResult != null)
            {
                return Collection.find(ourContext,qResult.getIntColumn("collection_id"));
            }
            return null;
        }
    }

    /**
     * Find all the items in the archive with a given authority key value
     * in the indicated metadata field.
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value the value of authority key to look for
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     */
    public static ItemIterator findByAuthorityValue(Context context,
            String schema, String element, String qualifier, String value)
        throws SQLException, AuthorizeException, IOException
    {
        MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException("No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }

        TableRowIterator rows = DatabaseManager.queryTable(context, "item",
            "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' "+
            "AND item.item_id = metadatavalue.resource_id AND metadata_field_id = ? AND authority = ? AND resource_type_id = ?",
            mdf.getFieldID(), value, Constants.ITEM);
        return new ItemIterator(context, rows);
    }

    @Override
    protected void getAuthoritiesAndConfidences(String fieldKey, String[] values, String[] authorities, int[] confidences, int i) {
        Choices c = ChoiceAuthorityManager.getManager().getBestMatch(fieldKey, values[i], getOwningCollectionID(), null);
        authorities[i] = c.values.length > 0 ? c.values[0].authority : null;
        confidences[i] = c.confidence;
    }
}
