/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dspace.core.Context;

/**
 * Interface for event dispatchers. The primary role of a dispatcher is to
 * deliver a set of events to a configured list of consumers. It may also
 * transform, consolidate, and otherwise optimize the event stream prior to
 * delivering events to its consumers.
 * 
 * @version $Revision$
 */
public abstract class Dispatcher
{
    protected String name;

    /** unique identifier of this dispatcher - cached hash of its text Name */
    protected int identifier;

    /**
     * Map of consumers by their configured name.
     */
    protected Map<String, ConsumerProfile> consumers = new LinkedHashMap<String, ConsumerProfile>();

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
     * @return unique integer that identifies this Dispatcher configuration.
     */
    public int getIdentifier()
    {
        return identifier;
    }

    /**
     * Add a consumer profile to the end of the list.
     * 
     * @param cp
     *            the event consumer profile to add
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
