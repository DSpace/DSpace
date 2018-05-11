/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * When only one login method is defined in the dspace.cfg file this class will
 * redirect to the URL provided by that AuthenticationMethod class
 * 
 * @author Jay Paz
 * @author Scott Phillips
 * 
 */
public class LoginRedirect extends AbstractAction {

	protected AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();

	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
			throws Exception {

		final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
				.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
				.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
		final Iterator<AuthenticationMethod> authMethods = authenticationService
				    .authenticationMethodIterator();

        if (authMethods == null)
        {
            throw new IllegalStateException(
                    "No explicit authentication methods found when exactly one was expected.");
        }

		AuthenticationMethod authMethod = null;

        while (authMethods.hasNext())
        {
            AuthenticationMethod currAuthMethod = authMethods.next();
            if (currAuthMethod.loginPageURL(ContextUtil
                    .obtainContext(objectModel), httpRequest, httpResponse) != null)
            {
                if (authMethod != null)
                {
                    throw new IllegalStateException(
                            "Multiple explicit authentication methods found when only one was expected.");
                }
                authMethod = currAuthMethod;
            }
        }

        final String url = ((AuthenticationMethod) authMethod).loginPageURL(
                ContextUtil.obtainContext(objectModel), httpRequest,
                httpResponse);

	      
		// now we want to check for the force ssl property
		if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.force.ssl")) {

			if (!httpRequest.isSecure()) {
				StringBuffer location = new StringBuffer("https://");
				location.append(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.hostname")).append(url).append(
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
