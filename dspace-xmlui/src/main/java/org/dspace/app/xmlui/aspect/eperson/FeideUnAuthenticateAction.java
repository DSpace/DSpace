/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Unauthenticate the current user. There is no way this action will fail, 
 * so any components inside the action will be executed.
 *
 * This action will always send an HTTP redirect to the DSpace homepage.
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

public class FeideUnAuthenticateAction extends AbstractAction {

    /**
     * Logout the current user.
     *
     * @param redirector
     * @param resolver
     * @param objectModel
     *            Cocoon's object model
     * @param source
     * @param parameters
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
                   String source, Parameters parameters) throws Exception {

        Context context = ContextUtil.obtainContext(objectModel);
        final HttpServletRequest httpRequest =
                (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        final HttpServletResponse httpResponse =
                (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

        EPerson eperson = context.getCurrentUser();

        // Actually log the user out.
        AuthenticationUtil.logOut(context, httpRequest);

        // Set the user as logged in for the rest of this request so that the cache does not get spoiled.
        context.setCurrentUser(eperson);

        String logoutPath = httpRequest.getContextPath() + "/Endpoint/SingleLogoutService/Redirect/Request?RelayState=";

        // Forward the user to the home page.
        httpResponse.sendRedirect(httpResponse.encodeRedirectURL(logoutPath + this.getBasePath(httpRequest) + httpRequest.getContextPath()));


        return new HashMap();
    }

    private String getBasePath(HttpServletRequest request) {
        StringBuilder basePath = new StringBuilder();

        basePath.append(request.getScheme());
        basePath.append("://");
        basePath.append(request.getServerName());
        if ("http".equals(request.getScheme()) && request.getServerPort() != 80 ||
                "https".equals(request.getScheme()) && request.getServerPort() != 443) {
            basePath.append(":").append(request.getServerPort());
        }
        return basePath.toString();
    }

}
