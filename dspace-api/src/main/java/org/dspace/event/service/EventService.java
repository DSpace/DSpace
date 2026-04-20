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
    String DEFAULT_DISPATCHER = "default";

    /**
     * Get dispatcher for configuration named by "name". Returns cached instance
     * if one exists.
     *
     * @param name dispatcher name
     * @return cached instance of dispatcher
     */
    Dispatcher getDispatcher(String name);

    /**
     * Returns a Dispatcher instance to the service once it is no longer required.
     * <p>
     * This method releases the resources associated with the dispatcher,
     * allowing the service to reclaim or recycle the instance for future
     * event processing tasks.
     * </p>
     *
     * @param key  the identifier of the dispatcher to be returned.
     * @param disp the Dispatcher instance to return.
     * @throws IllegalStateException if the dispatcher cannot be processed by the service.
     */
    void returnDispatcher(String key, Dispatcher disp);

    /**
     * Retrieves the unique numerical index associated with a specific consumer.
     * <p>
     * This index is typically used by the event system to track the
     * state of event processing across different consumers using bitsets.
     * </p>
     *
     * @param consumerClass the configured name of the consumer.
     * @return the unique index of the consumer, or -1 if the consumer
     * is not recognized.
     */
    int getConsumerIndex(String consumerClass);

    /**
     * Reload the dispatcher configuration.
     */
    void reloadConfiguration();
}
