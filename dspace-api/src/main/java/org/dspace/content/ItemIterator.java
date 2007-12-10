/*
 * ItemIterator.java
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

import java.sql.SQLException;

import java.util.Iterator;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;

/**
 * Specialized iterator for DSpace Items. This iterator is used for loading
 * items into memory one by one, since in many cases, it would not make sense to
 * load a set of items into memory all at once. For example, loading in an
 * entire community or site's worth of items wouldn't make sense.
 *
 * Note that this class is not a real Iterator, as it does not implement
 * the Iterator interface
 *
 * FIXME: I want to bin this class. It's more or less utterly pointless now we
 * have ItemProxy. -- James Rutherford
 * 
 * @author Robert Tansley
 * @author Richard Jones
 * @version $Revision$
 */
public class ItemIterator
{
    /*
     * This class basically wraps a TableRowIterator.
     */

    /** Our context */
    private Context ourContext;

    /** The table row iterator of Item rows */
    private TableRowIterator itemRows;

    /** a real iterator which works over the item ids when present */
    private Iterator iditr;

    private ItemDAO dao;
    
    /**
     * Construct an item iterator using a set of TableRow objects from
     * the item table
     * 
     * @param context
     *            our context
     * @param rows
     *            the rows that correspond to the Items to be iterated over
     */
    public ItemIterator(Context context, TableRowIterator rows)
    {
        ourContext = context;
        itemRows = rows;
        dao = ItemDAOFactory.getInstance(context);
    }

    /**
     * Construct an item iterator using an array list of item ids
     * 
     * @param context
     *            our context
     * @param iids
     *            the array list to be iterated over
     */
    public ItemIterator(Context context, List iids)
    {
    	ourContext = context;
    	iditr = iids.iterator();
        dao = ItemDAOFactory.getInstance(context);
    }
    
    /**
     * Find out if there are any more items to iterate over
     * 
     * @return <code>true</code> if there are more items
     * @throws SQLException
     */
    public boolean hasNext() throws SQLException
    {
    	if (iditr != null)
    	{
    		return iditr.hasNext();
    	}
    	else if (itemRows != null)
    	{
    		return itemRows.hasNext();
    	}
    	return false;
    }

    /**
     * Get the next item in the iterator. Returns <code>null</code> if there
     * are no more items.
     * 
     * @return the next item, or <code>null</code>
     * @throws SQLException
     */
    public Item next() throws SQLException
    {
    	if (iditr != null)
    	{
    		return nextByID();
    	}
    	else if (itemRows != null)
    	{
    		return nextByRow();
    	}
    	return null;
    }
    
    /**
     * This private method knows how to get the next result out of the 
     * item id iterator
     * 
     * @return	the next item instantiated from the id
     * @throws SQLException
     */
    private Item nextByID()
    	throws SQLException
    {
    	if (iditr.hasNext())
        {
    		// get the id
    		int id = ((Integer) iditr.next()).intValue();
    		
            // Check cache
            Item fromCache = (Item) ourContext.fromCache(Item.class, id);

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return dao.retrieve(id);
            }
        }
        else
        {
            return null;
        }
    }
    
    /**
     * return the id of the next item.
     * 
     * @return	the next id or -1 if none
     */
    public int nextID()
    	throws SQLException
    {
    	if (iditr != null)
    	{
    		return nextByIDID();
    	}
    	else if (itemRows != null)
    	{
    		return nextByRowID();
    	}
    	return -1;
    }
    
    /**
     * Sorry about the name of this one!  It returns the ID of the item
     * as opposed to the item itself when we are iterating over an ArrayList
     * of item ids
     * 
     * @return	the item id, or -1 if none
     */
    private int nextByIDID()
    {
    	if (iditr.hasNext())
        {
    		// get the id
    		int id = ((Integer) iditr.next()).intValue();
    		
            return id;
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Returns the ID of the item as opposed to the item itself when we are
     * iterating over the TableRow array.
     * 
     * @return	the item id, or -1 if none
     */
    private int nextByRowID()
    	throws SQLException
    {
    	if (itemRows.hasNext())
        {
            TableRow row = itemRows.next();
            return row.getIntColumn("item_id");
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Return the next item instantiated from the supplied TableRow
     * 
     * @return	the item or null if none
     * @throws SQLException
     */
    private Item nextByRow()
    	throws SQLException
    {
        ItemDAO dao = ItemDAOFactory.getInstance(ourContext);
    	if (itemRows.hasNext())
        {
            return dao.retrieve(itemRows.next().getIntColumn("item_id"));
        }
        else
        {
            return null;
        }
    }
}
