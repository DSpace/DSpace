/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.selection.Selector;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationServiceImpl;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;

/**
 * Selector will count the number of interactive AuthenticationMethods defined in the 
 * dspace configuration file.
 * @author Jay Paz
 * @author Scott Phillips
 *
 */
public class AuthenticationCountSelector implements Selector{

    protected AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    /**
     * Returns true if the expression (in this case a number) is equal to the number
     * of AuthenticationMethods defined in the dspace.cfg file.
     */
	public boolean select(String expression, Map objectModel, Parameters parameters) {
		// get an iterator of all the AuthenticationMethods defined
		final Iterator<AuthenticationMethod> authMethods = authenticationService
		    .authenticationMethodIterator();
		
		  final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
          .get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
  final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
          .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
  
		int authMethodCount = 0;
		
		// iterate to count the methods
		while(authMethods.hasNext()){
			AuthenticationMethod auth = authMethods.next();
			try
            {
                if (auth.loginPageURL(
                        ContextUtil.obtainContext(objectModel), httpRequest,
                        httpResponse) != null){
                    authMethodCount++;
                }
            }
            catch (SQLException e)
            {
                // mmm... we should not never go here, anyway we convert it in an unchecked exception 
                throw new IllegalStateException(e);
            }
		}
		
		final Integer exp = Integer.valueOf(expression);
		
		return (authMethodCount == exp);
	}

	

}
