/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Class for updating browse system from content events.
 * Prototype: only Item events recognized.
 *
 * XXX FIXME NOTE:  The Browse Consumer is INCOMPLETE because the
 * deletion of an Item CANNOT be implemented as an event consumer:
 * When an Item is deleted, the browse tables must be updated
 * immediately, within the same transaction, to maintain referential
 * consistency.  It cannot be handled in an Event consumer since by
 * definition that runs after the transaction is committed.
 * Perhaps this can be addressed if the Browse system is replaced.
 *
 * To handle create/modify events:  accumulate Sets of Items to be added
 * and updated out of the event stream.  Process them in endEvents()
 * filter out update requests for Items that were just created.
 *
 * Recommended filter:  Item+Create|Modify|Modify_Metadata:Collection+Add|Remove
 *
 * @version $Revision$
 */
public class BrowseConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(BrowseConsumer.class);

    // items to be updated in browse index
    private Map<Integer, ItemHolder> toUpdate = null;

    public void initialize()
        throws Exception
    {
       
    }

    public void consume(Context ctx, Event event)
        throws Exception
    {
        if(toUpdate == null)
        {
            toUpdate = new HashMap<Integer, ItemHolder>();
        }
        
        log.debug("consume() evaluating event: " + event.toString());
        
        
        int st = event.getSubjectType();
        int et = event.getEventType();
        
        switch (st)
        {

        // If an Item is created or its metadata is modified..
        case Constants.ITEM:
            if (et == Event.MODIFY_METADATA || et == Event.CREATE || et == Event.MODIFY)
            {
                Item subj = (Item)event.getSubject(ctx);
                if (subj != null)
                {
                    log.debug("consume() adding event to update queue: " + event.toString());
                    if (et == Event.CREATE || !toUpdate.containsKey(subj.getID()))
                    {
                        toUpdate.put(subj.getID(), new ItemHolder(subj, et == Event.CREATE));
                    }
                }
            }
            break;
        // track ADD and REMOVE from collections, that changes browse index.
        case Constants.COLLECTION:
            if (event.getObjectType() == Constants.ITEM && (et == Event.ADD || et == Event.REMOVE))
            {
                Item obj = (Item)event.getObject(ctx);
                if (obj != null)
                {
                    log.debug("consume() adding event to update queue: " + event.toString());
                    if (!toUpdate.containsKey(obj.getID()))
                    {
                        toUpdate.put(obj.getID(), new ItemHolder(obj, false));
                    }
                }
            }
            break;
        default:
            log.debug("consume() ignoring event: " + event.toString());
        }
        
    }

    public void end(Context ctx)
        throws Exception
    {
        
        if (toUpdate != null)
        {

            // Update/Add items
            for (ItemHolder i : toUpdate.values())
            {
                // FIXME: there is an exception handling problem here
                try
                {
                    // Update browse indices
                    ctx.turnOffAuthorisationSystem();
                    IndexBrowse ib = new IndexBrowse(ctx);
                    ib.indexItem(i.item, i.createEvent);
                    ctx.restoreAuthSystemState();
                }
                catch (BrowseException e)
                {
                    log.error("caught exception: ", e);
                    //throw new SQLException(e.getMessage());
                }

                if (log.isDebugEnabled())
                {
                    log.debug("Updated browse indices for Item id="
                            + String.valueOf(i.item.getID()) + ", hdl="
                            + i.item.getHandle());
                }
            }

            // NOTE: Removed items are necessarily handled inline (ugh).

            // browse updates wrote to the DB, so we have to commit.
            ctx.getDBConnection().commit();

        }
        
        // clean out toUpdate
        toUpdate = null;
    }
    
    public void finish(Context ctx) {
    	
    }

    private final class ItemHolder {
        private Item item;
        private boolean createEvent;

        ItemHolder(Item pItem, boolean pCreateEvent)
        {
            item = pItem;
            createEvent = pCreateEvent;
        }
    }
}
