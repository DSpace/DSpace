/*
 * Bundle.java
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

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing bundles of bitstreams stored in the DSpace system
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Bundle
{
    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this bundle */
    private TableRow bundleRow;
    
    /** The bitstreams in this bundle */
    private List bitstreams;

    /**
     * True if the bitstreams have changed since reading from the DB
     * or the last update()
     */
    private boolean bitstreamsChanged;


    /**
     * Construct a bundle object with the given table row
     *
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    Bundle(Context context, TableRow row)
        throws SQLException
    {
        ourContext = context;
        bundleRow = row;
        bitstreamsChanged = false;
        bitstreams = new ArrayList();
        
        // Get bitstreams
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "bitstream",
            "SELECT bitstream.* FROM bitstream, bundle2bitstream WHERE " +
                "bundle2bitstream.bitstream_id=bitstream.bitstream_id AND " +
                "bundle2bitstream.bundle_id=" +
                bundleRow.getIntColumn("bundle_id") + ";");

        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();
            bitstreams.add(new Bitstream(ourContext, r));
        }
    }

    
    /**
     * Get a bundle from the database.  The bundle and bitstream metadata are
     * all loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the bundle
     *   
     * @return  the bundle, or null if the ID is invalid.
     */
    static Bundle find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find(context,
            "bundle",
            id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Bundle(context, row);
        }
    }
    

    /**
     * Create a new bundle, with a new ID.  Not inserted in database.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created bundle
     */
    public static Bundle create(Context context)
        throws AuthorizeException, SQLException
    {
        // FIXME: Check authorisation 
        
        // Create a table row
        TableRow row = DatabaseManager.create(context, "bundle");
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
     * @return array of <code>Item</code>s this bundle appears
     *         in
     */
    public Item[] getItems()
        throws SQLException
    {
        List items = new ArrayList();

        // Get items
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "item",
            "select item.* from item, item2bundle where " +
                "item2bundle.item_id=item.item_id AND " +
                "item2bundle.bundle_id=" +
                bundleRow.getIntColumn("bundle_id") + ";");

        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();
            items.add(new Item(ourContext, r));
        }
        
        Item[] itemArray = new Item[items.size()];
        itemArray = (Item[]) items.toArray(itemArray);
        
        return itemArray;
    }
    

    /**
     * Add a bitstream
     *
     * @param b  the bitstream to add
     */
    public void addBitstream(Bitstream b)
        throws AuthorizeException
    {
        // FIXME Check authorisation

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
        
        // Add the bitstream
        bitstreams.add(b);
        bitstreamsChanged = true;
    }
    

    /**
     * Remove a bitstream from this bundle - the bitstream is not deleted
     *
     * @param b  the bitstream to remove
     */
    public void removeBitstream(Bitstream b)
        throws AuthorizeException
    {
        // FIXME Check authorisation

        ListIterator li = bitstreams.listIterator();

        while (li.hasNext())
        {
            Bitstream existing = (Bitstream) li.next();

            if (b.getID() == existing.getID())
            {
                // We've found the bitstream to remove
                li.remove();               
                bitstreamsChanged = true;
            }
        }
    }


    /**
     * Update the bundle item, including any changes to bitstreams.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation

        DatabaseManager.update(ourContext, bundleRow);
        
        // Redo bitstream mappings if they've changed
        if (bitstreamsChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(ourContext,
                "delete from bundle2bitstream where bundle_id=" + getID());

            // Add new mappings
            Iterator i = bitstreams.iterator();

            while (i.hasNext())
            {
                Bitstream b = (Bitstream) i.next();

                TableRow mappingRow = DatabaseManager.create(ourContext,
                    "bundle2bitstream");
                mappingRow.setColumn("bundle_id", getID());
                mappingRow.setColumn("bitstream_id", b.getID());
                DatabaseManager.update(ourContext, mappingRow);
            }

            bitstreamsChanged = false;
        }
    }


    /**
     * Delete the bundle.  Any association between the bundle and bitstreams
     * or items are removed.  The bitstreams contained in the bundle are
     * NOT removed.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation

        // Remove item-bundle mappings
        DatabaseManager.updateQuery(ourContext,
            "delete from item2bundle where bundle_id=" + getID());

        // Remove bundle-bitstream mappings
        DatabaseManager.updateQuery(ourContext,
            "delete from bundle2bitstream where bitstream_id=" + getID());

        // Remove ourself
        DatabaseManager.delete(ourContext, bundleRow);
    }


    /**
     * Delete the bundle, and any bitstreams it contains.  Any associations
     * with items are deleted.  However, bitstreams that are also contained
     * in other bundles are NOT deleted.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME: Check authorisation

        // First delete ourselves
        delete();
        
        // Now see if any of our bitstreams were in other bundles
        Iterator i = bitstreams.iterator();

        while (i.hasNext())
        {
            Bitstream b = (Bitstream) i.next();
            
            // Try and find any mapping rows pertaining to the bitstream
            TableRowIterator tri = DatabaseManager.query(ourContext,
                "bundle2bitstream",
                "select * from bundle2bitstream where bitstream_id=" +
                    b.getID());
            
            if (tri.toList().size() == 0)
            {
                // The bitstream is not in any other bundle, so delete it
                b.delete();
            }
        }
    }
}
