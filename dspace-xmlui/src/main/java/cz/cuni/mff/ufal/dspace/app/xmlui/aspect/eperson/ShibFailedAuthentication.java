/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.eperson;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.aspect.eperson.FailedAuthentication;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * modified for LINDAT/CLARIN
*/
public class ShibFailedAuthentication extends FailedAuthentication {

	/**language strings */
    public static final Message T_title =
    message("xmlui.EPerson.FailedAuthentication.title");
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    public static final Message T_trail =
        message("xmlui.EPerson.FailedAuthentication.trail");
    
    public static final Message T_h1 =
        message("xmlui.EPerson.FailedAuthentication.h1");
    
	
	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

		HttpServletResponse response = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
		//can't have empty body, sould not be visible, there's a redirect
		body.addDivision("shib_failed_login").addPara("There was an error");
		String redirectTo = ConfigurationManager.getProperty("dspace.url");

		String ourEntityId = ConfigurationManager.getProperty("authentication-shibboleth","spEntityId");

		Object o = context.fromCache(cz.cuni.mff.ufal.Headers.class, 1);
		if ( o != null ) {
			final Map<String, List<String>> headers = ((cz.cuni.mff.ufal.Headers) o).get();
			String idpEntityId = headers.get("shib-identity-provider").get(0);
			String cc = ConfigurationManager.getProperty("feedback.recipient");

			redirectTo = String.format("%s/page/error?idpEntityId=%s&cc=%s&ourEntityId=%s",
					redirectTo, idpEntityId, cc, ourEntityId);

		}
		response.sendRedirect(response.encodeRedirectURL(redirectTo));
	}
}
