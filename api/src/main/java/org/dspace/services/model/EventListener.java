/**
 * $Id: EventListener.java 3599 2009-03-17 07:23:54Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/model/EventListener.java $
 * EventListener.java - DSpace2 - Oct 9, 2008 2:43:00 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
 */

package org.dspace.services.model;



/**
 * This will be triggered when events occur in the system <br/>
 * Allows a developer to be notifed when specific events occur by
 * implementing this interface
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface EventListener {

    /**
     * This defines the events that you want to know about by event name,
     * {@link #receiveEvent(Event)} will be called whenever an event occurs which has a name
     * which begins with any of the strings this method returns, simply return empty array if you do
     * not want to match events this way<br/>
     * <br/> <b>Note:</b> Can be used with {@link #getResourcePrefix()}
     * 
     * @return an arrays of event name prefixes
     */
    public String[] getEventNamePrefixes();

    /**
     * This defines the events that you want to know about by event resource (reference),
     * {@link #receiveEvent(Event)} will be called whenever an event occurs which has a
     * resource which begins with the string this method returns, simply return empty string to match
     * no events this way<br/> 
     * <br/> 
     * <b>Note:</b> Can be used with {@link #getEventNamePrefixes()}
     * 
     * @return a string with a resource (reference) prefix
     */
    public String getResourcePrefix();

    /**
     * This defines what should happen when an event occurs which passes through the filters
     * created by {@link #getEventNamePrefixes()} and {@link #getResourcePrefix()}
     * 
     * @param event includes all the information related to the event that occurred
     */
    public void receiveEvent(Event event);

}
