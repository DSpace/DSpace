/*
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
	
	private Context context;
	
	private DSpaceObject object;

	private Action action;
			
	private static String checkParams(Action action, HttpServletRequest request, Context context, DSpaceObject object)
	{
		if(action == null)
			throw new RuntimeException("action cannot be null");
			
		if(request == null)
			throw new RuntimeException("request cannot be null");
		

		if(context == null)
			throw new RuntimeException("context cannot be null");
		
		if(object == null)
			throw new RuntimeException("object cannot be null");
		
		try
		{
			String objText = Constants.typeText[object.getType()].toLowerCase();
			return  objText + ":" + action.text();
		}catch(Exception e)
		{
			
		}
		return "";
		
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

	public HttpServletRequest getRequest() {
		return request;
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
