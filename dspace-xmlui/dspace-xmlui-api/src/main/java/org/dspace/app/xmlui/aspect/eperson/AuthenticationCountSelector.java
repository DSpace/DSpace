/*
 * AuthenticationCountSelector.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
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
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.selection.Selector;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;

/**
 * Selector will count the number of interactive AuthenticationMethods defined in the 
 * dspace configuration file
 * @author Jay Paz
 * @author Scott Phillips
 *
 */
public class AuthenticationCountSelector implements Selector{
    /**
     * Returns true if the expression (in this case a number) is equal to the number
     * of AuthenticationMethods defined in the dspace.cfg file
     * @return
     */
	public boolean select(String expression, Map objectModel, Parameters parameters) {
		// get an iterator of all the AuthenticationMethods defined
		final Iterator<AuthenticationMethod> authMethods = (Iterator<AuthenticationMethod>) AuthenticationManager
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
                throw new RuntimeException(e);
            }
		}
		
		final Integer exp = new Integer(expression);
		
		return (authMethodCount == exp);
	}

	

}
