/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;


/**
 * Allows the creation of system events and registration of event listeners
 * for notification of system events.
 * The service can be configured to log events or ignore certain events.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface EventService {

    /**
     * Consumes an event asynchronously by retrieving it from the given supplier
     *
     * @param eventConsumer
     */
    void scheduleAsyncEventConsumer(Consumer<Consumer<Event>> eventConsumer);

    /**
     * Fires an event immediately (does not add it to the queue).
     *
     * @param event contains the data related to this event
     */
    public void fireEvent(Event event);

    /**
     * Register an event listener which will be notified when events occur.
     *
     * @param listener an implementation of the event listener
     */
    public void registerEventListener(EventListener listener);

    /**
     * Fires an event asynchronously by retrieving it from the given supplier
     *
     * @param eventSupplier
     */
    void fireAsyncEvent(Supplier<? extends Event> eventSupplier);

}
