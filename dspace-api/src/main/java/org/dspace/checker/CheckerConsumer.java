/*
 * CheckerConsumer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Class for removing Checker data for a Bitstreams based on deletion events.
 *
 * @version $Revision$
 */
public class CheckerConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(CheckerConsumer.class);
    
    private BitstreamInfoDAO bitstreamInfoDAO = new BitstreamInfoDAO();
    
    /**
     * Initialize - allocate any resources required to operate.
     * Called at the start of ANY sequence of event consume() calls.
     */
    public void initialize() throws Exception
    {
    	// no-op
    }
    
    /**
     * Consume an event
     *
     * @param ctx       the execution context object
     *
     * @param event the content event
     */
    public void consume(Context ctx, Event event) throws Exception
    {
        
    	if (event.getEventType() == Event.DELETE)
    	{
            log.debug("Attempting to remove Checker Info");
    	    bitstreamInfoDAO.deleteBitstreamInfoWithHistory(event.getSubjectID());
            log.debug("Completed removing Checker Info");
    	}
    }
   
    /**
     * Signal that there are no more events queued in this
     * event stream.
     */
    public void end(Context ctx) throws Exception
    {
    	// no-op
    }
     
    /**
     * Finish - free any allocated resources.
     * Called when consumer is being released
     */
    public void finish(Context ctx) throws Exception
    {
    	// no-op
    }
}
