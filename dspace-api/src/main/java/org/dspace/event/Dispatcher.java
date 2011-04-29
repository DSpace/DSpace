/*
 * Dispatcher.java
 *
 * Version: $Revision: 3762 $
 *
 * Date: $Date: 2009-05-07 00:36:47 -0400 (Thu, 07 May 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dspace.core.Context;

/**
 * Interface for event dispatchers. The primary role of a dispatcher is to
 * deliver a set of events to a configured list of consumers. It may also
 * transform, consolidate, and otherwise optimize the event stream prior to
 * delivering events to its consumers.
 * 
 * @version $Revision: 3762 $
 */
public abstract class Dispatcher
{
    protected String name;

    /** unique identifer of this dispatcher - cached hash of its text Name */
    protected int identifier;

    /**
     * Map of consumers by their configured name.
     */
    protected Map<String, ConsumerProfile> consumers = new HashMap<String, ConsumerProfile>();

    protected Dispatcher(String name)
    {
        super();
        this.name = name;
        this.identifier = name.hashCode();
    }

    public Collection getConsumers()
    {
        return consumers.values();
    }

    /**
     * @returns unique integer that identifies this Dispatcher configuration.
     */
    public int getIdentifier()
    {
        return identifier;
    }

    /**
     * Add a consumer to the end of the list.
     * 
     * @param consumer
     *            the event consumer to add
     * @param filter
     *            the event filter to apply
     */
    public abstract void addConsumerProfile(ConsumerProfile cp)
            throws IllegalArgumentException;

    /**
     * Dispatch all events added to this Context according to configured
     * consumers.
     * 
     * @param ctx
     *            the execution context object
     */
    public abstract void dispatch(Context ctx);

}
