/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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

	/** Possible user restricted states */
	public static final int STATE_ALL_SESSIONS = 1;
	public static final int STATE_CURRENT_SESSIONS = 2;
	public static final int STATE_ONLY_ADMINISTRATIVE_SESSIONS = 3;
	
	
	// Is an alert activated?
	private static boolean active;
	
	// The alert's message
	private static String message;
	
	// If a count down time is present, what time are we counting down too?
	private static long countDownToo;
	
	// Can users use the website?
	private static int restrictsessions = STATE_ALL_SESSIONS;
	
	/**
     * Generate the unique caching key.
     * @return the key.
     */
    @Override
    public Serializable getKey()
    {
    	if (active)
        {
            // Don't cache any alert messages
            return null;
        }
    	else
        {
            return "1";
        }
    }

    /**
     * Generate the cache validity object.
     * @return the validity.
     */
    @Override
    public SourceValidity getValidity()
    {
    	if (active)
        {
            return null;
        }
    	else
        {
            return NOPValidity.SHARED_INSTANCE;
        }
    }
	
    /**
     * If an alert is activated then add a count down message.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    @Override
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
     * @return true if active.
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
     * @return countdown end time.
	 */
	public static long getCountDownToo()
	{
		return SystemwideAlerts.countDownToo;
	}
	
	/**
	 * Set the time, in millieseconds, to which the countdown timer should end.
	 * 
	 * Note, that once the countdown has expired, the alert is
	 * still active. However the countdown will disappear.
     * @param countDownTo countdown end time.
	 */
	public static void setCountDownToo(long countDownTo)
	{
		SystemwideAlerts.countDownToo = countDownTo;
	}

	// Can users login or continue to use the system?
	public static int getRestrictSessions()
	{
		return SystemwideAlerts.restrictsessions;
	}
	
	// Set the ability to restrict use of the system
	public static void setRestrictSessions(int restrictsessions)
	{
		if (restrictsessions == STATE_ALL_SESSIONS ||
			restrictsessions == STATE_CURRENT_SESSIONS ||
			restrictsessions == STATE_ONLY_ADMINISTRATIVE_SESSIONS)
        {
            SystemwideAlerts.restrictsessions = restrictsessions;
        }
	}
	
	
	/**
	 * Are users able to start a new session, will return false if there is 
	 * a current alert activated and sessions are restricted.
	 * 
	 * @return if false do not allow user to start a new session, otherwise no restriction.
	 */
	public static boolean canUserStartSession()
	{
        return !SystemwideAlerts.active ||
                (restrictsessions != STATE_ONLY_ADMINISTRATIVE_SESSIONS &&
                        restrictsessions != STATE_CURRENT_SESSIONS);
    }
	
	/**
	 * Are users able to maintain a session, will return false if there is 
	 * a current alert activated and sessions are restricted.
	 * 
	 * @return if false do not allow user to maintain their current session 
	 * or start a new session, otherwise no restriction.
	 */
	public static boolean canUserMaintainSession()
	{
        return !SystemwideAlerts.active || restrictsessions != STATE_ONLY_ADMINISTRATIVE_SESSIONS;
    }
}
