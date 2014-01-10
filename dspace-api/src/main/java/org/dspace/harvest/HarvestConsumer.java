/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.apache.log4j.Logger;
import org.dspace.core.*;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

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

    /**
     * Initialise the consumer
     *
     * @throws Exception
     */
    public void initialize()
        throws Exception
    {

    }

    /**
     * Consume the event
     *
     * @param context
     * @param event
     * @throws Exception
     */
    public void consume(Context context, Event event)
        throws Exception
    {
    	int st = event.getSubjectType();
	    int et = event.getEventType();
	    int id = event.getSubjectID();
	
	    switch (st)
	    {
	        case Constants.ITEM:
	            if (et == Event.DELETE)
	            {
	            	HarvestedItem hi = HarvestedItem.find(context, id);
	            	if (hi != null) {
	            		log.debug("Deleted item '" + id + "', also deleting associated harvested_item '" + hi.getOaiID() + "'.");
	            		hi.delete();
	            		hi.update();
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
	        		HarvestedCollection hc = HarvestedCollection.find(context, id);
	            	if (hc != null) {
	            		log.debug("Deleted collection '" + id + "', also deleting associated harvested_collection '" + hc.getOaiSource() + ":" + hc.getOaiSetId() + "'.");
	            		hc.delete();
	            		hc.update();
	            	}	            		
	            	else
                    {
                        log.debug("Deleted collection '" + id + "' and the associated harvested_collection.");
                    }
	            }
	        default:
	            log.warn("consume() got unrecognized event: " + event.toString());
	    }
    }

    /**
     * Handle the end of the event
     *
     * @param ctx
     * @throws Exception
     */
    public void end(Context ctx)
        throws Exception
    {

    }

    /**
     * Finish the event
     *
     * @param ctx
     */
    public void finish(Context ctx)
    {

    }
}
