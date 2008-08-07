/*
 * Item.java
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

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
    public final static String ANY = "*";

    /** log4j category */
    private static Logger log = Logger.getLogger(Item.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow itemRow;

    /** The e-person who submitted this item */
    private EPerson submitter;

    /** The bundles in this item - kept in sync with DB */
    private List<Bundle> bundles;

    /** The Dublin Core metadata - a list of DCValue objects. */
    private List<DCValue> dublinCore;

    /** Handle, if any */
    private String handle;

    /**
     * True if the Dublin Core has changed since reading from the DB or the last
     * update()
     */
    private boolean dublinCoreChanged;

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
        ourContext = context;
        itemRow = row;
        dublinCoreChanged = false;
        modified = false;
        dublinCore = new ArrayList<DCValue>();
        clearDetails();
 
        // Get Dublin Core metadata
        TableRowIterator tri = retrieveMetadata();

        try
        {
            while (tri.hasNext())
            {
                TableRow resultRow = tri.next();

                // Get the associated metadata field and schema information
                int fieldID = resultRow.getIntColumn("metadata_field_id");
                MetadataField field = MetadataField.find(context, fieldID);

                if (field == null)
                {
                    log.error("Loading item - cannot found metadata field "
                            + fieldID);
                }
                else
                {
                    MetadataSchema schema = MetadataSchema.find(
                            context, field.getSchemaID());

                    // Make a DCValue object
                    DCValue dcv = new DCValue();
                        dcv.element = field.getElement();
                        dcv.qualifier = field.getQualifier();
                    dcv.value = resultRow.getStringColumn("text_value");
                    dcv.language = resultRow.getStringColumn("text_lang");
                        //dcv.namespace = schema.getNamespace();
                        dcv.schema = schema.getName();

                    // Add it to the list
                    dublinCore.add(dcv);
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        // Get our Handle if any
        handle = HandleManager.findHandle(context, this);

        // Cache ourselves
        context.cache(this, row.getIntColumn("item_id"));
    }

    private TableRowIterator retrieveMetadata() throws SQLException
    {
        return DatabaseManager.queryTable(ourContext, "MetadataValue",
                "SELECT * FROM MetadataValue WHERE item_id= ? ORDER BY metadata_field_id, place",
                itemRow.getIntColumn("item_id"));
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
        context.setIgnoreAuthorization(true);
        i.update();
        context.setIgnoreAuthorization(false);

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

        myCollection = Collection.find(ourContext, cid);

        return myCollection;
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
        for (DCValue dcv : dublinCore)
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
            tokens[i] = dcf.nextToken().toLowerCase().trim();
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

    /**
     * Add Dublin Core metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     * 
     * @param element
     *            the Dublin Core element
     * @param qualifier
     *            the Dublin Core qualifer, or <code>null</code> for
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
     *            the Dublin Core qualifer, or <code>null</code> for
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
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifer name, or <code>null</code> for
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
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.schema = schema;
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = lang;
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
            dublinCore.add(dcv);
            addDetails(schema+"."+element+((qualifier==null)? "": "."+qualifier));

        }

        if (values.length > 0)
        {
            dublinCoreChanged = true;
        }
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
     *            the metadata qualifer, or <code>null</code> for
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
        for (DCValue dcv : dublinCore)
        {
            if (!match(schema, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        dublinCore = values;
        dublinCoreChanged = true;
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the elemenent, qualifier and language passed
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
        else if (!schema.equals(Item.ANY))
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
                tri.close();
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

                for (int i = 0; i < parents.length; i++)
                {
                    communities.add(parents[i]);
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
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
                    tri.close();
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
        TableRow mappingRow = DatabaseManager.create(ourContext, "item2bundle");
        mappingRow.setColumn("item_id", getID());
        mappingRow.setColumn("bundle_id", b.getID());
        DatabaseManager.update(ourContext, mappingRow);

        ourContext.addEvent(new Event(Event.ADD, Constants.ITEM, getID(), Constants.BUNDLE, b.getID(), b.getName()));
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

        ourContext.addEvent(new Event(Event.REMOVE, Constants.ITEM, getID(), Constants.BUNDLE, b.getID(), b.getName()));

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
                tri.close();
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

        Bitstream[] bsArray = new Bitstream[bitstreamList.size()];
        bsArray = (Bitstream[]) bitstreamList.toArray(bsArray);

        return bsArray;
    }

    /**
     * Store a copy of the license a user granted in this item.
     * 
     * @param license
     *            the license the user granted
     * @param eperson
     *            the eperson who granted the license
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void licenseGranted(String license, EPerson eperson)
            throws SQLException, IOException, AuthorizeException
    {
        // Put together text to store
        String licenseText = "License granted by " + eperson.getFullName()
                + " (" + eperson.getEmail() + ") on "
                + DCDate.getCurrent().toString() + " (GMT):\n\n" + license;

        // Store text as a bitstream
        byte[] licenseBytes = licenseText.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(licenseBytes);
        Bitstream b = createSingleBitstream(bais, "LICENSE");

        // Now set the format and name of the bitstream
        b.setName("license.txt");
        b.setSource("Written by org.dspace.content.Item");

        // Find the License format
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(ourContext,
                "License");
        b.setFormat(bf);

        b.update();
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

        // Set the last modified date
        itemRow.setColumn("last_modified", new Date());

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
                }
            }
        }

        // Make sure that withdrawn and in_archive are non-null
        if (itemRow.isColumnNull("in_archive"))
        {
            itemRow.setColumn("in_archive", false);
        }

        if (itemRow.isColumnNull("withdrawn"))
        {
            itemRow.setColumn("withdrawn", false);
        }

        // Map counting number of values for each element/qualifier.
        // Keys are Strings: "element" or "element.qualifier"
        // Values are Integers indicating number of values written for a
        // element/qualifier
        Map<String,Integer> elementCount = new HashMap<String,Integer>();

        DatabaseManager.update(ourContext, itemRow);

        // Redo Dublin Core if it's changed
        if (dublinCoreChanged)
        {
            // Arrays to store the working information required
            int[]     placeNum = new int[dublinCore.size()];
            boolean[] storedDC = new boolean[dublinCore.size()];
            MetadataField[] dcFields = new MetadataField[dublinCore.size()];

            // Work out the place numbers for the in memory DC
            for (int dcIdx = 0; dcIdx < dublinCore.size(); dcIdx++)
            {
                DCValue dcv = dublinCore.get(dcIdx);

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
                if (dcFields[dcIdx] == null)
                {
                    // Bad DC field, log and throw exception
                    log.warn(LogManager
                            .getHeader(ourContext, "bad_dc",
                                    "Bad DC field. schema="+String.valueOf(dcv.schema)
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
                            + "schema="+dcv.schema+", "
                            + dcv.element
                            + " " + dcv.qualifier);
                }
            }

            // Now the precalculations are done, iterate through the existing metadata
            // looking for matches
            TableRowIterator tri = retrieveMetadata();
            if (tri != null)
            {
                try
                {
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();
                        // Assume that we will remove this row, unless we get a match
                        boolean removeRow = true;

                        // Go through the in-memory metadata, unless we've already decided to keep this row
                        for (int dcIdx = 0; dcIdx < dublinCore.size() && removeRow; dcIdx++)
                        {
                            // Only process if this metadata has not already been matched to something in the DB
                            if (!storedDC[dcIdx])
                            {
                                boolean matched = true;
                                DCValue dcv   = dublinCore.get(dcIdx);

                                // Check the metadata field is the same
                                if (matched && dcFields[dcIdx].getFieldID() != tr.getIntColumn("metadata_field_id"))
                                    matched = false;

                                // Check the place is the same
                                if (matched && placeNum[dcIdx] != tr.getIntColumn("place"))
                                    matched = false;

                                // Check the text is the same
                                if (matched)
                                {
                                    String text = tr.getStringColumn("text_value");
                                    if (dcv.value == null && text == null)
                                        matched = true;
                                    else if (dcv.value != null && dcv.value.equals(text))
                                        matched = true;
                                    else
                                        matched = false;
                                }

                                // Check the language is the same
                                if (matched)
                                {
                                    String lang = tr.getStringColumn("text_lang");
                                    if (dcv.language == null && lang == null)
                                        matched = true;
                                    else if (dcv.language != null && dcv.language.equals(lang))
                                        matched = true;
                                    else
                                        matched = false;
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
                        }
                    }
                }
                finally
                {
                    tri.close();
                }
            }

            // Add missing in-memory DC
            for (int dcIdx = 0; dcIdx < dublinCore.size(); dcIdx++)
            {
                // Only write values that are not already in the db
                if (!storedDC[dcIdx])
                {
                    DCValue dcv = dublinCore.get(dcIdx);

                    // Write DCValue
                    MetadataValue metadata = new MetadataValue();
                    metadata.setItemId(getID());
                    metadata.setFieldId(dcFields[dcIdx].getFieldID());
                    metadata.setValue(dcv.value);
                    metadata.setLanguage(dcv.language);
                    metadata.setPlace(placeNum[dcIdx]);
                    metadata.create(ourContext);
                }
            }

            ourContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.ITEM, getID(), getDetails()));
            dublinCoreChanged = false;
            clearDetails();
        }

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), null));
            modified = false;
        }
    }

    private MetadataField getMetadataField(DCValue dcv) throws SQLException, AuthorizeException
    {
        return MetadataField.findByElement(ourContext,
                        getMetadataSchemaID(dcv), dcv.element, dcv.qualifier);
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
        String timestamp = DCDate.getCurrent().toString();

        // Build some provenance data while we're at it.
        String collectionProv = "";
        Collection[] colls = getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            collectionProv = collectionProv + colls[i].getMetadata("name")
                    + " (ID: " + colls[i].getID() + ")\n";
        }

        // Check permission. User either has to have REMOVE on owning collection
        // or be COLLECTION_EDITOR of owning collection
        if (AuthorizeManager.authorizeActionBoolean(ourContext,
                getOwningCollection(), Constants.COLLECTION_ADMIN)
                || AuthorizeManager.authorizeActionBoolean(ourContext,
                        getOwningCollection(), Constants.REMOVE))
        {
            // authorized
        }
        else
        {
            throw new AuthorizeException(
                    "To withdraw item must be COLLECTION_ADMIN or have REMOVE authorization on owning Collection");
        }

        // Set withdrawn flag. timestamp will be set; last_modified in update()
        itemRow.setColumn("withdrawn", true);

        // in_archive flag is now false
        itemRow.setColumn("in_archive", false);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();
        String prov = "Item withdrawn by " + e.getFullName() + " ("
                + e.getEmail() + ") on " + timestamp + "\n"
                + "Item was in collections:\n" + collectionProv
                + InstallItem.getBitstreamProvenanceMessage(this);

        addDC("description", "provenance", "en", prov);

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), "WITHDRAW"));

        // and all of our authorization policies
        // FIXME: not very "multiple-inclusion" friendly
        AuthorizeManager.removeAllPolicies(ourContext, this);

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
        String timestamp = DCDate.getCurrent().toString();

        // Check permission. User must have ADD on all collections.
        // Build some provenance data while we're at it.
        String collectionProv = "";
        Collection[] colls = getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            collectionProv = collectionProv + colls[i].getMetadata("name")
                    + " (ID: " + colls[i].getID() + ")\n";
            AuthorizeManager.authorizeAction(ourContext, colls[i],
                    Constants.ADD);
        }

        // Clear withdrawn flag
        itemRow.setColumn("withdrawn", false);

        // in_archive flag is now true
        itemRow.setColumn("in_archive", true);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();
        String prov = "Item reinstated by " + e.getFullName() + " ("
                + e.getEmail() + ") on " + timestamp + "\n"
                + "Item was in collections:\n" + collectionProv
                + InstallItem.getBitstreamProvenanceMessage(this);

        addDC("description", "provenance", "en", prov);

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), "REINSTATE"));

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
        ourContext.addEvent(new Event(Event.DELETE, Constants.ITEM, getID(), getHandle()));

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
//        	 FIXME: there is an exception handling problem here
        try
        {
//            	 Remove from indicies
            IndexBrowse ib = new IndexBrowse(ourContext);
            ib.itemRemoved(this);
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new SQLException(e.getMessage());
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
     * @param other
     *            object to compare to
     * @return <code>true</code> if object passed in represents the same item
     *         as this object
     */
    public boolean equals(DSpaceObject other)
    {
        if (this.getType() == other.getType())
        {
            if (this.getID() == other.getID())
            {
                return true;
            }
        }

        return false;
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
                "DELETE FROM MetadataValue WHERE item_id= ? ",
                getID());
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
    public void replaceAllItemPolicies(List newpolicies) throws SQLException,
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
    public void replaceAllBitstreamPolicies(List newpolicies)
            throws SQLException, AuthorizeException
    {
        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                Bitstream mybitstream = bs[j];

                // change bitstream policies
                AuthorizeManager.removeAllPolicies(ourContext, bs[j]);
                AuthorizeManager.addPolicies(ourContext, newpolicies, bs[j]);
            }

            // change bundle policies
            AuthorizeManager.removeAllPolicies(ourContext, mybundle);
            AuthorizeManager.addPolicies(ourContext, newpolicies, mybundle);
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
                Bitstream mybitstream = bs[j];

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
        List policies = AuthorizeManager.getPoliciesActionFilter(ourContext, c,
                Constants.DEFAULT_ITEM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        Iterator i = policies.iterator();

        // MUST have default policies
        if (!i.hasNext())
        {
            throw new java.sql.SQLException("Collection " + c.getID()
                    + " has no default item READ policies");
        }

        while (i.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) i.next();
            rp.setAction(Constants.READ);
        }

        replaceAllItemPolicies(policies);

        policies = AuthorizeManager.getPoliciesActionFilter(ourContext, c,
                Constants.DEFAULT_BITSTREAM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        i = policies.iterator();

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
     * Moves the item from one collection to another one
     * 
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move (Collection from, Collection to) throws SQLException, AuthorizeException, IOException 
    {
        // Move the Item from one Collection to the other
        to.addItem(this);
        from.removeItem(this);

        // If we are moving from the owning collection, update that too
        if (isOwningCollection(from))
    	{
    		setOwningCollection(to);
    		update();
    	}
        else
        {
            // Although we haven't actually updated anything within the item
            // we'll tell the event system that it has, so that any consumers that
            // care about the structure of the repository can take account of the move

            // Note that updating the owning collection above will have the same effect,
            // so we only do this here if the owning collection hasn't changed.
            
            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), null));
        }
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
        if (getOwningCollection().canEditBoolean())
        {
            return true;
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        if (AuthorizeManager.authorizeActionBoolean(ourContext,
                getOwningCollection(), Constants.COLLECTION_ADMIN))
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
}
