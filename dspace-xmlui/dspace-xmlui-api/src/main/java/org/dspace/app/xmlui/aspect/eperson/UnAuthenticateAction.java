/*
 * UnAuthenticatedAction.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/04/06 15:15:59 $
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

package org.dspace.app.xmlui.aspect.eperson;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Unauthenticate the current user. There is no way this action will fail, 
 * so any components inside the action will be exceuted.
 * 
 * This aciton will always send an HTTP redirect to the DSpace homepage.
 * 
 * Example: 
 * 
 * <map:action name="UnAuthenticateAction" src="org.dspace.app.xmlui.eperson.UnAuthenticateAction"/>
 * 
 * <map:act type="UnAuthenticateAction">
 *   <map:serialize type="xml"/>
 * </map:act>
 * 
 * @author Scott Phillips
 */

public class UnAuthenticateAction extends AbstractAction
{

    /**
     * Logout the current user.
     * 
     * @param pattern
     *            un-used.
     * @param objectModel
     *            Cocoon's object model
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        
        Context context = ContextUtil.obtainContext(objectModel);
        final HttpServletRequest httpRequest = 
            (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        final HttpServletResponse httpResponse = 
            (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

        EPerson eperson = context.getCurrentUser();
        
        // Actualy log the user out.
        AuthenticationUtil.loggedOut(context,httpRequest);
        
        // Set the user as logged in for the rest of this request so that the cache does not get spoiled.
        context.setCurrentUser(eperson);
        
        // Forward the user to the home page.
        if((ConfigurationManager.getBooleanProperty("xmlui.public.logout")) && (httpRequest.isSecure())) {
				StringBuffer location = new StringBuffer("http://");
				location.append(ConfigurationManager.getProperty("dspace.hostname")).append(
						httpRequest.getContextPath());
				httpResponse.sendRedirect(location.toString());
			
		}
        else{
        	httpResponse.sendRedirect(httpRequest.getContextPath());
        }
        
        return new HashMap();
    }

}
