/**
 * UsageEvent.java
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

package org.dspace.app.xmlui.utils;

import org.apache.cocoon.environment.Request;

import org.dspace.app.statistics.AbstractUsageEvent;
import org.dspace.app.statistics.PassiveUsageEvent;
import org.dspace.core.Context;
import org.dspace.core.PluginConfigurationError;
import org.dspace.core.PluginManager;

/**
 * Facade to hide the PluginManager guts from usage event producers.
 * 
 * Some configured flavor of AbstractUsageEvent is created and wrapped.
 * This allows the caller to simply <code>new UsageEvent().fire(args);</code>
 * or the like without having to deal with configuration plumbing.
 * Suggested by Mark Diggory, dspace-devel@lists.sourceforge.net, 27-Mar-2008.
 * 
 * @author mwood@IUPUI.Edu
 */
public class UsageEvent extends AbstractUsageEvent
{
    /** The actual event, of whatever type is configured */
    AbstractUsageEvent ue;

    /**
     * Wrap the creation of an internal AbstractUsageEvent for later use
     */
    public UsageEvent()
    {
        try
        {
            ue = (AbstractUsageEvent) PluginManager
                    .getSinglePlugin(AbstractUsageEvent.class);
        }
        catch (PluginConfigurationError pce)
        {
            ue = new PassiveUsageEvent();
        }
    }

    /** @see org.dspace.app.statistics.AbstractUsageEvent#fire() */
    public void fire()
    {
        ue.fire();
    }
    
    /**
     * Convenience to fill and fire an event all in one call, for XMLUI
     * 
     * @param request
     *            the Request passed by Cocoon
     * @param context
     *            the DSpace context for this request
     * @param eventType
     *            the type of event (view, logon, etc.)
     * @param objectType
     *            the type of object experiencing the event (bitstream, etc.)
     * @param objectID
     *            the identifier of the specific object experiencing the event
     */
   public void fire(Request request, Context context,
            int eventType, int objectType, int objectID)
    {
        ue.setSessionID(request.getSession(true).getId());
        ue.setSource(request.getRemoteAddr());
        ue.setEperson(context.getCurrentUser());
        ue.setEventType(eventType);
        ue.setObjectType(objectType);
        ue.setID(objectID);
        ue.fire();
    }
}
