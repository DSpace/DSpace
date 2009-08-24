/*
 * HarvestConsumer.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 12:02:24 -0500 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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

package org.dspace.harvest;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import javax.mail.MessagingException;
import java.util.Date;

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
     * Initalise the consumer
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
	            		log.debug("Deleted item '" + id + "' and the associated harvested_item.");
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
	            		log.debug("Deleted collection '" + id + "' and the associated harvested_collection.");
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
