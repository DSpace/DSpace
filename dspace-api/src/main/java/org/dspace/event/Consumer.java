/*
 * Consumer.java
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
