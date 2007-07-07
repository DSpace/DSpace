/*
 * SystemwideAlerts.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.administrative;

import java.io.Serializable;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Metadata;
import org.dspace.app.xmlui.wing.element.PageMeta;

/**
 * This class maintains any system-wide alerts. If any alerts are activated then
 * they are added to the page's metadata for display by the theme.
 * 
 * This class also creates an interface for the alert system to be maintained.
 * 
 * @author Scott Phillips
 */
public class SystemwideAlerts extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
	/** Language Strings */
	private static final Message T_COUNTDOWN = message("xmlui.administrative.SystemwideAlerts.countdown");

	// Is an alert activated?
	private static boolean active;
	
	// The alert's message
	private static String message;
	
	// If a count down time is present, what time are we counting down too?
	private static long countDownToo;
	
	/**
     * Generate the unique caching key.
     */
    public Serializable getKey()
    {
    	if (active)
    		// Don't cache any alert messages
    		return null;
    	else
    		return "1";
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
    	if (active)
    		return null;
    	else
    		return NOPValidity.SHARED_INSTANCE;
    }
	
    /**
     * If an alert is activated then add a count down message.
     */
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        
		if (active)
		{
	        Metadata alert = pageMeta.addMetadata("alert","message");
	        
	        long time = countDownToo - System.currentTimeMillis();
	        if (time > 0)
	        {
	        	// from milliseconds to minutes
	        	time = time / (60*1000); 
	        	
	        	alert.addContent(T_COUNTDOWN.parameterize(time));
	        }

	        alert.addContent(message);
		}
    }

	/**
	 * Check whether an alert is active.
	 */
	public static boolean isAlertActive()
	{
		return SystemwideAlerts.active;
	}
	
	/**
	 * Activate the current alert.
	 */
	public static void activateAlert()
	{
		SystemwideAlerts.active = true;
	}
	
	/**
	 * Deactivate the current alert.
	 */
	public static void deactivateAlert()
	{
		SystemwideAlerts.active = false;
	}
	
	/**
	 * Set the current alert's message.
	 * @param message The new message
	 */
	public static void setMessage(String message)
	{
		SystemwideAlerts.message = message;
	}
	
	/**
	 * @return the current alert's message
	 */
	public static String getMessage()
	{
		return SystemwideAlerts.message;
	}

	/**
	 * Get the time, in millieseconds, when the countdown timer is scheduled to end.
	 */
	public static long getCountDownToo()
	{
		return SystemwideAlerts.countDownToo;
	}
	
	/**
	 * Set the time, in millieseconds, to which the countdown timer should end.
	 * 
	 * Note, that once the countdown has expried, the alert is
	 * still active. However the countdown will disappear.
	 */
	public static void setCountDownToo(long countDownTo)
	{
		SystemwideAlerts.countDownToo = countDownTo;
	}
	
	
	
}
