/*
 * BrowseConsumer.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/04/10 04:11:09 $
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

package org.dspace.browse;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;

import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.event.EventManager;

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
 * @version $Revision: 1.1 $
 */
public class BrowseConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(BrowseConsumer.class);
    
    // items to be added to browse index
    private Set toAdd = null;

    // items to be updated in browse index
    private Set toUpdate = null;


    public void initialize()
        throws Exception
    {
        toAdd = new HashSet();
        toUpdate = new HashSet();
    }

    public void consume(Context ctx, Event event)
        throws Exception
    {
        DSpaceObject subj = event.getSubject(ctx);
        int et = event.getEventType();

        // If an Item is added or modified..
        if (subj != null && subj.getType() == Constants.ITEM)
        {
            if (et == Event.CREATE)
                toAdd.add(subj);
            else
                toUpdate.add(subj);

        // track ADD and REMOVE from collections, that changes browse index.
        } else if (subj != null && subj.getType() == Constants.COLLECTION &&
                   event.getObjectType() == Constants.ITEM &&
                   (et == Event.ADD || et == Event.REMOVE))
        {
            DSpaceObject obj = event.getObject(ctx);
            if (obj != null)
                toUpdate.add(obj);
        }
        else if (subj != null)
            log.warn("consume() got unrecognized event: "+event.toString());
    }

    public void end(Context ctx)
        throws Exception
    {
        for (Iterator ai = toAdd.iterator(); ai.hasNext();)
        {
            Item i = (Item)ai.next();
            // FIXME: there is an exception handling problem here
            try
            {
            	// Update browse indices
            	IndexBrowse ib = new IndexBrowse(ctx);
            	ib.indexItem(i);
            }
            catch (BrowseException e)
            {
            	log.error("caught exception: ", e);
            	throw new SQLException(e.getMessage());
            }

            toUpdate.remove(i);
            if (log.isDebugEnabled())
                log.debug("Added browse indices for Item id="+String.valueOf(i.getID())+", hdl="+i.getHandle());
        }

        // don't update an item we've just added.
        for (Iterator ui = toUpdate.iterator(); ui.hasNext();)
        {
            Item i = (Item)ui.next();
            // FIXME: there is an exception handling problem here
            try
            {
            	// Update browse indices
            	IndexBrowse ib = new IndexBrowse(ctx);
            	ib.indexItem(i);
            }
            catch (BrowseException e)
            {
            	log.error("caught exception: ", e);
            	throw new SQLException(e.getMessage());
            }
        
            if (log.isDebugEnabled())
                log.debug("Updated browse indices for Item id="+String.valueOf(i.getID())+", hdl="+i.getHandle());
        }

        // NOTE: Removed items are necessarily handled inline (ugh).

        // browse updates wrote to the DB, so we have to commit.
        ctx.getDBConnection().commit();
        
        // clean out toAdd & toUpdate
        toAdd.clear();
        toUpdate.clear();
    }
    
    public void finish(Context ctx) {
    	
    	toAdd = toUpdate = null;
    	return;
    }
}
