/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * This action will start the necessary steps to authenticate a user. After the user 
 * successfully authenticates, the user will resume this request with all parameters
 * and attributes intact. An optional message can be added that will be displayed
 * on the login form. This could be used to provide a reason why the user is being
 * queried for a user name and password.
 *
 * <p>Possible parameters are:
 *
 * <ul>
 *  <li>header: An i18n message that will be used as the header for the message.
 *  <li>message: An i18n message tag.
 *  <li>characters: Characters to be displayed, possibly for untranslated error messages
 * </ul>
 *
 * <p>Example:
 * <pre>
 * {@code
 * <map:action name="StartAuthenticationAction" src="org.dspace.app.xmlui.eperson.StartAuthenticationAction"/>
 * 
 * <map:act type="StartAuthenticationAction"/>
 * }
 * </pre>
 * 
 * Typically, this is used in conjunction with the AuthenticatedSelector as:
 *
 * <pre>
 * {@code
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
 * }
 * </pre>
 * 
 * @author Scott Phillips
 */

public class StartAuthenticationAction extends AbstractAction
{
    /**
     * Redirect the user to the login page.
     * @param redirector redirector.
     * @param resolver source resolver.
     * @param objectModel object model.
     * @param source source.
     * @param parameters sitemap parameters.
     * @return result of the action.
     * @throws java.lang.Exception passed through.
     */
    @Override
    public Map act(Redirector redirector, SourceResolver resolver,
            Map objectModel, String source, Parameters parameters)
            throws Exception
    {
    	
    	String header = parameters.getParameter("header",null);
    	String message = parameters.getParameter("message",null);
    	String characters = parameters.getParameter("characters",null);
    	
    	// Interrupt this request
    	AuthenticationUtil.interruptRequest(objectModel,header,message,characters);

    	final HttpServletRequest  httpRequest  = (HttpServletRequest)  objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT); 
    	final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT); 
    	
    	httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");

        return new HashMap();
    }

}
