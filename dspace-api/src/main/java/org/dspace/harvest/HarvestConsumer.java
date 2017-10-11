/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;

import java.util.UUID;

/**
 * Class for handling cleanup of harvest settings for collections and items
 *
 *
 * @version $Revision: 3705 $
 *
 * @author Stuart Lewis
 * @author Alexey Maslov
 */
public class HarvestConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(HarvestConsumer.class);

    protected HarvestedCollectionService harvestedCollectionService;
    protected HarvestedItemService harvestedItemService;

    /**
     * Initialise the consumer
     *
     * @throws Exception if error
     */
    @Override
    public void initialize()
        throws Exception
    {
        harvestedItemService = HarvestServiceFactory.getInstance().getHarvestedItemService();
    }

    /**
     * Consume the event
     *
     * @param context
     *     The relevant DSpace Context.
     * @param event
     *     DSpace event
     * @throws Exception if error
     */
    @Override
    public void consume(Context context, Event event)
        throws Exception
    {
        int st = event.getSubjectType();
        int et = event.getEventType();
        UUID id = event.getSubjectID();
    
        switch (st)
        {
            case Constants.ITEM:
                if (et == Event.DELETE)
                {
                    HarvestedItem hi = harvestedItemService.find(context, (Item) event.getSubject(context));
                    if (hi != null) {
                        log.debug("Deleted item '" + id + "', also deleting associated harvested_item '" + hi.getOaiID() + "'.");
                        harvestedItemService.delete(context, hi);
                    }
                    else
                    {
                        log.debug("Deleted item '" + id + "' and the associated harvested_item.");
                    }
                } 
                break;
            case Constants.COLLECTION:
                if (et == Event.DELETE)
                {
                    HarvestedCollection hc = harvestedCollectionService.find(context, (Collection) event.getSubject(context));
                    if (hc != null) {
                        log.debug("Deleted collection '" + id + "', also deleting associated harvested_collection '" + hc.getOaiSource() + ":" + hc.getOaiSetId() + "'.");
                        harvestedCollectionService.delete(context, hc);
                    }
                    else
                    {
                        log.debug("Deleted collection '" + id + "' and the associated harvested_collection.");
                    }
                }
                break;
            default:
                log.warn("consume() got unrecognized event: " + event.toString());
        }
    }

    /**
     * Handle the end of the event
     *
     * @param ctx
     *     The relevant DSpace Context.
     * @throws Exception if error
     */
    @Override
    public void end(Context ctx)
        throws Exception
    {

    }

    /**
     * Finish the event
     *
     * @param ctx
     *     The relevant DSpace Context.
     */
    @Override
    public void finish(Context ctx)
    {

    }
}
