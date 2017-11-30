/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.model;



/**
 * These will be triggered when specific events occur in the system.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface EventListener {

    /**
     * This defines the events that you want to know about by event name.
     * {@link #receiveEvent(Event)} will be called whenever an event 
     * occurs which has a name that begins with any of the strings this
     * method returns.  Simply return an empty array if you do
     * not want to match events this way.
     * <p>
     * <b>Note:</b>  Can be used with {@link #getResourcePrefix()}.
     * 
     * @return an arrays of event name prefixes
     */
    public String[] getEventNamePrefixes();

    /**
     * This defines the events that you want to know about by event 
     * resource (reference).
     * {@link #receiveEvent(Event)} will be called whenever an event 
     * occurs which has a resource that begins with the string this
     * method returns.  Simply return an empty string to match
     * no events this way.
     * <p>
     * <b>Note:</b>  Can be used with {@link #getEventNamePrefixes()}.
     * 
     * @return a string with a resource (reference) prefix
     */
    public String getResourcePrefix();

    /**
     * Called when an event occurs which passes through the filters
     * created by {@link #getEventNamePrefixes()} and
     * {@link #getResourcePrefix()}.
     * 
     * @param event includes all the information related to the event that occurred
     */
    public void receiveEvent(Event event);

}
