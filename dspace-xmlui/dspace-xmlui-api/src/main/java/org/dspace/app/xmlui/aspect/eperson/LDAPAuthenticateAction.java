/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Attempt to authenticate the user based upon their presented credentials. This
 * action uses the http parameters of username, ldap_password, and login_realm
 * as credentials.
 * 
 * If the authentication attempt is successful then an HTTP redirect will be
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
		// First check if we are performing a new login
		Request request = ObjectModelHelper.getRequest(objectModel);

		String username = request.getParameter("username");
		String password = request.getParameter("ldap_password");
		String realm = request.getParameter("login_realm");

		// Skip out of no name or password given.
		if (username == null || password == null)
        {
            return null;
        }
		
		try {
			Context context = AuthenticationUtil.authenticate(objectModel,username, password, realm);

			EPerson eperson = context.getCurrentUser();

			if (eperson != null) {
				// The user has successfully logged in
				String redirectURL = request.getContextPath();

				if (AuthenticationUtil.isInterupptedRequest(objectModel)) {
					// Resume the request and set the redirect target URL to
					// that of the originally interrupted request.
					redirectURL += AuthenticationUtil
							.resumeInterruptedRequest(objectModel);
				}
                                else
                                {
                                        // Otherwise direct the user to the specified 'loginredirect' page (or homepage by default)
                                        String loginRedirect = ConfigurationManager.getProperty("xmlui.user.loginredirect");
                                        redirectURL += (loginRedirect != null) ? loginRedirect.trim() : "/";	
                                }

				// Authentication successful send a redirect.
				final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
						.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

				httpResponse.sendRedirect(redirectURL);

				// log the user out for the rest of this current request,
				// however they will be reauthenticated
				// fully when they come back from the redirect. This prevents
				// caching problems where part of the
				// request is performed fore the user was authenticated and the
				// other half after it succeeded. This
				// way the user is fully authenticated from the start of the
				// request.
				context.setCurrentUser(null);

				return new HashMap();
			}
		} catch (SQLException sqle) {
			throw new PatternException("Unable to perform authentication", sqle);
		}

		return null;
	}

}
