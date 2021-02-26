/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.service;

import org.dspace.event.Dispatcher;

/**
 * Class for managing the content event environment. The EventManager mainly
 * acts as a factory for Dispatchers, which are used by the Context to send
 * events to consumers. It also contains generally useful utility methods.
 *
 * Version: $Revision$
 */
public interface EventService {

    // The name of the default dispatcher assigned to every new context unless
    // overridden
    public static final String DEFAULT_DISPATCHER = "default";

    /**
     * Get dispatcher for configuration named by "name". Returns cached instance
     * if one exists.
     *
     * @param name dispatcher name
     * @return chached instance of dispatcher
     */
    public Dispatcher getDispatcher(String name);

    public void returnDispatcher(String key, Dispatcher disp);

    public int getConsumerIndex(String consumerClass);
}
