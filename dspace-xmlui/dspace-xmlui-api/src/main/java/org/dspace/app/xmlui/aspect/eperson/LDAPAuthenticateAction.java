/*
 * LDAPAuthenticateAction.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/08/08 20:55:36 $
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Attempt to authenticate the user based upon their presented credentials. This
 * action uses the http parameters of username, ldap_password, and login_realm
 * as credentials.
 * 
 * If the authentication attempt is successfull then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the system
 * before authenticated or if none is supplied back to the DSpace homepage. The
 * action will also return true, thus contents of the action will be excuted.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:act name="LDAPAuthenticate"> <map:serialize type="xml"/> </map:act>
 * <map:transform type="try-to-login-again-transformer">
 * 
 * @author Jay Paz
 */

public class LDAPAuthenticateAction extends AbstractAction {

	/**
	 * Attempt to authenticate the user.
	 */
	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {
		// First check if we are preforming a new login
		Request request = ObjectModelHelper.getRequest(objectModel);

		String username = request.getParameter("username");
		String password = request.getParameter("ldap_password");
		String realm = request.getParameter("login_realm");

		// Skip out of no name or password given.
		if (username == null || password == null)
			return null;
		
		try {
			Context context = AuthenticationUtil.Authenticate(objectModel,username, password, realm);

			EPerson eperson = context.getCurrentUser();

			if (eperson != null) {
				// The user has successfully logged in
				String redirectURL = request.getContextPath();

				if (AuthenticationUtil.isInterupptedRequest(objectModel)) {
					// Resume the request and set the redirect target URL to
					// that of the originaly interrupted request.
					redirectURL += AuthenticationUtil
							.resumeInterruptedRequest(objectModel);
				}

				// Authentication successfull send a redirect.
				final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
						.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

				httpResponse.sendRedirect(redirectURL);

				// log the user out for the rest of this current request,
				// however they will be reauthenticated
				// fully when they come back from the redirect. This prevents
				// caching problems where part of the
				// request is preformed fore the user was authenticated and the
				// other half after it succedded. This
				// way the user is fully authenticated from the start of the
				// request.
				context.setCurrentUser(null);

				return new HashMap();
			}
		} catch (SQLException sqle) {
			throw new PatternException("Unable to preform authentication", sqle);
		}

		return null;
	}

}
