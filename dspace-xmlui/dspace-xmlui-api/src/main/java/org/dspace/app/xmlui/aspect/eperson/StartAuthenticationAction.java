/*
 * StartAuthenticationAction.java
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

/**
 * 
 * This action will start the nessary steps to authenticate a user. After the user 
 * successfully authenticates the user will resume this request with all parameters
 * and attributes intact. An optional message can be added that will be displayed
 * on the login form. This could be used to provide a reason why the user is being
 * queried for a user name and password.
 * 
 * Possible parameter are:
 * 
 * header: An i18n message that will be used as the header for the message.
 * 
 * message: An i18n message tag.
 * 
 * characters: Characters to be displayed, possibly for untranslated error messages
 * 
 * 
 * <map:action name="StartAuthenticationAction" src="org.dspace.app.xmlui.eperson.StartAuthenticationAction"/>
 * 
 * 
 * <map:act type="StartAuthenticationAction"/>
 * 
 * 
 * Typicaly this is used in conjunction with the AuthenticatedSelector as:
 * 
 * <map:select type="AuthenticatedSelector">
 *   <map:when test="eperson">
 *     ...
 *   </map:when>
 *   <map:otherwise>
 *     <map:act type="startAuthenticationAction">
 *       <map:parameter name="message" value="xmlui.Aspect.component.tag"/>
 *     </map:act>
 *   </map:otherwise>
 * </map:select>
 * 
 * @author Scott Phillips
 */

public class StartAuthenticationAction extends AbstractAction
{
    /**
     * Redirect the user to the login page.
     */
    public Map act(Redirector redirector, SourceResolver resolver,
            Map objectModel, String source, Parameters parameters)
            throws Exception
    {
    	
    	String header = parameters.getParameter("header",null);
    	String message = parameters.getParameter("message",null);
    	String characters = parameters.getParameter("characters",null);
    	
    	// Interupt this request
    	AuthenticationUtil.interruptRequest(objectModel,header,message,characters);

    	final HttpServletRequest  httpRequest  = (HttpServletRequest)  objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT); 
    	final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT); 
    	
    	httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");

        return new HashMap();
    }

}
