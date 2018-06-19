/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Simple container class containing information about a harvested DSpace item.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class HarvestedItemInfo
{
    /** Context used when creating this object */
    public Context context;
    
    /** Internal item ID (as opposed to item's OAI ID, which is the Handle) */
    public UUID itemID;

    /** The Handle, with no prefix */
    public String handle;

    /** The datestamp */
    public Date datestamp;

    /** The item. Only filled out if requested */
    public Item item;

    /**
     * A List of Strings. The Handles of collections this item is in. Only
     * filled out if originally requested when invoking <code>Harvest</code>
     * (N.B. not Collection objects)
     */
    public List<String> collectionHandles;

    /** True if this item has been withdrawn */
    public boolean withdrawn;
}
