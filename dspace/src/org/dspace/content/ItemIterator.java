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

import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Specialized iterator for DSpace Items. This iterator is used for loading
 * items into memory one by one, since in many cases, it would not make sense to
 * load a set of items into memory all at once. For example, loading in an
 * entire community or site's worth of items wouldn't make sense.
 * 
 * @author Robert Tansley
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

    /**
     * Construct an item iterator. This is not a public method, since this
     * iterator is only created by CM API methods.
     * 
     * @param context
     *            our context
     * @param rows
     *            the rows that correspond to the Items to be iterated over
     */
    ItemIterator(Context context, TableRowIterator rows)
    {
        ourContext = context;
        itemRows = rows;
    }

    /**
     * Find out if there are any more items to iterate over
     * 
     * @return <code>true</code> if there are more items
     */
    public boolean hasNext() throws SQLException
    {
        return itemRows.hasNext();
    }

    /**
     * Get the next item in the iterator. Returns <code>null</code> if there
     * are no more items.
     * 
     * @return the next item, or <code>null</code>
     */
    public Item next() throws SQLException
    {
        if (itemRows.hasNext())
        {
            // Convert the row into an Item object
            TableRow row = itemRows.next();

            // Check cache
            Item fromCache = (Item) ourContext.fromCache(Item.class, row
                    .getIntColumn("item_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Item(ourContext, row);
            }
        }
        else
        {
            return null;
        }
    }
}
