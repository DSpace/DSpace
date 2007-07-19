/*
 * Bundle.java
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
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
    private static Logger log = Logger.getLogger(Bundle.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this bundle */
    private TableRow bundleRow;

    /** The bitstreams in this bundle */
    private List<Bitstream> bitstreams;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

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
        ourContext = context;
        bundleRow = row;
        bitstreams = new ArrayList<Bitstream>();

        // Get bitstreams
        TableRowIterator tri = DatabaseManager.queryTable(
                ourContext, "bitstream",
                "SELECT bitstream.* FROM bitstream, bundle2bitstream WHERE "
                        + "bundle2bitstream.bitstream_id=bitstream.bitstream_id AND "
                        + "bundle2bitstream.bundle_id= ? ",
                bundleRow.getIntColumn("bundle_id"));
        
        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();

            // First check the cache
            Bitstream fromCache = (Bitstream) context.fromCache(
                    Bitstream.class, r.getIntColumn("bitstream_id"));

            if (fromCache != null)
            {
                bitstreams.add(fromCache);
            }
            else
            {
                bitstreams.add(new Bitstream(ourContext, r));
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Cache ourselves
        context.cache(this, row.getIntColumn("bundle_id"));

        modified = modifiedMetadata = false;
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
        return bundleRow.getStringColumn("name");
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
        bundleRow.setColumn("name", name);
        modifiedMetadata = true;
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
        // close the TableRowIterator to free up resources
        tri.close();

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

        ourContext.addEvent(new Event(Event.ADD, Constants.BUNDLE, getID(), Constants.BITSTREAM, b.getID(), String.valueOf(b.getSequenceID())));

        // copy authorization policies from bundle to bitstream
        // FIXME: multiple inclusion is affected by this...
        AuthorizeManager.inheritPolicies(ourContext, this, b);

        // Add the mapping row to the database
        TableRow mappingRow = DatabaseManager.create(ourContext,
                "bundle2bitstream");
        mappingRow.setColumn("bundle_id", getID());
        mappingRow.setColumn("bitstream_id", b.getID());
        DatabaseManager.update(ourContext, mappingRow);
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
                
                // In the event that the bitstream to remove is actually
                // the primary bitstream, be sure to unset the primary
                // bitstream.
                if (b.getID() == getPrimaryBitstreamID()) {
                	unsetPrimaryBitstreamID();
                }
            }
        }

        ourContext.addEvent(new Event(Event.REMOVE, Constants.BUNDLE, getID(), Constants.BITSTREAM, b.getID(), String.valueOf(b.getSequenceID())));

        // Delete the mapping row
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM bundle2bitstream WHERE bundle_id= ? "+
                "AND bitstream_id= ? ", 
                getID(), b.getID());

        // If the bitstream is orphaned, it's removed
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM bundle2bitstream WHERE bitstream_id= ? ",
                b.getID());

        if (!tri.hasNext())
        {
            // The bitstream is an orphan, delete it
            b.delete();
        }
        // close the TableRowIterator to free up resources
        tri.close();
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

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.BUNDLE, getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.BUNDLE, getID(), null));
            modifiedMetadata = false;
        }

        DatabaseManager.update(ourContext, bundleRow);
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

        ourContext.addEvent(new Event(Event.DELETE, Constants.BUNDLE, getID(), getName()));

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
    }

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.BUNDLE;
    }
}
