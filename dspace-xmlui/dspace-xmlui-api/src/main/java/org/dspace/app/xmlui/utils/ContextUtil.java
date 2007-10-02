/*
 * ContextUtil.java
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2006/04/06 15:14:39 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.core.Context;

/**
 * Miscellaneous UI utility methods methods for managing DSpace context.
 * 
 * This class was "adapted" from the UIUtil.java in the DSpace webui.
 * 
 * @author Robert Tansley
 * @author Scott Phillips
 */
public class ContextUtil
{

    private static final Logger log = Logger.getLogger(ContextUtil.class);
    
    /** Where the context is stored on an HTTP Request object */
    public final static String DSPACE_CONTEXT = "dspace.context";

    /**
     * Obtain a new context object. If a context object has already been created
     * for this HTTP request, it is re-used, otherwise it is created.
     * 
     * @param objectModel
     *            the cocoon Objectmodel
     * 
     * @return a context object
     */
    public static Context obtainContext(Map objectModel) throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

        if (context == null)
        {
            // No context for this request yet
            context = new Context();

            // Set the session ID
            context.setExtraLogInfo("session_id="
                    + request.getSession().getId());

            // Check if we've all ready been authenticated.
            final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
                    .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
            AuthenticationUtil.resumeLogin(context, httpRequest);

            // Set any special groups - invoke the authentication mgr.
            int[] groupIDs = AuthenticationManager.getSpecialGroups(context, httpRequest);

            for (int i = 0; i < groupIDs.length; i++)
            {
                context.setSpecialGroup(groupIDs[i]);
                log.debug("Adding Special Group id="+String.valueOf(groupIDs[i]));
            }

            // Set the session ID and IP address
            context.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + request.getRemoteAddr());

            // Store the context in the request
            request.setAttribute(DSPACE_CONTEXT, context);
        }

        return context;
    }

    /**
     * Check if a context exists for this request, if so complete the context.
     * 
     * @param request
     *            The request object 
     */
    public static void closeContext(HttpServletRequest request) throws ServletException
    {
    	Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

    	if (context != null && context.isValid())
    	{
    		try {
    			context.complete();
    		} catch (SQLException sqle) {
    			throw new ServletException("Unable to close DSpace context.",sqle);
    		}
    	}

    }

}
