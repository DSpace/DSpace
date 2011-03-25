/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import org.dspace.core.Context;

/**
 * Interface for content event consumers. Note that the consumer cannot tell if
 * it is invoked synchronously or asynchronously; the consumer interface and
 * sequence of calls is the same for both. Asynchronous consumers may see more
 * consume() calls between the start and end of the event stream, if they are
 * invoked asynchronously, once in a long time period, rather than synchronously
 * after every Context.commit().
 * 
 * @version $Revision$
 */
public interface Consumer
{
    /**
     * Initialize - allocate any resources required to operate. This may include
     * initializing any pooled JMS resources. Called ONCE when created by the
     * dispatcher pool. This should be used to set up expensive resources that
     * will remain for the lifetime of the consumer.
     */
    public void initialize() throws Exception;

    /**
     * Consume an event; events may get filtered at the dispatcher level, hiding
     * it from the consumer. This behavior is based on the dispatcher/consumer
     * configuration. Should include logic to initialize any resources required
     * for a batch of events.
     * 
     * @param ctx
     *            the execution context object
     * 
     * @param event
     *            the content event
     */
    public void consume(Context ctx, Event event) throws Exception;

    /**
     * Signal that there are no more events queued in this event stream and
     * event processing for the preceding consume calls should be finished up.
     */
    public void end(Context ctx) throws Exception;

    /**
     * Finish - free any allocated resources. Called when consumer (via it's
     * parent dispatcher) is going to be destroyed by the dispatcher pool.
     */
    public void finish(Context ctx) throws Exception;

}
