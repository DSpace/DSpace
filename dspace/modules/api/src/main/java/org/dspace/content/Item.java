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
import java.lang.RuntimeException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.event.Event;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;

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
 * @version $Revision: 6107 $
 */
public class Item extends DSpaceObject
{
    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    /** log4j category */
    private static final Logger log = Logger.getLogger(Item.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow itemRow;

    /** The e-person who submitted this item */
    private EPerson submitter;

    /** The bundles in this item - kept in sync with DB */
    private List<Bundle> bundles;

    /** The Dublin Core metadata - inner class for lazy loading */
    MetadataCache dublinCore = new MetadataCache();

    /** Handle, if any */
    private String handle;

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    private boolean modified;

    private int internalItemId;

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
        ourContext = context;
        itemRow = row;
        internalItemId = row.getIntColumn("item_id");
        dublinCore.metadataChanged = false;
        modified = false;
        clearDetails();

        // Get our Handle if any
        handle = HandleManager.findHandle(context, this);

        // Cache ourselves
        context.cache(this, internalItemId);
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

        // Call update to give the item a last modified date. OK this isn't
        // amazingly efficient but creates don't happen that often.
        context.turnOffAuthorisationSystem();
        i.update();
        context.restoreAuthSystemState();

        context.addEvent(new Event(Event.CREATE, Constants.ITEM, i.getID(), null));

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

    /**
     * Get the internal ID of this item. In general, this shouldn't be exposed
     * to users
     *
     * @return the internal identifier
     */
    public int getID() {
        return internalItemId;
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
        if (modified) {
            Date lastModified = new Timestamp(new Date().getTime());
            itemRow.setColumn("last_modified", lastModified);

            // Make sure that withdrawn and in_archive are non-null
            if (itemRow.isColumnNull("in_archive")) {
                itemRow.setColumn("in_archive", false);
            }

            if (itemRow.isColumnNull("withdrawn")) {
                itemRow.setColumn("withdrawn", false);
            }

            try {
                DatabaseManager.update(ourContext, itemRow);
            } catch (SQLException e) {
                log.error(LogManager.getHeader(ourContext, "Error while updating last modified timestamp", "Item: " + internalItemId));
            }
            modified = false;
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
        if(cid!=-1)
        {
            myCollection = Collection.find(ourContext, cid);
        }
        if(myCollection==null)
        {
            WorkspaceItem workspaceItem = WorkspaceItem.findByItemId(ourContext,itemRow.getIntColumn("item_id"));
            if(workspaceItem!=null)
            {
                myCollection = workspaceItem.getCollection();
            }
            else
            {
                try{
                WorkflowItem workflowItem = WorkflowItem.findByItemId(ourContext,itemRow.getIntColumn("item_id"));
                if(workflowItem!=null)
                {
                    myCollection = workflowItem.getCollection();
                }
                }catch (Exception e)
                {
                    log.error("error while finding the owning collection for item: "+itemRow.getIntColumn("item_id")+" in workflow");
                }
            }
        }

        return myCollection;
    }

    // just get the collection ID for internal use
    private int getOwningCollectionID()
    {
        return itemRow.getIntColumn("owning_collection");
    }

    /**
     * Get Dublin Core metadata for the item.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches Dublin Core fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>item.getDC( "title", null, Item.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>item.getDC( "title", Item.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param element
     *            the Dublin Core element. <code>Item.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            DC must have an element.
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are returned, and
     *            <code>Item.ANY</code> means values with any country code or
     *            no country code are returned.
     * @return Dublin Core fields that match the parameters
     */
    @Deprecated
    public DCValue[] getDC(String element, String qualifier, String lang)
    {
        return getMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

    /**
     * Get metadata for the item in a chosen schema.
     * See <code>MetadataSchema</code> for more information about schemas.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches metadata fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>item.getMetadata("dc", "title", null, Item.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>item.getMetadata("dc, "title", Item.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element name. <code>Item.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            metadata must have an element.
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are returned, and
     *            <code>Item.ANY</code> means values with any country code or
     *            no country code are returned.
     * @return metadata fields that match the parameters
     */
    public DCValue[] getMetadata(String schema, String element, String qualifier,
                                 String lang)
    {
        // Build up list of matching values
        List<DCValue> values = new ArrayList<DCValue>();
        for (DCValue dcv : dublinCore.getMetadata())
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                DCValue copy = new DCValue();
                copy.element = dcv.element;
                copy.qualifier = dcv.qualifier;
                copy.value = dcv.value;
                copy.language = dcv.language;
                copy.schema = dcv.schema;
                copy.authority = dcv.authority;
                copy.confidence = dcv.confidence;
                values.add(copy);
            }
        }

        // Create an array of matching values
        DCValue[] valueArray = new DCValue[values.size()];
        valueArray = (DCValue[]) values.toArray(valueArray);

        return valueArray;
    }

    /**
     * Retrieve metadata field values from a given metadata string
     * of the form <schema prefix>.<element>[.<qualifier>|.*]
     *
     * @param mdString
     *            The metadata string of the form
     *            <schema prefix>.<element>[.<qualifier>|.*]
     */
    public DCValue[] getMetadata(String mdString)
    {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens())
        {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = getMetadata(schema, element, qualifier, Item.ANY);
        }

        return values;
    }

    public List<DCValue> getMetadata(String mdString, String authority) {
        String[] elements = getElements(mdString);
        return getMetadata(elements[0], elements[1], elements[2], elements[3], authority);
    }

    public List<DCValue> getMetadata(String schema, String element, String qualifier, String lang, String authority) {
        DCValue[] metadata = getMetadata(schema, element, qualifier, lang);
        List<DCValue> dcValues = Arrays.asList(metadata);
        if (!authority.equals(Item.ANY)) {
            Iterator<DCValue> iterator = dcValues.iterator();
            while (iterator.hasNext()) {
                DCValue dcValue = iterator.next();
                if (!authority.equals(dcValue.authority)) {
                    iterator.remove();
                }
            }
        }
        return dcValues;
    }

    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p/>
     * The returned array will always have length >= 4
     * <p/>
     * Values in the returned array can be empty or null.
     */
    public static String[] getElements(String fieldName) {
        String[] tokens = StringUtils.split(fieldName, ".");

        int add = 4 - tokens.length;
        if (add > 0) {
            tokens = (String[]) ArrayUtils.addAll(tokens, new String[add]);
        }

        return tokens;
    }

    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p/>
     * The returned array will always have length >= 4
     * <p/>
     * When @param fill is true, elements that would be empty or null are replaced by Item.ANY
     */
    public static String[] getElementsFilled(String fieldName) {
        String[] elements = getElements(fieldName);
        for (int i = 0; i < elements.length; i++) {
            if (StringUtils.isBlank(elements[i])) {
                elements[i] = Item.ANY;
            }
        }
        return elements;
    }

    public void replaceMetadataValue(DCValue oldValue, DCValue newValue) {
        // check both dcvalues are for the same field
        if (oldValue.hasSameFieldAs(newValue)) {

            String schema = oldValue.schema;
            String element = oldValue.element;
            String qualifier = oldValue.qualifier;

            // Save all metadata for this field
            DCValue[] dcvalues = getMetadata(schema, element, qualifier, Item.ANY);
            clearMetadata(schema, element, qualifier, Item.ANY);

            for (DCValue dcvalue : dcvalues) {
                if (dcvalue.equals(oldValue)) {
                    addMetadata(schema, element, qualifier, newValue.language, newValue.value, newValue.authority, newValue.confidence);
                } else {
                    addMetadata(schema, element, qualifier, dcvalue.language, dcvalue.value, dcvalue.authority, dcvalue.confidence);
                }
            }
        }
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
                "AND item.item_id = metadatavalue.item_id AND metadata_field_id = ?";
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
     * Add Dublin Core metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     *
     * @param element
     *            the Dublin Core element
     * @param qualifier
     *            the Dublin Core qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     */
    @Deprecated
    public void addDC(String element, String qualifier, String lang,
                      String[] values)
    {
        addMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang, values);
    }

    /**
     * Add a single Dublin Core metadata field. This is appended to existing
     * values. Use <code>clearDC</code> to remove values.
     *
     * @param element
     *            the Dublin Core element
     * @param qualifier
     *            the Dublin Core qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     */
    @Deprecated
    public void addDC(String element, String qualifier, String lang,
                      String value)
    {
        addMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang, value);
    }

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     * <p>
     * If metadata authority control is available, try to get authority
     * values.  The authority confidence depends on whether authority is
     * <em>required</em> or not.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier name, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     */
    public void addMetadata(String schema, String element, String qualifier, String lang,
                            String[] values)
    {
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        if (mam.isAuthorityControlled(fieldKey))
        {
            String authorities[] = new String[values.length];
            int confidences[] = new int[values.length];
            for (int i = 0; i < values.length; ++i)
            {
                Choices c = ChoiceAuthorityManager.getManager().getBestMatch(fieldKey, values[i], getOwningCollectionID(), null);
                authorities[i] = c.values.length > 0 ? c.values[0].authority : null;
                confidences[i] = c.confidence;
            }
            addMetadata(schema, element, qualifier, lang, values, authorities, confidences);
        }
        else
        {
            addMetadata(schema, element, qualifier, lang, values, null, null);
        }
    }

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier name, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     * @param authorities
     *            the external authority key for this value (or null)
     * @param confidences
     *            the authority confidence (default 0)
     */
    public void addMetadata(String schema, String element, String qualifier, String lang,
                            String[] values, String authorities[], int confidences[])
    {
        List<DCValue> dcValueList = dublinCore.getMetadata();
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        boolean authorityControlled = mam.isAuthorityControlled(schema, element, qualifier);
        boolean authorityRequired = mam.isAuthorityRequired(schema, element, qualifier);
        String fieldName = schema+"."+element+((qualifier==null)? "": "."+qualifier);

        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.schema = schema;
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = (lang == null ? null : lang.trim());

            // Logic to set Authority and Confidence:
            //  - normalize an empty string for authority to NULL.
            //  - if authority key is present, use given confidence or NOVALUE if not given
            //  - otherwise, preserve confidence if meaningful value was given since it may document a failed authority lookup
            //  - CF_UNSET signifies no authority nor meaningful confidence.
            //  - it's possible to have empty authority & CF_ACCEPTED if e.g. user deletes authority key
            if (authorityControlled)
            {
                if (authorities != null && authorities[i] != null && authorities[i].length() > 0)
                {
                    dcv.authority = authorities[i];
                    dcv.confidence = confidences == null ? Choices.CF_NOVALUE : confidences[i];
                }
                else
                {
                    dcv.authority = null;
                    dcv.confidence = confidences == null ? Choices.CF_UNSET : confidences[i];
                }
                // authority sanity check: if authority is required, was it supplied?
                // XXX FIXME? can't throw a "real" exception here without changing all the callers to expect it, so use a runtime exception
                if (authorityRequired && (dcv.authority == null || dcv.authority.length() == 0))
                {
                    throw new IllegalArgumentException("The metadata field \"" + fieldName + "\" requires an authority key but none was provided. Vaue=\"" + dcv.value + "\"");
                }
            }
            if (values[i] != null)
            {
                // remove control unicode char
                String temp = values[i].trim();
                char[] dcvalue = temp.toCharArray();
                for (int charPos = 0; charPos < dcvalue.length; charPos++)
                {
                    if (Character.isISOControl(dcvalue[charPos]) &&
                            !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                            !String.valueOf(dcvalue[charPos]).equals("\n") &&
                            !String.valueOf(dcvalue[charPos]).equals("\r"))
                    {
                        dcvalue[charPos] = ' ';
                    }
                }
                dcv.value = String.valueOf(dcvalue);
            }
            else
            {
                dcv.value = null;
            }
            if(!dcValueList.contains(dcv)){
                dcValueList.add(dcv);
                addDetails(fieldName);
                if (values.length > 0)
                {
                    dublinCore.metadataChanged = true;
                }
            }
        }
        updateMetadata();
    }

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use <code>clearDC</code> to remove values.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     */
    public void addMetadata(String schema, String element, String qualifier,
                            String lang, String value)
    {
        String[] valArray = new String[1];
        valArray[0] = value;

        addMetadata(schema, element, qualifier, lang, valArray);
    }

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use <code>clearDC</code> to remove values.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     * @param authority
     *            the external authority key for this value (or null)
     * @param confidence
     *            the authority confidence (default 0)
     */
    public void addMetadata(String schema, String element, String qualifier,
                            String lang, String value, String authority, int confidence)
    {
        String[] valArray = new String[1];
        String[] authArray = new String[1];
        int[] confArray = new int[1];
        valArray[0] = value;
        authArray[0] = authority;
        confArray[0] = confidence;

        addMetadata(schema, element, qualifier, lang, valArray, authArray, confArray);
    }

    /**
     * Clear Dublin Core metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an item.
     *
     * @param element
     *            the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are removed, and <code>Item.ANY</code>
     *            means values with any country code or no country code are
     *            removed.
     */
    @Deprecated
    public void clearDC(String element, String qualifier, String lang)
    {
        clearMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

    /**
     * Clear metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an item.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are removed, and <code>Item.ANY</code>
     *            means values with any country code or no country code are
     *            removed.
     */
    public void clearMetadata(String schema, String element, String qualifier,
                              String lang)
    {
        // We will build a list of values NOT matching the values to clear
        List<DCValue> values = new ArrayList<DCValue>();
        for (DCValue dcv : dublinCore.getMetadata())
        {
            if (!match(schema, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        dublinCore.setMetadata(values);
        dublinCore.metadataChanged = true;
        updateMetadata();
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element to match, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier to match, or <code>Item.ANY</code>
     * @param language
     *            the language to match, or <code>Item.ANY</code>
     * @param dcv
     *            the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    private boolean match(String schema, String element, String qualifier,
                          String language, DCValue dcv)
    {
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(Item.ANY) && !element.equals(dcv.element))
        {
            // Elements do not match, no wildcard
            return false;
        }

        if (qualifier == null)
        {
            // Value must be unqualified
            if (dcv.qualifier != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!qualifier.equals(Item.ANY))
        {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(dcv.qualifier))
            {
                return false;
            }
        }

        if (language == null)
        {
            // Value must be null language to match
            if (dcv.language != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!language.equals(Item.ANY))
        {
            // Not a wildcard, so language must match exactly
            if (!language.equals(dcv.language))
            {
                return false;
            }
        }

        if (!schema.equals(Item.ANY))
        {
            if (dcv.schema != null && !dcv.schema.equals(schema))
            {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
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
                + internalItemId + ",bundle_id=" + b.getID()));

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
        mappingRow.setColumn("item_id", internalItemId);
        mappingRow.setColumn("bundle_id", b.getID());
        DatabaseManager.insert(ourContext, mappingRow);

        ourContext.addEvent(new Event(Event.ADD, Constants.ITEM, internalItemId, Constants.BUNDLE, b.getID(), b.getName()));
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
                + internalItemId + ",bundle_id=" + b.getID()));

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
                internalItemId, b.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.ITEM, internalItemId, Constants.BUNDLE, b.getID(), b.getName()));

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
                + internalItemId));

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
        updateMetadata();

        clearDetails();

        updateLastModified();
    }

    public void updateMetadata() {
        if (dublinCore.metadataChanged) {
            modified = dublinCore.updateMetadata();
        }
    }

    private transient MetadataField[] allMetadataFields = null;
    private MetadataField getMetadataField(DCValue dcv) throws SQLException, AuthorizeException
    {
        if (allMetadataFields == null)
        {
            allMetadataFields = MetadataField.findAll(ourContext);
        }

        if (allMetadataFields != null)
        {
            int schemaID = getMetadataSchemaID(dcv);
            for (MetadataField field : allMetadataFields)
            {
                if (field.getSchemaID() == schemaID &&
                        StringUtils.equals(field.getElement(), dcv.element) &&
                        StringUtils.equals(field.getQualifier(), dcv.qualifier))
                {
                    return field;
                }
            }
        }

        return null;
    }

    private int getMetadataSchemaID(DCValue dcv) throws SQLException
    {
        int schemaID;
        MetadataSchema schema = MetadataSchema.find(ourContext,dcv.schema);
        if (schema == null)
        {
            schemaID = MetadataSchema.DC_SCHEMA_ID;
        }
        else
        {
            schemaID = schema.getSchemaID();
        }
        return schemaID;
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

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, internalItemId, "WITHDRAW"));

        // and all of our authorization policies
        // FIXME: not very "multiple-inclusion" friendly
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Write log
        log.info(LogManager.getHeader(ourContext, "withdraw_item", "user="
                + e.getEmail() + ",item_id=" + internalItemId));
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

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, internalItemId, "REINSTATE"));

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
                + e.getEmail() + ",item_id=" + internalItemId));
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
    public void delete() throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation here. If we don't, it may happen that we remove the
        // metadata but when getting to the point of removing the bundles we get an exception
        // leaving the database in an inconsistent state
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        ourContext.addEvent(new Event(Event.DELETE, Constants.ITEM, internalItemId, getHandle()));

        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(ourContext, internalItemId);

        Version version=null;
        Version previous=null;
        int objectId=-1;
        if(history!=null){
            version  = versioningService.getVersion(ourContext, this);
            previous = history.getPrevious(version);
            if(previous!=null)
                objectId=previous.getItem().getID();
        }

        ourContext.addEvent(new Event(Event.DELETE, Constants.ITEM, internalItemId, Constants.ITEM, objectId, getHandle()));

        log.info(LogManager.getHeader(ourContext, "delete_item", "item_id="
                + internalItemId));

        // Remove from cache
        ourContext.removeCached(this, internalItemId);

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
//          Remove from indices
            IndexBrowse ib = new IndexBrowse(ourContext);
            ib.itemRemoved(this);

            // Reinstate previous version if present
            if(previous!=null){
                ib.indexItem(previous.getItem());
            }
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new SQLException(e.getMessage(), e);
        }

        //Before we remove our metadata it is important to check if we are a part of something
        DSpaceObject dso = DryadWorkflowUtils.getDataPackage(ourContext, this);
        if(dso != null){
            //We have a valid dso
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            try{
                Item publication = (Item) dso;
                DCValue[] datasets = publication.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", Item.ANY);
                publication.clearMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", Item.ANY);
                for (DCValue identifier : datasets) {

                    DSpaceObject dataset = null;
                    try {
                        dataset = identifierService.resolve(ourContext, identifier.value);
                    } catch (IdentifierNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IdentifierNotResolvableException e) {
                        throw new RuntimeException(e);
                    }


                    //Check if this is our current dataset, if so no need to readd it
                    if(dataset != null && dataset instanceof Item && dataset.getID() == internalItemId)
                        continue;

                    //Add our identifier
                    publication.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", null, identifier.value);

                }
                publication.update();
            } catch (IllegalArgumentException e){
                log.error(LogManager.getHeader(ourContext, "Error while deleting a data file", "data file id:" + internalItemId), e);
            }
        }


        // Remove version attached to the item
        if(versioningService.getVersion(ourContext, this)!=null)
            versioningService.removeVersion(ourContext, this);
        else{
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            try {
                identifierService.delete(ourContext, this);
            } catch (IdentifierException e) {
                throw new RuntimeException(e);
            }
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

        // Finally remove item row
        DatabaseManager.delete(ourContext, itemRow);
    }

    /**
     * Remove item and all its sub-structure from the context cache.
     * Useful in batch processes where a single context has a long,
     * multi-item lifespan
     */
    public void decache() throws SQLException
    {
        // Remove item and it's submitter from cache
        ourContext.removeCached(this, internalItemId);
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
        if (internalItemId != other.getID())
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
     * Utility method to remove all descriptive metadata associated with the item from
     * the database (regardless of in-memory version)
     *
     * @throws SQLException
     */
    private void removeMetadataFromDatabase() throws SQLException
    {
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM MetadataValue WHERE item_id= ? ", internalItemId);
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
        // remove the submit authorization policies
        // and replace them with the collection's default READ policies
        List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(ourContext, c, Constants.DEFAULT_ITEM_READ);

        // MUST have default policies
        if (policies.size() < 1)
        {
            throw new java.sql.SQLException("Collection " + c.getID()
                    + " has no default item READ policies");
        }

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        for (ResourcePolicy rp : policies)
        {
            rp.setAction(Constants.READ);
        }

        replaceAllItemPolicies(policies);

        policies = AuthorizeManager.getPoliciesActionFilter(ourContext, c, Constants.DEFAULT_BITSTREAM_READ);

        if (policies.size() < 1)
        {
            throw new java.sql.SQLException("Collection " + c.getID()
                    + " has no default bitstream READ policies");
        }

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        for (ResourcePolicy rp : policies)
        {
            rp.setAction(Constants.READ);
        }

        replaceAllBitstreamPolicies(policies);

        log.debug(LogManager.getHeader(ourContext, "item_inheritCollectionDefaultPolicies",
                "item_id=" + internalItemId));
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
                    "item_id=" + internalItemId + ", from " +
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

            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, internalItemId, null));
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

        if(ourContext.getCurrentUser()==null) return false;

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
        DCValue t[] = getMetadata("dc", "title", null, Item.ANY);
        return (t.length >= 1) ? t[0].value : null;
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
                                                   String schema, String element, String qualifier, String value, Boolean in_archive)
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

        String query = "SELECT item.* FROM metadatavalue,item WHERE "+
                "item.item_id = metadatavalue.item_id AND metadata_field_id = ?";
        if (in_archive) {
            query += " AND item.in_archive='1'";
        }
        TableRowIterator rows = null;
        if (Item.ANY.equals(value))
        {
            rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID());
        }
        else
        {
            query += " AND metadatavalue.text_value = ?";
            rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(), value);
        }
        return new ItemIterator(context, rows);
    }

    /**
     * Returns an iterator of Items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     * NOTE: only searches items in the archive
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
        return findByMetadataField(context, schema, element, qualifier, value, true);
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
                            "WHERE template_item_id = ?",internalItemId);
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
                            "WHERE template_item_id = ?",internalItemId);
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
                        "AND item.item_id = metadatavalue.item_id AND metadata_field_id = ? AND authority = ?",
                mdf.getFieldID(), value);
        return new ItemIterator(context, rows);
    }

    class MetadataCache
    {
        List<DCValue> metadata = null;
        boolean metadataChanged = true;

        List<DCValue> getMetadata() {
            if ((metadataChanged==true)||(metadata == null)) {
                metadata = new ArrayList<DCValue>();

                // Get Dublin Core metadata
                try {
                    TableRowIterator tri = retrieveMetadata();
                    if (tri != null) {
                        while (tri.hasNext()) {
                            TableRow resultRow = tri.next();

                            // Get the associated metadata field and schema information
                            int fieldID = resultRow.getIntColumn("metadata_field_id");
                            MetadataField field = MetadataField.find(ourContext, fieldID);

                            if (field == null) {
                                log.error("Loading item - cannot find metadata field " + fieldID);
                            } else {
                                MetadataSchema schema = MetadataSchema.find(ourContext, field.getSchemaID());
                                if (schema == null) {
                                    log.error("Loading item - cannot find metadata schema " + field.getSchemaID() + ", field " + fieldID);
                                } else {
                                    // Make a DCValue object
                                    DCValue dcv = new DCValue();
                                    dcv.element = field.getElement();
                                    dcv.qualifier = field.getQualifier();
                                    dcv.value = resultRow.getStringColumn("text_value");
                                    dcv.language = resultRow.getStringColumn("text_lang");
                                    //dcv.namespace = schema.getNamespace();
                                    dcv.schema = schema.getName();
                                    dcv.authority = resultRow.getStringColumn("authority");
                                    dcv.confidence = resultRow.getIntColumn("confidence");

                                    // Add it to the list
                                    metadata.add(dcv);
                                }
                            }
                        }
                        tri.close();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("couldn't access database metadata for item " + internalItemId, e);
                }
            }
            return metadata;
        }

        void setMetadata(List<DCValue> m)
        {
            metadata = m;
            metadataChanged = true;
        }

        TableRowIterator retrieveMetadata() throws SQLException
        {
            if (internalItemId > 0)
            {
                return DatabaseManager.queryTable(ourContext, "MetadataValue",
                        "SELECT * FROM MetadataValue WHERE item_id= ? ORDER BY metadata_field_id, place",
                        internalItemId);
            }

            return null;
        }

        boolean updateMetadata() {
            boolean hasBeenModified = false;

            metadataChanged = false;

            // Map counting number of values for each element/qualifier.
            // Keys are Strings: "element" or "element.qualifier"
            // Values are Integers indicating number of values written for a
            // element/qualifier
            Map<String,Integer> elementCount = new HashMap<String,Integer>();

            try {
                List<DCValue> currMetadata = getMetadata();
                // Arrays to store the working information required
                int[]     placeNum = new int[currMetadata.size()];
                boolean[] storedDC = new boolean[currMetadata.size()];
                MetadataField[] dcFields = new MetadataField[currMetadata.size()];

                // Work out the place numbers for the in memory DC
                for (int dcIdx = 0; dcIdx < currMetadata.size(); dcIdx++)
                {
                    DCValue dcv = currMetadata.get(dcIdx);

                    // Work out the place number for ordering
                    int current = 0;

                    // Key into map is "element" or "element.qualifier"
                    String key = dcv.element + ((dcv.qualifier == null) ? "" : ("." + dcv.qualifier));

                    Integer currentInteger = elementCount.get(key);
                    if (currentInteger != null)
                    {
                        current = currentInteger.intValue();
                    }

                    current++;
                    elementCount.put(key, Integer.valueOf(current));

                    // Store the calculated place number, reset the stored flag, and cache the metadatafield
                    placeNum[dcIdx] = current;
                    storedDC[dcIdx] = false;
                        dcFields[dcIdx] = getMetadataField(dcv);
                        if (dcFields[dcIdx] == null) {
                            // Bad DC field, log and throw exception
                            log.warn(LogManager
                                    .getHeader(ourContext, "bad_dc",
                                            "Bad DC field. schema=" + dcv.schema
                                                    + ", element: \""
                                                    + ((dcv.element == null) ? "null"
                                                    : dcv.element)
                                                    + "\" qualifier: \""
                                                    + ((dcv.qualifier == null) ? "null"
                                                    : dcv.qualifier)
                                                    + "\" value: \""
                                                    + ((dcv.value == null) ? "null"
                                                    : dcv.value) + "\""));

                            throw new SQLException("bad_dublin_core "
                                    + "schema=" + dcv.schema + ", "
                                    + dcv.element
                                    + " " + dcv.qualifier);
                        }
                }

                // Now the precalculations are done, iterate through the existing metadata
                // looking for matches
                TableRowIterator tri = retrieveMetadata();

                if (tri != null) {
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();
                        // Assume that we will remove this row, unless we get a match
                        boolean removeRow = true;

                        // Go through the in-memory metadata, unless we've already decided to keep this row
                        for (int dcIdx = 0; dcIdx < currMetadata.size() && removeRow; dcIdx++)
                        {
                            // Only process if this metadata has not already been matched to something in the DB
                            if (!storedDC[dcIdx])
                            {
                                boolean matched = true;
                                DCValue dcv   = currMetadata.get(dcIdx);

                                // Check the metadata field is the same
                                if (matched && dcFields[dcIdx].getFieldID() != tr.getIntColumn("metadata_field_id"))
                                {
                                    matched = false;
                                }

                                // Check the place is the same
                                if (matched && placeNum[dcIdx] != tr.getIntColumn("place"))
                                {
                                    matched = false;
                                }

                                // Check the text is the same
                                if (matched)
                                {
                                    String text = tr.getStringColumn("text_value");
                                    if (dcv.value == null && text == null)
                                    {
                                        matched = true;
                                    }
                                    else if (dcv.value != null && dcv.value.equals(text))
                                    {
                                        matched = true;
                                    }
                                    else
                                    {
                                        matched = false;
                                    }
                                }

                                // Check the language is the same
                                if (matched)
                                {
                                    String lang = tr.getStringColumn("text_lang");
                                    if (dcv.language == null && lang == null)
                                    {
                                        matched = true;
                                    }
                                    else if (dcv.language != null && dcv.language.equals(lang))
                                    {
                                        matched = true;
                                    }
                                    else
                                    {
                                        matched = false;
                                    }
                                }

                                // check that authority and confidence match
                                if (matched)
                                {
                                    String auth = tr.getStringColumn("authority");
                                    int conf = tr.getIntColumn("confidence");
                                    if (!((dcv.authority == null && auth == null) ||
                                            (dcv.authority != null && auth != null && dcv.authority.equals(auth))
                                                    && dcv.confidence == conf))
                                    {
                                        matched = false;
                                    }
                                }

                                // If the db record is identical to the in memory values
                                if (matched)
                                {
                                    // Flag that the metadata is already in the DB
                                    storedDC[dcIdx] = true;

                                    // Flag that we are not going to remove the row
                                    removeRow = false;
                                }
                            }
                        }

                        // If after processing all the metadata values, we didn't find a match
                        // delete this row from the DB
                        if (removeRow)
                        {
                            DatabaseManager.delete(ourContext, tr);
                            hasBeenModified = true;
                        }
                    }
                    tri.close();
                }

                // Add missing in-memory DC
                for (int dcIdx = 0; dcIdx < getMetadata().size(); dcIdx++) {
                    // Only write values that are not already in the db
                    if (!storedDC[dcIdx]) {
                        DCValue dcv = getMetadata().get(dcIdx);

                        // Write DCValue
                        MetadataValue metadata = new MetadataValue();
                        metadata.setItemId(internalItemId);
                        metadata.setFieldId(dcFields[dcIdx].getFieldID());
                        metadata.setValue(dcv.value);
                        metadata.setLanguage(dcv.language);
                        metadata.setPlace(placeNum[dcIdx]);
                        metadata.setAuthority(dcv.authority);
                        metadata.setConfidence(dcv.confidence);
                        try {
                            metadata.create(ourContext);
                        } catch (Exception e) {
                            throw new RuntimeException("Couldn't create metadata for item " + internalItemId, e);
                        }
                        hasBeenModified = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return hasBeenModified;
        }
    }

   public static ItemIterator findByLastModifiedGreaterThan(Context context, Date lastGenerateDate) throws SQLException{

       String myQuery = "SELECT * FROM item WHERE in_archive=true and withdrawn=false and  last_Modified >= ?";

       java.sql.Timestamp ts = new java.sql.Timestamp(lastGenerateDate.getTime());
       TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery, ts);

       return new ItemIterator(context, rows);
   }

   /**
    * Force an update to last_modified
    */
   public void touch() {
        modified = true;
   }

    public static ItemIterator findAllWithdrawn(Context context) throws SQLException{
        String myQuery = "SELECT * FROM item WHERE withdrawn='1'";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }
}
