/*
 * Item.java
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.dspace.administer.DCType;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.Browse;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing an item in DSpace.
 * <P>
 * This class holds in memory the item Dublin Core metadata, the bundles in
 * the item, and the bitstreams in those bundles.  When modifying the item,
 * if you modify the Dublin Core or the "in archive" flag, you must call
 * <code>update</code> for the changes to be written to the database.  Creating,
 * adding or removing bundles or bitstreams has immediate effect in the
 * database.
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Item
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
    private List bundles;

    /** The Dublin Core metadata - a list of DCValue objects. */
    private List dublinCore;

    /**
     * True if the Dublin Core has changed since reading from the DB
     * or the last update()
     */
    private boolean dublinCoreChanged;


    /**
     * Construct an item with the given table row
     *
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    Item(Context context, TableRow row)
        throws SQLException
    {
        ourContext = context;
        itemRow = row;
        dublinCoreChanged = false;
        dublinCore = new ArrayList();
        bundles = new ArrayList();

        // Get the submitter
        submitter = null;
        if (!itemRow.isColumnNull("submitter_id"))
        {
            submitter = EPerson.find(ourContext,
                itemRow.getIntColumn("submitter_id"));
        }

        // Get bundles
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "bundle",
            "SELECT bundle.* FROM bundle, item2bundle WHERE " +
                "item2bundle.bundle_id=bundle.bundle_id AND " +
                "item2bundle.item_id=" +
                itemRow.getIntColumn("item_id") + ";");

        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();

            // First check the cache
            Bundle fromCache = (Bundle) context.fromCache(
                Bundle.class, r.getIntColumn("bundle_id"));

            if (fromCache != null)
            {
                bundles.add(fromCache);
            }
            else
            {
                bundles.add(new Bundle(ourContext, r));
            }
        }

        // Get Dublin Core metadata
        tri = DatabaseManager.query(ourContext, "dcvalue",
            "SELECT * FROM dcvalue WHERE item_id=" +
                itemRow.getIntColumn("item_id") +
                " ORDER BY dc_type_id, place;");

        while (tri.hasNext())
        {
            TableRow resultRow = (TableRow) tri.next();

            // Get the Dublin Core type
            TableRow typeRow = DatabaseManager.find(ourContext,
                "dctyperegistry",
                resultRow.getIntColumn("dc_type_id"));

            // Make a DCValue object
            DCValue dcv = new DCValue();
            dcv.element = typeRow.getStringColumn("element");
            dcv.qualifier = typeRow.getStringColumn("qualifier");
            dcv.value = resultRow.getStringColumn("text_value");
            dcv.language = resultRow.getStringColumn("text_lang");

            // Add it to the list
            dublinCore.add(dcv);
        }

        // Cache ourselves
        context.cache(this, row.getIntColumn("item_id"));
    }


    /**
     * Get an item from the database.  The item, its Dublin Core metadata,
     * and the bundle and bitstream metadata are all loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       Internal ID of the item
     *
     * @return  the item, or null if the internal ID is invalid.
     */
    public static Item find(Context context, int id)
        throws SQLException
    {
        // First check the cache
        Item fromCache = (Item) context.fromCache(Item.class, id);
            
        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context,
            "item",
            id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_item",
                    "not_found,item_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_item",
                    "item_id=" + id));
            }

            return new Item(context, row);
        }
    }


    /**
     * Create a new item, with a new internal ID.  This method is not public,
     * since items need to be created as workspace items.  Authorisation is
     * the responsibility of the caller.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created item
     */
    public static Item create(Context context)
        throws SQLException
    {
        TableRow row = DatabaseManager.create(context, "item");

        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context,
                "create_item",
                "item_id=" + row.getIntColumn("item_id")));
        }


        return new Item(context, row);
    }


    /**
     * Get all the items in the archive.  Only items with the "in archive"
     * flag set are included.  The order of the list is indeterminate.
     *
     * @param  context  DSpace context object
     *
     * @return  an iterator over the items in the archive.
     */
    public static ItemIterator findAll(Context context)
        throws SQLException
    {
        TableRowIterator rows = DatabaseManager.query(context,
            "item",
            "SELECT * FROM item WHERE in_archive=true;");

        return new ItemIterator(context, rows);
    }


    /**
     * Find all the items in the archive by a given submitter.  The order is
     * indeterminate.  Only items with the "in archive" flag set are included.
     *
     * @param  context  DSpace context object
     * @param  eperson  the submitter
     *
     * @return an iterator over the items submitted by eperson
     */
    public static ItemIterator findBySubmitter(Context context, EPerson eperson)
        throws SQLException
    {
        TableRowIterator rows = DatabaseManager.query(context,
            "item",
            "SELECT * FROM item WHERE in_archive=true AND submitter_id=" +
                eperson.getID() + ";");

        return new ItemIterator(context, rows);
    }


    /**
     * Get the internal ID of this item.  In general, this shouldn't be
     * exposed to users
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return itemRow.getIntColumn("item_id");
    }


    /**
     * Find out if the item is part of the main archive
     *
     * @return  true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return itemRow.getBooleanColumn("in_archive");
    }


    /**
     * Set the "is_archived" flag.  This is public and only
     * <code>WorkflowItem.archive()</code> should set this.
     *
     * @param isArchived  new value for the flag
     */
    public void setArchived(boolean isArchived)
    {
        itemRow.setColumn("in_archive", isArchived);
    }


    /**
     * Get Dublin Core metadata for the item.  Passing in a <code>null</code>
     * value only matches Dublin Core fields where that qualifier or languages
     * is actually <code>null</code>.  Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     *   Return values of the unqualified "title" field, in any language.
     *   Qualified title fields (e.g. "title.uniform") are NOT returned:<P>
     *      <code>item.getDC( "title", null, Item.ANY );</code>
     * <P>
     *   Return all US English values of the "title" element, with any qualifier
     *   (including unqualified):<P>
     *      <code>item.getDC( "title", Item.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant.  When retrieving with wildcards, values of
     * a particular element/qualifier/language combinations will be adjacent,
     * but the overall ordering of the combinations is indeterminate.
     *
     * @param  element    the Dublin Core element.  <code>Item.ANY</code>
     *                    matches any element.  <code>null</code> doesn't
     *                    really make sense as all DC must have an element.
     * @param  qualifier  the qualifier.  <code>null</code> means unqualified,
     *                    and <code>Item.ANY</code> means any qualifier
     *                    (including unqualified.)
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means only values with no language
     *                    are returned, and <code>Item.ANY</code> means values
     *                    with any country code or no country code are returned.
     *
     * @return  Dublin Core fields that match the parameters
     */
    public DCValue[] getDC(String element, String qualifier, String lang)
    {
        // Build up list of matching values
        List values = new ArrayList();
        Iterator i = dublinCore.iterator();

        while (i.hasNext())
        {
            DCValue dcv = (DCValue) i.next();

            if (match(element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                DCValue copy = new DCValue();
                copy.element = dcv.element;
                copy.qualifier = dcv.qualifier;
                copy.value = dcv.value;
                copy.language = dcv.language;

                values.add(copy);
            }
        }

        // Create an array of matching values
        DCValue[] valueArray = new DCValue[values.size()];
        valueArray = (DCValue[]) values.toArray(valueArray);

        return valueArray;
    }


    /**
     * Add Dublin Core metadata fields.  These are appended to existing values.
     * Use <code>clearDC</code> to remove values.  The ordering of values
     * passed in is maintained.
     *
     * @param  element    the Dublin Core element
     * @param  qualifier  the Dublin Core qualifer, or <code>null</code> for
     *                    unqualified
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means the value has no language
     *                    (for example, a date).
     * @param  values     the values to add.
     */
    public void addDC(String element,
                      String qualifier,
                      String lang,
                      String[] values)
    {
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = lang;
            dcv.value = values[i];
            dublinCore.add(dcv);
        }

        if (values.length > 0)
        {
            dublinCoreChanged = true;
        }
    }


    /**
     * Add a single Dublin Core metadata field.  This is appended to existing
     * values.  Use <code>clearDC</code> to remove values.
     *
     * @param  element    the Dublin Core element
     * @param  qualifier  the Dublin Core qualifer, or <code>null</code> for
     *                    unqualified
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means the value has no language
     *                    (for example, a date).
     * @param  value      the value to add.
     */
    public void addDC(String element,
                      String qualifier,
                      String lang,
                      String value)
    {
        String[] valArray = new String[1];
        valArray[0] = value;

        addDC(element, qualifier, lang, valArray);
    }


    /**
     * Clear Dublin Core metadata values.  As with <code>addDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.  <code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code>
     * will remove all Dublin Core metadata associated with an item.
     *
     * @param element     the Dublin Core element to remove, or
     *                    <code>Item.ANY</code>
     * @param qualifier   the qualifier.  <code>null</code> means unqualified,
     *                    and <code>Item.ANY</code> means any qualifier
     *                    (including unqualified.)
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means only values with no language
     *                    are removed, and <code>Item.ANY</code> means values
     *                    with any country code or no country code are removed.
     */
    public void clearDC(String element, String qualifier, String lang)
    {
        // We will build a list of values NOT matching the values to clear
        List values = new ArrayList();
        Iterator i = dublinCore.iterator();

        while (i.hasNext())
        {
            DCValue dcv = (DCValue) i.next();

            if (!match(element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        dublinCore = values;
    }


    /**
     * Utility method for pattern-matching Dublin Core.  This method will
     * return <code>true</code> if the element, qualifier and language passed
     * in match the element, qualifier and language of the Dublin Core value
     * passed in.  Any or all of the elemenent, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param element    the element to match, or <code>Item.ANY</code>
     * @param qualifier  the qualifier to match, or <code>Item.ANY</code>
     * @param language   the language to match, or <code>Item.ANY</code>
     * @param dcv        the Dublin Core value
     *
     * @return  <code>true</code> if there is a match
     */
    private boolean match(String element, String qualifier, String language,
        DCValue dcv)
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

        // If we get this far, we have a match
        return true;
    }


    /**
     * Get the e-person that originally submitted this item
     *
     * @return  the submitter
     */
    public EPerson getSubmitter()
    {
        return submitter;
    }


    /**
     * Set the e-person that originally submitted this item.  This is a
     * public method since it is handled by the WorkspaceItem class in the
     * ingest package.
     * <code>update</code> must be called to write the change to the database.
     *
     * @param  sub  the submitter
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
    }


    /**
     * Get the collections this item is in.  The order is indeterminate.
     *
     * @return the collections this item is in, if any.
     */
    public Collection[] getCollections()
        throws SQLException
    {
        List collections = new ArrayList();

        // Get collection table rows
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "collection",
            "SELECT collection.* FROM collection, collection2item WHERE " +
                "collection2item.collection_id=collection.collection_id AND " +
                "collection2item.item_id=" +
                itemRow.getIntColumn("item_id") + ";");

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

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }


    /**
     * Get the communities this item is in.  Returns an unordered array of
     * the communities that house the collections this item is in.
     *
     * @return  the communities this item is in.
     */
    public Community[] getCommunities()
        throws SQLException
    {
        List communities = new ArrayList();

        // Get community table rows
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "community",
            "SELECT community.* FROM community, community2item " +
                "WHERE community2item.community_id=community.community_id " +
                "AND community2item.item_id=" +
                itemRow.getIntColumn("item_id") + ";");

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) ourContext.fromCache(
                Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                communities.add(fromCache);
            }
            else
            {
                communities.add(new Community(ourContext, row));
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
    public Bundle[] getBundles()
    {
        Bundle[] bundleArray = new Bundle[bundles.size()];
        bundleArray = (Bundle[]) bundles.toArray(bundleArray);

        return bundleArray;
    }


    /**
     * Create a bundle in this item, with immediate effect
     *
     * @return  the newly created bundle
     */
    public Bundle createBundle()
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Bundle b = Bundle.create(ourContext);
        addBundle(b);
        return b;
    }


    /**
     * Add an existing bundle to this item.  This has immediate effect.
     *
     * @param b  the bundle to add
     */
    public void addBundle(Bundle b)
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext,
            "add_bundle",
            "item_id=" + getID() +
                ",bundle_id=" + b.getID()));

        // Check it's not already there
        for (int i = 0; i < bundles.size(); i++)
        {
            Bundle existing = (Bundle) bundles.get(i);
            if (b.getID() == existing.getID())
            {
                // Bundle is already there; no change
                return;
            }
        }

        // Add the bundle to in-memory list
        bundles.add(b);

        // Insert the mapping
        TableRow mappingRow = DatabaseManager.create(ourContext, "item2bundle");
        mappingRow.setColumn("item_id", getID());
        mappingRow.setColumn("bundle_id", b.getID());
        DatabaseManager.update(ourContext, mappingRow);
    }


    /**
     * Remove a bundle.  Only the mapping between the item and the bundle is
     * removed.  The bundle itself is not deleted from the database.
     *
     * @param b  the bundle to remove
     */
    public void removeBundle(Bundle b)
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext,
            "remove_bundle",
            "item_id=" + getID() +
                ",bundle_id=" + b.getID()));

        ListIterator li = bundles.listIterator();

        while (li.hasNext())
        {
            Bundle existing = (Bundle) li.next();

            if (b.getID() == existing.getID())
            {
                // We've found the bundle to remove
                li.remove();
            }
        }

        // Remove mappings from DB
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM item2bundle WHERE item_id=" + getID() +
                " AND bundle_id=" + b.getID());
    }


    /**
     * Create a single bitstream in a new bundle.  Provided as a convenience
     * method for the most common use.
     *
     * @param is   the stream to create the new bitstream from
     */
    public Bitstream createSingleBitstream(InputStream is)
        throws AuthorizeException, IOException, SQLException
    {
        // Authorisation is checked by methods below

        // Create a bundle
        Bundle bnd = createBundle();
        Bitstream bitstream = bnd.createBitstream(is);
        bnd.update();
        addBundle(bnd);

        return bitstream;
    }


    /**
     * Get all non-internal bitstreams in the item.  This is mainly used
     * for auditing for provenance messages and adding format.* DC values.
     * The order is indeterminate.
     *
     * @return  non-internal bitstreams.
     */
    public Bitstream[] getNonInternalBitstreams()
    {
        List bitstreamList = new ArrayList();

        // Go through the bundles and bitstreams picking out ones which aren't
        // of internal formats
        Bundle[] bundles = getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

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
     * Update the item "in archive" flag and Dublin Core metadata in the
     * database
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        log.info(LogManager.getHeader(ourContext,
            "update_item",
            "item_id=" + getID()));

        // Map counting number of values for each element/qualifier.
        // Keys are Strings: "element" or "element.qualifier"
        // Values are Integers indicating number of values written for a
        // element/qualifier
        Map elementCount = new HashMap();

        DatabaseManager.update(ourContext, itemRow);

        // Redo Dublin Core if it's changed
        if (dublinCoreChanged)
        {
            // Remove existing DC
            removeDCFromDatabase();

            // Add in-memory DC
            Iterator i = dublinCore.iterator();

            while (i.hasNext())
            {
                DCValue dcv = (DCValue) i.next();

                // Get the DC Type
                DCType dcType = DCType.findByElement(ourContext,
                    dcv.element,
                    dcv.qualifier);

                if (dcType == null)
                {
                    // Bad DC field, ignore it
                    // FIXME: An error?
                    log.warn(LogManager.getHeader(ourContext,
                        "bad_dc",
                        "Bad DC field.  element: \"" +
                            (dcv.element == null ? "null" : dcv.element) +
                            "\" qualifier: \"" +
                            (dcv.qualifier == null ? "null" : dcv.qualifier) +
                            "\" value: \"" +
                            (dcv.value == null ? "null" : dcv.value) + "\""));
                }
                else
                {
                    // Work out the place number for ordering
                    int current = 0;

                    // Key into map is "element" or "element.qualifier"
                    String key = dcv.element +
                        (dcv.qualifier == null ? "" : "." + dcv.qualifier);

                    Integer currentInteger = (Integer) elementCount.get(key);

                    if (currentInteger != null)
                    {
                        current = currentInteger.intValue();
                    }

                    current++;
                    elementCount.put(key, new Integer(current));

                    // Write DCValue
                    TableRow valueRow = DatabaseManager.create(ourContext,
                        "dcvalue");

                    valueRow.setColumn("item_id", getID());
                    valueRow.setColumn("dc_type_id", dcType.getID());
                    valueRow.setColumn("text_value", dcv.value);
                    valueRow.setColumn("text_lang", dcv.language);
                    valueRow.setColumn("place", current);

                    DatabaseManager.update(ourContext, valueRow);
                }
            }

            dublinCoreChanged = false;
        }

        // Update browse indices
        Browse.itemChanged(ourContext, this);

        if (isArchived())
        {
            // FIXME: Update search index
        }
    }


    /**
     * Delete the item.  Bundles and bitstreams are also deleted if they are
     * not also included in another item.  The Dublin Core metadata is deleted,
     * as are any associations with collections.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.DELETE);

        log.info(LogManager.getHeader(ourContext,
            "update_item",
            "item_id=" + getID()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove from indices, if appropriate
        if (isArchived())
        {
            // Remove from Browse indices
            Browse.itemRemoved(ourContext, getID());

            // FIXME: Remove from search index
        }

        // Delete the Dublin Core
        removeDCFromDatabase();

        // Remove all item -> bundle mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM item2bundle WHERE item_id=" + getID() + ";");

        // Work out which Bundles are "orphans"
        Iterator i = bundles.iterator();

        while (i.hasNext())
        {
            Bundle b = (Bundle) i.next();

            TableRowIterator mappings = DatabaseManager.query(ourContext,
                "item2bundle",
                "SELECT * FROM item2bundle WHERE bundle_id=" + b.getID() + ";");

            if (!mappings.hasNext())
            {
                // No mapping between bundle b and another item; remove it
                b.deleteWithContents();
            }
        }

        // Remove collection -> item mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM collection2item WHERE item_id=" + getID() + ";");

        // Finally remove item row
        DatabaseManager.delete(ourContext, itemRow);
    }


    /**
     * Return <code>true</code> if <code>other</code> is the same Item as
     * this object, <code>false</code> otherwise
     *
     * @param other   object to compare to
     *
     * @return  <code>true</code> if object passed in represents the same
     *          item as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Item))
        {
            return false;
        }

        return (getID() == ((Item) other).getID());
    }


    /**
     * Utility method to remove all Dublin Core associated with the item
     * from the database (regardless of in-memory version)
     */
    private void removeDCFromDatabase()
        throws SQLException
    {
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM dcvalue WHERE item_id=" + getID() + ";");
    }
}
