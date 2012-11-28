/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.servlet.http.HttpSession;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * 
 * This action simply records pipeline events that it sees, keeping track of
 * the users and pages they are viewing. Later the control panel's activity
 * viewer can access this data to get a realtime snap shot of current activity
 * of the repository.
 *
 * @author Scott Phillips
 */

public class CurrentActivityAction extends AbstractAction
{

	/** The maximum number of events that are recorded */
	public static final int MAX_EVENTS;
	
	/** The HTTP header that contains the real IP for this request, this is used when DSpace is placed behind a load balancer */
	public static final String IP_HEADER;
	
	/** The ordered list of events, by access time */
	private static Queue<Event> events = new LinkedList<Event>();
	
	/** record events that are from anynmous users */
	private static boolean recordAnonymousEvents = true;
	
	/** record events from automatic bots */
	private static boolean recordBotEvents = false;
	
	/**
	 * Allow the DSpace.cfg to override our activity max and ip header.
	 */
	static {
		// If the dspace.cfg has a max event count then use it.
		if (ConfigurationManager.getProperty("xmlui.controlpanel.activity.max") != null)
        {
			MAX_EVENTS = ConfigurationManager.getIntProperty("xmlui.controlpanel.activity.max");
        }
        else
        {
            MAX_EVENTS = 250;
        }
		
		if (ConfigurationManager.getProperty("xmlui.controlpanel.activity.ipheader") != null)
        {
			IP_HEADER = ConfigurationManager.getProperty("xmlui.controlpanel.activity.ipheader");
        }
        else
        {
            IP_HEADER = "X-Forwarded-For";
        }
	}
	
	
    /**
     * Record this current event.
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);
        
        // Ensure only one thread is manipulating the events queue at a time.
        synchronized (events) {
	        // Create and store our events
	        Event event = new Event(context,request);
	        
	        // Check if we should record the event
	        boolean record = true;
	        if (!recordAnonymousEvents && event.isAnonymous())
            {
	        	record = false;
            }

	        if (!recordBotEvents && event.isBot())
            {
	        	record = false;
            }
	        
	        if (record)
            {
	        	events.add(event);
            }
	        
	        // Remove the oldest element from the list if we are over our max
	        // number of elements.
	        while (events.size() > MAX_EVENTS)
            {
	        	events.poll();
            }
        }
       
        return null;
    }
    
    /**
     * @return a list of all current events.
     */
    public static List<Event> getEvents()
    {
    	List<Event> list = new ArrayList<Event>();
    	// Make sure only one thread is manipulating the events queue at a time.
    	synchronized (events) {
	    	list.addAll(events);
    	}
    	return list;
    }
    
	
    public static boolean getRecordAnonymousEvents() {
    	return recordAnonymousEvents;
    }
    
    public static void setRecordAnonymousEvents(boolean record) {
    	recordAnonymousEvents = record;
    }
    
    public static boolean getRecordBotEvents() {
    	return recordBotEvents;
    }
    
    public static void setRecordBotEvents(boolean record) {
    	recordBotEvents = record;
    }
    
    /**
     * An object that represents an activity event.
     */
    public static class Event
    {
    	/** The Servlet session */
    	private String sessionID;
    	
    	/** The eperson ID */
    	private int epersonID = -1;
    	
    	/** The url being viewed */
    	private String url;
    	
    	/** A timestap of this event */
    	private long timestamp;
    	
    	/** The user-agent for the request */
    	private String userAgent;
    	
    	/** The ip address of the requester */
    	private String ip;
    	
    	/**
    	 * Construct a new activity event, grabing various bits of data about the request from the context and request.
    	 */
    	public Event(Context context, Request request)
    	{
    		if (context != null)
    		{
    			EPerson eperson = context.getCurrentUser();
    			if (eperson != null)
                {
                    epersonID = eperson.getID();
                }
    		}
    		
    		if (request != null)
    		{
    			url = request.getSitemapURI();
    			HttpSession session = request.getSession(true);
    			if (session != null)
                {
                    sessionID = session.getId();
                }
    			
    			userAgent = request.getHeader("User-Agent");
    			
    			ip = request.getHeader(IP_HEADER);
    			if (ip == null)
                {
                    ip = request.getRemoteAddr();
                }
    		}
    		
    		// The current time
    		timestamp = System.currentTimeMillis();
    	}
    	
  
    	public String getSessionID()
    	{
    		return sessionID;
    	}
    	
    	public int getEPersonID()
    	{
    		return epersonID;
    	}
    	
    	public String getURL()
    	{
    		return url;
    	}
    	
    	public long getTimeStamp()
    	{
    		return timestamp;
    	}
    	
    	public String getUserAgent()
    	{
    		return userAgent;
    	}
    	
    	public String getIP()
    	{
    		return ip;
    	}
    	
    	/**
    	 * Is this event anonymous?
    	 */
    	public boolean isAnonymous()
    	{
    		return (epersonID == -1);
    	}
    	
    	/**
    	 * Is this event from a bot?
    	 */
    	public boolean isBot()
    	{
	    if (userAgent == null)
            {
                return false;
            }
	    String ua = userAgent.toLowerCase();

	    return (ua.contains("google/") ||
		    ua.contains("msnbot/") ||
		    ua.contains("googlebot/") || 
		    ua.contains("webcrawler/") ||
		    ua.contains("inktomi") ||
		    ua.contains("teoma") ||
		    ua.contains("bot"));
    	}
    	
    	/**
    	 * Return the activity viewer's best guess as to what browser or bot
	 * was initiating the request.
    	 *
    	 * @return A short name for the browser or bot.
    	 */
    	public String getDectectedBrowser()
    	{
    		if (userAgent == null)
            {
                return "No browser provided";
            }

            String userAgentLower = userAgent.toLowerCase();
    		
    		// BOTS
    		if (userAgentLower.contains("google/"))
            {
                return "Google (bot)";
            }
    		
    		if (userAgentLower.contains("msnbot/"))
            {
                return "MSN (bot)";
            }
    		   
    	    if (userAgentLower.contains("googlebot/"))
            {
                return "Google (bot)";
            }
    		
    		if (userAgentLower.contains("webcrawler/"))
            {
                return "WebCrawler (bot)";
            }
    		
    		if (userAgentLower.contains("inktomi"))
            {
                return "Inktomi (bot)";
            }
    		
    		if (userAgentLower.contains("teoma"))
            {
                return "Teoma (bot)";
            }
    		
    		if (userAgentLower.contains("bot"))
            {
                return "Unknown (bot)";
            }
    		
    		// Normal Browsers
    		if (userAgent.contains("Lotus-Notes/"))
            {
                return "Lotus-Notes";
            }
    		
    		if (userAgent.contains("Opera"))
            {
                return "Opera";
            }
    		
    		if (userAgent.contains("Safari/"))
    		{
    		    if (userAgent.contains("Chrome"))
                {
                    return "Chrome";
                }
    		
    			return "Safari";
    		}
    		
    		if (userAgent.contains("Konqueror/"))
            {
                return "Konqueror";
            }
    		
    		// Internet explorer browsers
    		if (userAgent.contains("MSIE"))
    		{
    		    if (userAgent.contains("MSIE 9"))
                {
                    return "MSIE 9";
                }
    		    if (userAgent.contains("MSIE 8"))
                {
                    return "MSIE 8";
                }
    		    if (userAgent.contains("MSIE 7"))
                {
                    return "MSIE 7";
                }
    		    if (userAgent.contains("MSIE 6"))
                {
                    return "MSIE 6";
                }
    		    if (userAgent.contains("MSIE 5"))
                {
                    return "MSIE 5";
                }
    		    
    		    // Can't fine the version number
    			return "MSIE";
    		}
    		
    		// Gecko based browsers
    		if (userAgent.contains("Gecko/"))
    		{
    		    if (userAgent.contains("Camio/"))
                {
                    return "Gecko/Camino";
                }
    		    
    		    if (userAgent.contains("Chimera/"))
                {
                    return "Gecko/Chimera";
                }
    		    
    		    if (userAgent.contains("Firebird/"))
                {
                    return "Gecko/Firebird";
                }
    		    
    		    if (userAgent.contains("Phoenix/"))
                {
                    return "Gecko/Phoenix";
                }
    		    
    		    if (userAgent.contains("Galeon"))
                {
                    return "Gecko/Galeon";
                }
    		    
    		    if (userAgent.contains("Firefox/1"))
                {
                    return "Firefox 1.x";
                }
    		    
    		    if (userAgent.contains("Firefox/2"))
                {
                    return "Firefox 2.x";
                }
    		    
    		    if (userAgent.contains("Firefox/3"))
                {
                    return "Firefox 3.x";
                }
    		    
    		    if (userAgent.contains("Firefox/4"))
                {
                    return "Firefox 4.x";
                }
    		   
    		    if (userAgent.contains("Firefox/"))
                {
                    return "Firefox"; // can't find the exact version
                }
    		    
    		    if (userAgent.contains("Netscape/"))
                {
                    return "Netscape";
                }
    		    
    		    // Can't find the exact distribution
    			return "Gecko";
    		}
    		
    		// Generic browser types (lots of things report these names)
    		
    		if (userAgent.contains("KHTML/"))
            {
                return "KHTML";
            }
    		
    		if (userAgent.contains("Netscape/"))
            {
                return "Netscape";
            }
    		
    		if (userAgent.contains("Mozilla/"))
            {
                return "Mozilla"; // Almost everything says they are mozilla.
            }
    		
    		// if you get all the way to the end and still nothing, return unknown.
    		return "Unknown";
    	}  
    }
    
    
    

}
