/*
 * AbstractUsageEvent.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (C) 2008, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of the DSpace Foundation nor the names of their
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
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

package org.dspace.app.statistics;

import org.dspace.eperson.EPerson;

/**
 * Base class to be extended by usage event handlers.
 * 
 * @author Mark H. Wood
 * @version $Revision$
 */
public abstract class AbstractUsageEvent
{
    /** Event is "object has been viewed or downloaded" */
    public static final int VIEW = 1;

    /** Which session sent the query. */
    protected String sessionID;

    /** Address from which the query was received */
    protected String sourceAddress;

    /** The EPerson making the request, or null if not logged on */
    protected EPerson eperson;

    /** What happened? Viewed, logged on, etc. */
    protected int eventType;

    /**
     * Type of object which experienced the event. Bitstream, item, etc. See
     * {@link org.dspace.core.Constants Constants} for values.
     */
    protected int objectType;

    /** Identity of specific object which experienced the event */
    protected int objectID;

    /**
     * Because the PluginManager can only call a plugin's niladic constructor,
     * the constructor returns an "empty" event. It must be populated using the
     * setter methods before "firing".
     */
    public AbstractUsageEvent()
    {
        super();
    }

    /**
     * @param id
     *            opaque session identifier returned by the HTTP request
     */
    public void setSessionID(String id)
    {
        sessionID = id;
    }

    /** */
    public String getSessionID()
    {
        return sessionID;
    }

    /**
     * @param address
     *            the address from which the HTTP request came
     */
    public void setSource(String address)
    {
        sourceAddress = address;
    }

    /** */
    public String getSource()
    {
        return sourceAddress;
    }

    /**
     * @param user
     *            an object representing the logged-on user, if any. May be
     *            null.
     */
    public void setEperson(EPerson user)
    {
        eperson = user;
    }

    /** */
    public EPerson getEperson()
    {
        return eperson;
    }

    /**
     * @param type
     *            the type of event (view, logon, etc.)
     */
    public void setEventType(int type)
    {
        eventType = type;
    }

    /** */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * @param type
     *            the type of object experiencing the event (bitstream, etc.)
     */
    public void setObjectType(int type)
    {
        objectType = type;
    }

    /** */
    public int getObjectType()
    {
        return objectType;
    }

    /**
     * @param id
     *            the identifier of the specific object experiencing the event
     */
    public void setID(int id)
    {
        objectID = id;
    }

    /** */
    public int getID()
    {
        return objectID;
    }

    /** Called when the event is fully configured, to process the data. */
    abstract public void fire();
}
