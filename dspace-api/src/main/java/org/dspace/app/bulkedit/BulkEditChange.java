/*
 * BulkEditChange.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

package org.dspace.app.bulkedit;

import org.dspace.content.Item;
import org.dspace.content.DCValue;
import org.dspace.content.Collection;

import java.util.ArrayList;

/**
 * Utility class to store changes to item that may occur during a batch edit.
 *
 * @author Stuart Lewis
 */
public class BulkEditChange
{
    /** The item these changes relate to */
    private Item item;

    /** The ArrayList of hashtables with the new elements */
    private ArrayList<DCValue> adds;

    /** The ArrayList of hashtables with the removed elements */
    private ArrayList<DCValue> removes;

    /** The ArrayList of hashtablles with the unchanged elements */
    private ArrayList<DCValue> constant;

    /** The ArrayList of the complete set of new values (constant + adds) */
    private ArrayList<DCValue> complete;

    /** The Arraylist of old collections the item used to be mapped to */
    private ArrayList<Collection> oldMappedCollections;

    /** The Arraylist of new collections the item has been mapped into */
    private ArrayList<Collection> newMappedCollections;

    /** The old owning collection */
    private Collection oldOwningCollection;

    /** The new owning collection */
    private Collection newOwningCollection;

    /** Is this a new item */
    private boolean newItem;

    /** Have any changes actually been made? */
    private boolean empty;


    /**
     * Initalise a change holder for a new item 
     */
    public BulkEditChange()
    {
        // Set the item to be null
        item = null;
        newItem = true;
        empty = true;
        oldOwningCollection = null;
        newOwningCollection = null;

        // Initialise the arrays
        adds = new ArrayList<DCValue>();
        removes = new ArrayList<DCValue>();
        constant = new ArrayList<DCValue>();
        complete = new ArrayList<DCValue>();
        oldMappedCollections = new ArrayList<Collection>();
        newMappedCollections = new ArrayList<Collection>();
    }

    /**
     * Initalise a new change holder for an existing item
     *
     * @param i The Item to store
     */
    public BulkEditChange(Item i)
    {
        // Store the item
        item = i;
        newItem = false;
        empty = true;

        // Initalise the arrays
        adds = new ArrayList<DCValue>();
        removes = new ArrayList<DCValue>();
        constant = new ArrayList<DCValue>();
        complete = new ArrayList<DCValue>();
        oldMappedCollections = new ArrayList<Collection>();
        newMappedCollections = new ArrayList<Collection>();
    }

    /**
     * Store the item - used when a new item is created
     *
     * @param i The item
     */
    public void setItem(Item i)
    {
        // Store the item
        item = i;
    }

    /**
     * Add an added metadata value
     *
     * @param dcv The value to add
     */
    public void registerAdd(DCValue dcv)
    {
        // Add the added value
        adds.add(dcv);
        complete.add(dcv);
        empty = false;
    }

    /**
     * Add a removed metadata value
     *
     * @param dcv The value to remove
     */
    public void registerRemove(DCValue dcv)
    {
        // Add the removed value
        removes.add(dcv);
        empty = false;
    }

    /**
     * Add an unchanged metadata value
     *
     * @param dcv The value to keep unchanged
     */
    public void registerConstant(DCValue dcv)
    {
        // Add the removed value
        constant.add(dcv);
        complete.add(dcv);
    }

    /**
     * Add a new mapped Collection
     *
     * @param c The new mapped Collection
     */
    public void registerNewMappedCollection(Collection c)
    {
        // Add the new owning Collection
        newMappedCollections.add(c);
        empty = false;
    }

    /**
     * Add an old mapped Collection
     *
     * @param c The old mapped Collection
     */
    public void registerOldMappedCollection(Collection c)
    {
        // Add the old owning Collection (if it isn't there already, or is an old collection)
        boolean found = false;

        if ((this.getOldOwningCollection() != null) &&
            (this.getOldOwningCollection().getHandle().equals(c.getHandle())))
        {
            found = true;
        }

        for (Collection collection : oldMappedCollections)
        {
            if (collection.getHandle().equals(c.getHandle()))
            {
                found = true;
            }
        }

        if (!found)
        {
            oldMappedCollections.add(c);
            empty = false;
        }
    }

    /**
     * Register a change to the owning collection
     *
     * @param oldC The old owning collection
     * @param newC The new owning collection
     */
    public void changeOwningCollection(Collection oldC, Collection newC)
    {
        // Store the old owning collection
        oldOwningCollection = oldC;

        // Store the new owning collection
        newOwningCollection = newC;
        empty = false;
    }

    /**
     * Set the owning collection of an item
     *
     * @param newC The new owning collection
     */
    public void setOwningCollection(Collection newC)
    {
        // Store the new owning collection
        newOwningCollection = newC;
        //empty = false;
    }

    /**
     * Get the DSpace Item that these changes are applicable to.
     *
     * @return The item
     */
    public Item getItem()
    {
        // Return the item
        return item;
    }

    /**
     * Get the list of elements and their values that have been added.
     *
     * @return the list of elements and their values that have been added.
     */
    public ArrayList<DCValue> getAdds()
    {
        // Return the array
        return adds;
    }

    /**
     * Get the list of elements and their values that have been removed.
     *
     * @return the list of elements and their values that have been removed.
     */
    public ArrayList<DCValue> getRemoves()
    {
        // Return the array
        return removes;
    }

    /**
     * Get the list of unchanged values
     *
     * @return the list of unchanged values
     */
    public ArrayList<DCValue> getConstant()
    {
        // Return the array
        return constant;
    }

    /**
     * Get the list of all values
     *
     * @return the list of all values
     */
    public ArrayList<DCValue> getComplete()
    {
        // Return the array
        return complete;
    }

    /**
     * Get the list of new mapped Collections
     *
     * @return the list of new mapped collections
     */
    public ArrayList<Collection> getNewMappedCollections()
    {
        // Return the array
        return newMappedCollections;
    }

    /**
     * Get the list of old mapped Collections
     *
     * @return the list of old mapped collections
     */
    public ArrayList<Collection> getOldMappedCollections()
    {
        // Return the array
        return oldMappedCollections;
    }

    /**
     * Get the old owning collection
     *
     * @return the old owning collection
     */
    public Collection getOldOwningCollection()
    {
        // Return the old owning collection
        return oldOwningCollection;
    }

    /**
     * Get the new owning collection
     *
     * @return the new owning collection
     */
    public Collection getNewOwningCollection()
    {
        // Return the new owning collection
        return newOwningCollection;
    }

    /**
     * Does this change object represent a new item?
     *
     * @return Whether or not this is for a new item
     */
    public boolean isNewItem()
    {
        // Return the new item status
        return newItem;
    }

    /**
     * Have any changes actually been recorded, or is this empty?
     *
     * @return Whether or not changes have been made
     */
    public boolean hasChanges()
    {
        return !empty;
    }
}