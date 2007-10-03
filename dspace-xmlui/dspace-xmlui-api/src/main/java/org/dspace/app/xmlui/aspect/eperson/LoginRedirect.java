/*
 * LoginRedirect.java
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
package org.dspace.app.xmlui.aspect.eperson;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.core.ConfigurationManager;

/**
 * When only one login method is defined in the dspace.cfg file this class will
 * redirect to the URL provided by that AuthenticationMethod class
 * 
 * @author Jay Paz
 * @author Scott Phillips
 * 
 */
public class LoginRedirect extends AbstractAction {

	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {

		final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
				.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
				.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
		final Iterator authMethods = AuthenticationManager
				.authenticationMethodIterator();

		final String url = ((AuthenticationMethod) authMethods.next())
				.loginPageURL(ContextUtil.obtainContext(objectModel),
						httpRequest, httpResponse);

		if (authMethods.hasNext()) {
			throw new IllegalStateException(
					"Multiple authentication methods found when only one was expected.");
		}

		// now we want to check for the force ssl property
		if (ConfigurationManager.getBooleanProperty("xmlui.force.ssl")) {

			if (!httpRequest.isSecure()) {
				StringBuffer location = new StringBuffer("https://");
				location.append(ConfigurationManager.getProperty("dspace.hostname")).append(url).append(
						httpRequest.getQueryString() == null ? ""
								: ("?" + httpRequest.getQueryString()));
				httpResponse.sendRedirect(location.toString());
			} else {
				httpResponse.sendRedirect(url);
			}
		} else {
			httpResponse.sendRedirect(url);
		}

		return new HashMap();
	}

}
