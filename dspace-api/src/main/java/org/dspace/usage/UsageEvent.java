/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import javax.servlet.http.HttpServletRequest;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Event;

/**
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 *
 */
public class UsageEvent extends Event {
	
	public static enum Action {
		VIEW ("view"),
		CREATE ("create"),
		UPDATE ("update"),
		DELETE ("delete"),
		ADD ("add"),
		REMOVE ("remove"),
		BROWSE ("browse"),
		SEARCH ("search"),
		WORKFLOW ("workflow"),
		LOGIN ("login"),
		SUBSCRIBE ("subscribe"),
		UNSUBSCRIBE ("unsubscribe"),
		WITHDRAW ("withdraw"),
		REINSTATE ("reinstate"); 
		
		private final String text;
	    
	    Action(String text) {
	        this.text = text;
	    }
	    String text()   { return text; }
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private HttpServletRequest request;
	
	private String ip;
	
	private String userAgent;
	
	private String xforwardedfor;
	
	private Context context;
	
	private DSpaceObject object;

	private Action action;

	private static String checkParams(Action action, HttpServletRequest request, Context context, DSpaceObject object)
	{
        StringBuilder eventName = new StringBuilder();
		if(action == null)
        {
            throw new IllegalStateException("action cannot be null");
        }
			
		if(action != Action.WORKFLOW && request == null)
        {
            throw new IllegalStateException("request cannot be null");
        }
		

		if(context == null)
        {
            throw new IllegalStateException("context cannot be null");
        }
		
		if(action != Action.WORKFLOW && action != Action.SEARCH && object == null)
        {
            throw new IllegalStateException("object cannot be null");
        }else
        if(object != null){
            String objText = Constants.typeText[object.getType()].toLowerCase();
            eventName.append(objText).append(":");
        }
        eventName.append(action.text());
        
		return eventName.toString();
	}
	
	private static String checkParams(Action action, Context context, DSpaceObject object)
	{
        StringBuilder eventName = new StringBuilder();
		if(action == null)
        {
            throw new IllegalStateException("action cannot be null");
        }
			
//		if(action != Action.WORKFLOW)
//        {
//            throw new IllegalStateException("request cannot be null");
//        }
		

		if(context == null)
        {
            throw new IllegalStateException("context cannot be null");
        }
		
		if(action != Action.WORKFLOW && action != Action.SEARCH && object == null)
        {
            throw new IllegalStateException("object cannot be null");
        }else
        if(object != null){
            String objText = Constants.typeText[object.getType()].toLowerCase();
            eventName.append(objText).append(":");
        }
        eventName.append(action.text());
        

		return eventName.toString();
	}
	
	public UsageEvent(Action action, HttpServletRequest request, Context context, DSpaceObject object)
	{
		
		super(checkParams(action, request, context, object));
		
		this.action = action;
	
		this.setResourceReference(object != null ? Constants.typeText[object.getType()].toLowerCase() + ":" + object.getID() : null);
		
		switch(action)
		{
			case CREATE:
			case UPDATE:
			case DELETE:
			case WITHDRAW:
			case REINSTATE:	
			case ADD:
			case REMOVE:
				this.setModify(true);
				break;
			default : 
				this.setModify(false);
		}
		
		if(context != null && context.getCurrentUser() != null)
		{
			this.setUserId(
					String.valueOf(context.getCurrentUser().getID()));
		}
		this.request = request;
		this.context = context;
		this.object = object;
	}

	public UsageEvent(Action action, String ip, String userAgent, String xforwardedfor, Context context, DSpaceObject object)
	{
		
		super(checkParams(action, context, object));
		
		this.action = action;
	
		this.setResourceReference(object != null ? Constants.typeText[object.getType()].toLowerCase() + ":" + object.getID() : null);
		
		switch(action)
		{
			case CREATE:
			case UPDATE:
			case DELETE:
			case WITHDRAW:
			case REINSTATE:	
			case ADD:
			case REMOVE:
				this.setModify(true);
				break;
			default : 
				this.setModify(false);
		}
		
		if(context != null && context.getCurrentUser() != null)
		{
			this.setUserId(
					String.valueOf(context.getCurrentUser().getID()));
		}
		this.request = null;
		this.ip = ip;
		this.userAgent = userAgent;
		this.xforwardedfor = xforwardedfor;
		this.context = context;
		this.object = object;
	}

	
	public HttpServletRequest getRequest() {
		return request;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getXforwardedfor() {
		return xforwardedfor;
	}

	public void setXforwardedfor(String xforwardedfor) {
		this.xforwardedfor = xforwardedfor;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public DSpaceObject getObject() {
		return object;
	}

	public void setObject(DSpaceObject object) {
		this.object = object;
	}

	public Action getAction() {
		return this.action;
	}
	
}
