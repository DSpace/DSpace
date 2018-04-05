/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.checker.factory.CheckerServiceFactory;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
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
    
    protected ChecksumHistoryService checksumHistoryService = CheckerServiceFactory.getInstance().getChecksumHistoryService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    
    /**
     * Initialize - allocate any resources required to operate.
     * Called at the start of ANY sequence of event consume() calls.
     * @throws Exception if error
     */
    @Override
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
     * @throws Exception if error
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception
    {
        
    	if (event.getEventType() == Event.DELETE)
    	{
            Bitstream bitstream = bitstreamService.find(ctx, event.getSubjectID());
            log.debug("Attempting to remove Checker Info");
            checksumHistoryService.deleteByBitstream(ctx, bitstream);
            log.debug("Completed removing Checker Info");
    	}
    }
   
    /**
     * Signal that there are no more events queued in this
     * event stream.
     * @param ctx Context
     * @throws Exception if error
     */
    @Override
    public void end(Context ctx) throws Exception
    {
    	// no-op
    }
     
    /**
     * Finish - free any allocated resources.
     * Called when consumer is being released
     * @param ctx Context
     * @throws Exception if error
     */
    @Override
    public void finish(Context ctx) throws Exception
    {
    	// no-op
    }
}
