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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Category;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing an item in DSpace
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
    private static Category log = Category.getInstance(Item.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow itemRow;
    
    /** The e-person who submitted this item */
    private EPerson submitter;

    /** The bundles in this item */
    private List bundles;

    /**
     * True if the bundles have changed since reading from the DB
     * or the last update()
     */
    private boolean bundlesChanged;

    /**
     * The Dublin Core metadata - a list of DCValue objects.
     */
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
        bundlesChanged = false;
        bundles = new ArrayList();

        // FIXME
        submitter = null;
        
        // Get bitstreams
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "bundle",
            "select bundle.* from bundle, item2bundle where " +
                "item2bundle.bundle_id=bundle.bundle_id AND " +
                "item2bundle.item_id=" +
                itemRow.getIntColumn("item_id") + ";");

        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();
            bundles.add(new Bundle(ourContext, r));
        }

        // Get Dublin Core metadata
        tri = DatabaseManager.query(ourContext, "dcresult",
            "select * from dcresult where item_id=" +
                itemRow.getIntColumn("item_id"));

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
    }


    /**
     * Get an item from the database.  The item, its Dublin Core metadata,
     * and the bundle and bitstream metadata are all loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       Internal ID of the item
     *   
     * @return  the item, or null if the Handle is invalid.
     */
    public static Item find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find(context,
            "item",
            id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Item(context, row);
        }
    }


    /**
     * Get an item from the database.  The item, its Dublin Core metadata,
     * and the bundle and bitstream metadata are all loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  handle   Handle of the item
     *   
     * @return  the item, or null if the Handle is invalid.
     */
    public static Item find(Context context, String handle)
        throws SQLException
    {
        // FIXME: Need handle manager!
        return null;
    }

    
    /**
     * Create a new item, with a new internal ID.  Not inserted in database.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created item
     */
    static Item create(Context context)
        throws AuthorizeException, SQLException
    {
        // FIXME Check authorisation
        
        TableRow row = DatabaseManager.create(context, "item");
        return new Item(context, row);
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
        return itemRow.getBooleanColumn("is_archived");
    }


    /**
     * Set the "is_archived" flag.  This is not public since only
     * <code>WorkflowItem.archive()</code> should set this.
     *
     * @param isArchived  new value for the flag
     */
    void setArchived(boolean isArchived)
    {
        itemRow.setColumn("is_archived", isArchived);
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
     *
     * @param  element    the Dublin Core element.  Must be specified.
     * @param  qualifier  the qualifier.  <code>null</code> means unqualified,
     *                    and <code>Item.ANY</code> means any qualifier
     *                    (including unqualified.)
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means only values with no language
     *                    are returned, and <code>Item.ANY</code> means values
     *                    with any country code or no country code are returned.
     *
     * @return  values of the Dublin Core fields that match the parameters.
     */
    public String[] getDC(String element, String qualifier, String lang)
    {
        // Build up list of matching values
        List values = new ArrayList();
        Iterator i = dublinCore.iterator();
        
        while (i.hasNext())
        {
            DCValue dcv = (DCValue) i.next();
            
            if (match(element, qualifier, lang, dcv))
            {
                values.add(dcv.value);
            }
        }

        // Create an array of matching values
        String[] valueArray = new String[values.size()];
        valueArray = (String[]) values.toArray(valueArray);

        return valueArray;
    }
    
    
    /**
     * Add Dublin Core metadata fields.  These are appended to existing values.
     * Use <code>clearDC</code> to remove values.
     *
     * @param  element    the Dublin Core element
     * @param  qualifier  the Dublin Core qualifer, or <code>null</code> for
     *                    unqualified
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null means the value has no language (e.g.
     *                    a date).
     * @param  values     the values to add.
     */
    public void addDC(String element,
                      String qualifier,
                      String lang,
                      String[] values)
        throws AuthorizeException
    {
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = lang;
            dublinCore.add(dcv);
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
     *                    <code>null means the value has no language (e.g.
     *                    a date).
     * @param  value      the value to add.
     */
    public void addDC(String element,
                      String qualifier,
                      String lang,
                      String value)
        throws AuthorizeException
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
        throws AuthorizeException
    {
        // We will build a list of values NOT matching the values to clear
        List values = new ArrayList();
        Iterator i = dublinCore.iterator();
        
        while (i.hasNext())
        {
            DCValue dcv = (DCValue) i.next();
            
            if (!match(element, qualifier, lang, dcv))
            {
                values.add(dcv.value);
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
     * Get the handle of this item.  Returns <code>null</code> if no handle has
     * been assigned.
     *
     * @return  the handle
     */
    public String getHandle()
    {
        // FIXME
        return null;
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
     * Set the e-person that originally submitted this item.  This is not a
     * public method since it is handled by the WorkspaceItem class.
     *
     * @param  sub  the submitter
     */
    void setSubmitter(EPerson sub)
    {
        submitter = sub;
    }


    /**
     * Get the collections this item is in.
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
            "select collection.* from collection, collection2item where " +
                "collection2item.collection_id=collection.collection_id AND " +
                "collection2item.item_id=" +
                itemRow.getIntColumn("item_id") + ";");

        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();
            collections.add(new Item(ourContext, r));
        }
        
        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);
        
        return collectionArray;
    }
    

    /**
     * Get the bundles in this item
     *
     * @return the bundles
     */
    public Bundle[] getBundles()
    {
        Bundle[] bundleArray = new Bundle[bundles.size()];
        bundleArray = (Bundle[]) bundles.toArray(bundleArray);
        
        return bundleArray;
    }
    

    /**
     * Add a bundle
     *
     * @param b  the bundle to add
     */
    public void addBundle(Bundle b)
        throws AuthorizeException
    {
        // FIXME: Check auth

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
        
        // Add the bundle
        bundles.add(b);
        bundlesChanged = true;
    }

    
    /**
     * Remove a bundle.  Only the mapping between the item and the bundle is
     * removed.  The bundle itself is not deleted from the database.
     *
     * @param b  the bundle to remove
     */
    public void removeBundle(Bundle b)
        throws AuthorizeException
    {
        // FIXME Check authorisation

        ListIterator li = bundles.listIterator();

        while (li.hasNext())
        {
            Bundle existing = (Bundle) li.next();

            if (b.getID() == existing.getID())
            {
                // We've found the bundle to remove
                li.remove();               
                bundlesChanged = true;
            }
        }
    }
    

    /**
     * Add a single bitstream in a new bundle.  A bundle is created, and the
     * bitstream passed in made the single bitstream.  The bundle and
     * bundle-bitstream mapping are added to the database immediately, but
     * the item-bundle mapping won't be written until <code>update</code> is
     * called.
     *
     * @param bitstream  the bitstream to add
     */
    public void addSingleBitstream(Bitstream bitstream)
        throws AuthorizeException, SQLException
    {
        // Create a bundle
        Bundle bnd = Bundle.create(ourContext);
        bnd.addBitstream(bitstream);
        bnd.update();
        addBundle(bnd);
    }


    /**
     * Get all non-internal bitstreams in the item.  This is mainly used
     * for auditing for provenance messages and adding format.* DC values,
     * and is hence not public.
     *
     * @return  non-internal bitstreams.
     */
    Bitstream[] getNonInternalBitstreams()
    {
        List bitstreamList = new ArrayList();

        // Go through the bundles and bitstreams picking out ones which aren't
        // of internal formats
        Bundle[] bundles = getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int j = 0; j < bundles.length; j++)
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
     * Update the item, including Dublin Core metadata, and the bundle
     * and bitstream metadata, in the database.  Inserts if this is a new
     * item.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        
        DatabaseManager.update(ourContext, itemRow);
        
        // Redo bundle mappings if they've changed
        if (bundlesChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(ourContext,
                "delete from item2bundle where item_id=" + getID());

            // Add new mappings
            Iterator i = bundles.iterator();

            while (i.hasNext())
            {
                Bundle b = (Bundle) i.next();

                TableRow mappingRow = DatabaseManager.create(ourContext,
                    "item2bundle");
                mappingRow.setColumn("bundle_id", b.getID());
                mappingRow.setColumn("item_id", getID());
                DatabaseManager.update(ourContext, mappingRow);
            }

            bundlesChanged = false;
        }

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
                // FIXME: Maybe should use RegistryManager?
                String query = "select * from dctyperegistry where element " +
                    "LIKE \"" + dcv.element + "\" AND qualifier" +
                    (dcv.qualifier == null
                        ? "=null"
                        : " LIKE \"" + dcv.qualifier + "\"") +
                    ";";

                TableRow dcTypeRow = DatabaseManager.querySingle(ourContext,
                    "dctyperegistry",
                    query);

                if (dcTypeRow == null)
                {
                    // Bad DC field
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
                    // Write DCValue
                    TableRow valueRow = DatabaseManager.create(ourContext,
                        "dcvalue");

                    valueRow.setColumn("text_value", dcv.value);
                    valueRow.setColumn("text_lang", dcv.language);

                    DatabaseManager.update(ourContext, valueRow);
                    
                    // Write mapping
                    TableRow mappingRow = DatabaseManager.create(ourContext,
                        "item2dcvalue");

                    mappingRow.setColumn("item_id", getID());
                    mappingRow.setColumn("dc_value_id",
                        valueRow.getIntColumn("dc_value_id"));
                    mappingRow.setColumn("dc_type_id",
                        dcTypeRow.getIntColumn("dc_type_id"));

                    DatabaseManager.update(ourContext, mappingRow);
                }
            }
            
            dublinCoreChanged = false;
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
    }


    /**
     * Class representing a Dublin Core value
     */
    private class DCValue
    {
        /** The DC element */
        String element;
        /** The DC qualifier */
        String qualifier;
        /** The value of the field */
        String value;
        /** The language of the field */
        String language;
    }

    
    /**
     * Utility method to remove all Dublin Core associated with the item
     * from the database (regardless of in-memory version)
     */
    private void removeDCFromDatabase()
        throws SQLException
    {
        // We need to delete the mapping rows first, but get the dcvalue
        // rows before we do since we'll need to delete them afterwards
        TableRowIterator dcValueRows = DatabaseManager.query(ourContext,
            "dcvalue",
            "SELECT dcvalue.* FROM dcvalue, item2dcvalue WHERE " +
                "dcvalue.dc_value_id=item2dcvalue.dc_value_id AND " +
                "item2dcvalue.item_id=" + getID() + ";");

        // Now delete the mappings
        DatabaseManager.updateQuery(ourContext,
            "delete from item2dcvalue where item_id=" + getID() + ";");

        // And the values
        while (dcValueRows.hasNext())
        {
            TableRow r = dcValueRows.next();
            DatabaseManager.delete(ourContext, r);
        }
    }    
}
