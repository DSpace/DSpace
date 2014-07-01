/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Class for updating search indices from content events.
 * 
 * @version $Revision$
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class SearchConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(SearchConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<DSpaceObject> objectsToUpdate = null;

    // handles to delete since IDs are not useful by now.
    private Set<String> handlesToDelete = null;

    public void initialize() throws Exception
    {
        // No-op

    }

    /**
     * Consume a content event -- just build the sets of objects to add (new) to
     * the index, update, and delete.
     * 
     * @param ctx
     *            DSpace context
     * @param event
     *            Content event
     */
    public void consume(Context ctx, Event event) throws Exception
    {

        if (objectsToUpdate == null)
        {
            objectsToUpdate = new HashSet<DSpaceObject>();
            handlesToDelete = new HashSet<String>();
        }

        int st = event.getSubjectType();
        if (!(st == Constants.ITEM || st == Constants.BUNDLE
                || st == Constants.COLLECTION || st == Constants.COMMUNITY))
        {
            log
                    .warn("SearchConsumer should not have been given this kind of Subject in an event, skipping: "
                            + event.toString());
            return;
        }
        
        DSpaceObject subject = event.getSubject(ctx);
 
        DSpaceObject object = event.getObject(ctx);
        
        
        // If event subject is a Bundle and event was Add or Remove,
        // transform the event to be a Modify on the owning Item.
        // It could be a new bitstream in the TEXT bundle which
        // would change the index.
        int et = event.getEventType();
        if (st == Constants.BUNDLE)
        {
            if ((et == Event.ADD || et == Event.REMOVE) && subject != null
                    && "TEXT".equals( ((Bundle) subject).getName()) )
            {
                st = Constants.ITEM;
                et = Event.MODIFY;
                subject = ((Bundle) subject).getItems()[0];
                if (log.isDebugEnabled())
                {
                    log.debug("Transforming Bundle event into MODIFY of Item "
                            + subject.getHandle());
                }
            }
            else
            {
                return;
            }
        }

        switch (et)
        {
        case Event.CREATE:
        case Event.MODIFY:
        case Event.MODIFY_METADATA:
            if (subject == null)
            {
                log.warn(event.getEventTypeAsString() + " event, could not get object for "
                        + event.getSubjectTypeAsString() + " id="
                        + String.valueOf(event.getSubjectID())
                        + ", perhaps it has been deleted.");
            }
            else
            {
                log.debug("consume() adding event to update queue: " + event.toString());
                objectsToUpdate.add(subject);
            }
            break;
            
        case Event.REMOVE:
        case Event.ADD:
            if (object == null)
            {
                log.warn(event.getEventTypeAsString() + " event, could not get object for "
                        + event.getObjectTypeAsString() + " id="
                        + String.valueOf(event.getObjectID())
                        + ", perhaps it has been deleted.");
            }
            else
            {
                log.debug("consume() adding event to update queue: " + event.toString());
                objectsToUpdate.add(object);
            }
            break;
            
        case Event.DELETE:
            String detail = event.getDetail();
            if (detail == null)
            {
                log.warn("got null detail on DELETE event, skipping it.");
            }
            else
            {
                log.debug("consume() adding event to delete queue: " + event.toString());
                handlesToDelete.add(detail);
            }
            break;
        default:
            log
                    .warn("SearchConsumer should not have been given a event of type="
                            + event.getEventTypeAsString()
                            + " on subject="
                            + event.getSubjectTypeAsString());
            break;
        }
    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not
     * need to be added or updated, new objects don't also need an update, etc.
     */
    public void end(Context ctx) throws Exception
    {
        
        if(objectsToUpdate != null && handlesToDelete != null)
        {
         
            // update the changed Items not deleted because they were on create list
            for (DSpaceObject iu : objectsToUpdate)
            {
                /* we let all types through here and 
                 * allow the search DSIndexer to make 
                 * decisions on indexing and/or removal
                 */
                String hdl = iu.getHandle();
                if (hdl != null && !handlesToDelete.contains(hdl))
                {
                    try
                    {
                        DSIndexer.indexContent(ctx, iu, true);
                        log.debug("Indexed "
                             + Constants.typeText[iu.getType()]
                             + ", id=" + String.valueOf(iu.getID())
                             + ", handle=" + hdl);
                    }
                    catch (Exception e)
                    {
                        log.error("Failed while indexing object: ", e);
                    }
                }
            }

            for (String hdl : handlesToDelete)
            {
                try
                {
                    DSIndexer.unIndexContent(ctx, hdl);
                    if (log.isDebugEnabled())
                    {
                        log.debug("UN-Indexed Item, handle=" + hdl);
                    }
                }
                catch (Exception e)
                {
                    log.error("Failed while UN-indexing object: " + hdl, e);
                }

            }

        }
        
        // "free" the resources
        objectsToUpdate = null;
        handlesToDelete = null;
    }

    public void finish(Context ctx) throws Exception
    {
        // No-op

    }

}
