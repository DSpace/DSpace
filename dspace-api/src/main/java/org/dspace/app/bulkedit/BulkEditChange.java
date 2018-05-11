/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.dspace.content.Item;
import org.dspace.content.Collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to store changes to item that may occur during a batch edit.
 *
 * @author Stuart Lewis
 */
public class BulkEditChange
{
    /** The item these changes relate to */
    private Item item;

    /** The List of hashtables with the new elements */
    private List<BulkEditMetadataValue> adds;

    /** The List of hashtables with the removed elements */
    private List<BulkEditMetadataValue> removes;

    /** The List of hashtables with the unchanged elements */
    private List<BulkEditMetadataValue> constant;

    /** The List of the complete set of new values (constant + adds) */
    private List<BulkEditMetadataValue> complete;

    /** The list of old collections the item used to be mapped to */
    private List<Collection> oldMappedCollections;

    /** The list of new collections the item has been mapped into */
    private List<Collection> newMappedCollections;

    /** The old owning collection */
    private Collection oldOwningCollection;

    /** The new owning collection */
    private Collection newOwningCollection;

    /** Is this a new item */
    private boolean newItem;

    /** Has this item been deleted? */
    private boolean deleted;

    /** Has this item been withdrawn? */
    private boolean withdrawn;

    /** Has this item been reinstated? */
    private boolean reinstated;

    /** Have any changes actually been made? */
    private boolean empty;


    /**
     * Initialise a change holder for a new item 
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
        adds = new ArrayList<>();
        removes = new ArrayList<>();
        constant = new ArrayList<>();
        complete = new ArrayList<>();
        oldMappedCollections = new ArrayList<>();
        newMappedCollections = new ArrayList<>();
    }

    /**
     * Initialise a new change holder for an existing item
     *
     * @param i The Item to store
     */
    public BulkEditChange(Item i)
    {
        // Store the item
        item = i;
        newItem = false;
        empty = true;

        // Initialise the arrays
        adds = new ArrayList<>();
        removes = new ArrayList<>();
        constant = new ArrayList<>();
        complete = new ArrayList<>();
        oldMappedCollections = new ArrayList<>();
        newMappedCollections = new ArrayList<>();
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
    public void registerAdd(BulkEditMetadataValue dcv)
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
    public void registerRemove(BulkEditMetadataValue dcv)
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
    public void registerConstant(BulkEditMetadataValue dcv)
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
    public List<BulkEditMetadataValue> getAdds()
    {
        // Return the array
        return adds;
    }

    /**
     * Get the list of elements and their values that have been removed.
     *
     * @return the list of elements and their values that have been removed.
     */
    public List<BulkEditMetadataValue> getRemoves()
    {
        // Return the array
        return removes;
    }

    /**
     * Get the list of unchanged values
     *
     * @return the list of unchanged values
     */
    public List<BulkEditMetadataValue> getConstant()
    {
        // Return the array
        return constant;
    }

    /**
     * Get the list of all values
     *
     * @return the list of all values
     */
    public List<BulkEditMetadataValue> getComplete()
    {
        // Return the array
        return complete;
    }

    /**
     * Get the list of new mapped Collections
     *
     * @return the list of new mapped collections
     */
    public List<Collection> getNewMappedCollections()
    {
        // Return the array
        return newMappedCollections;
    }

    /**
     * Get the list of old mapped Collections
     *
     * @return the list of old mapped collections
     */
    public List<Collection> getOldMappedCollections()
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
     * Does this change object represent a deleted item?
     *
     * @return Whether or not this is for a deleted item
     */
    public boolean isDeleted()
    {
        // Return the new item status
        return deleted;
    }

    /**
     * Set that this item has been deleted
     */
    public void setDeleted() {
        // Store the setting
        deleted = true;
        empty = false;
    }

    /**
     * Does this change object represent a withdrawn item?
     *
     * @return Whether or not this is for a withdrawn item
     */
    public boolean isWithdrawn()
    {
        // Return the new item status
        return withdrawn;
    }

    /**
     * Set that this item has been withdrawn
     */
    public void setWithdrawn() {
        // Store the setting
        withdrawn = true;
        empty = false;
    }

    /**
     * Does this change object represent a reinstated item?
     *
     * @return Whether or not this is for a reinstated item
     */
    public boolean isReinstated()
    {
        // Return the new item status
        return reinstated;
    }

    /**
     * Set that this item has been deleted
     */
    public void setReinstated() {
        // Store the setting
        reinstated = true;
        empty = false;
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
