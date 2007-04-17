/*
 * PasswordServlet.java
 *
 * Version: $Revision: 1.7 $
 *
 * Date: $Date: 2004/12/22 17:48:35 $
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
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.servlet.RegisterServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

import edu.umd.lims.dspace.Ldap;

  
/**
 * Simple username and password authentication servlet. Displays the login form
 * <code>/login/password.jsp</code> on a GET, otherwise process the parameters
 * as an email and password.
 * 
 * @author Robert Tansley
 * @version $Revision: 1.7 $
 */
public class PasswordServlet extends DSpaceServlet
{
    /**
     * <pre>
     * Revision History:
     *
     *   2005/01/21: Ben
     *     - additional logging if unable to create EPerson
     *     - handle more missing ldap entries
     *
     *   2005/01/19: Ben
     *     - handle ldap entries with no 'mail' attribute
     *
     *   2004/12/22: Ben
     *     - save the entire ldap object in the session
     *     - close the ldap connection sooner, it can be closed
     *       and still have the ldap entry available
     *
     *   2004/12/20: Ben
     *     - save ou ldap information in the session
     *
     *   2004/12/17: Ben
     *     - add email notification for new ldap users registered
     *
     *   2004/10/25: Ben
     *     - initial version
     *
     * </pre>
     */

    /** log4j logger */
    private static Logger log = Logger.getLogger(PasswordServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Simply forward to the plain form
        JSPManager.showJSP(request, response, "/login/password.jsp");
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Process the POSTed information
        String email = request.getParameter("login_email");
 	String uid = request.getParameter("login_uid");
         String password = request.getParameter("login_password");


	if (email != null) {
	    doLocalLogin(context, request, response, email, password);
	} else if (uid != null) {
	    doUmLogin(context, request, response, uid, password);
	} else {
	    // should not reach here
	    log.info(LogManager.getHeader(context,
					  "failed_login",
					  "no login provided"));
	    JSPManager.showJSP(request, response, "/login/password.jsp");
	}
    }


    protected void doLocalLogin(Context context,
        HttpServletRequest request,
	HttpServletResponse response, 
	String email,
	String password)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Locate the eperson
        EPerson eperson = EPerson.findByEmail(context, email.toLowerCase());
        boolean loggedIn = false;

        // Verify the password
        if ((eperson != null) && eperson.canLogIn())
        {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate())
            {
                // they must use a certificate
                JSPManager.showJSP(request, response,
                        "/error/require-certificate.jsp");

                return;
            }
            else if (eperson.checkPassword(password))
            {
                // Logged in OK.
                Authenticate.loggedIn(context, request, eperson);

                log.info(LogManager
                        .getHeader(context, "login", "type=password"));

                // resume previous request
                Authenticate.resumeInterruptedRequest(request, response);

                return;
            }
        }

        // If we reach here, supplied email/password was duff.
        log.info(LogManager
                .getHeader(context, "failed_login", "email=" + email));
        JSPManager.showJSP(request, response, "/login/incorrect.jsp");
    }


    protected void doUmLogin(Context context,
        HttpServletRequest request,
	HttpServletResponse response, 
	String uid,
	String password)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
	HttpSession session = request.getSession();

	Ldap ldap = null;
	try {
	    // check for valid uid/password
	    ldap = new Ldap(context);
	    if (ldap.checkUid(uid) && ldap.checkPassword(password)) {
		ldap.close();

		String email = ldap.getEmail();
		if (email == null) {
		  email = uid + "@umd.edu";
		}

		// locate the eperson
		EPerson eperson = EPerson.findByEmail(context, email.toLowerCase());

		if (eperson == null) {
		    try {
			// Use the admin account to create the eperson
			EPerson admin = EPerson.findByEmail(context, "ldap_um@drum.umd.edu");
			context.setCurrentUser(admin);

			// Create a new eperson
			eperson = EPerson.create(context);
			
			String strFirstName = ldap.getFirstName();
			if (strFirstName == null)
			  strFirstName = "??";

			String strLastName = ldap.getLastName();
			if (strLastName == null)
			  strLastName = "??";

			String strPhone = ldap.getPhone();
			if (strPhone == null)
			  strPhone = "??";

			eperson.setEmail(email);
			eperson.setFirstName(strFirstName);
			eperson.setLastName(strLastName);
			eperson.setMetadata("phone", strPhone);
			eperson.setCanLogIn(false);
			eperson.setRequireCertificate(false);

			eperson.update();
			context.commit();
			
			log.info(LogManager.getHeader(context,
						      "create_um_eperson",
						      "eperson_id="+eperson.getID() +
						      ", uid=" + uid));

			// Send an email that the user has registered
			RegisterServlet.notifyRegistration(context, eperson, "ldap auto registered");
		    }
		    catch (Exception e) {
			log.info(LogManager.getHeader(context,
						      "failed_login",
						      "Unable to create new eperson for um uid="+uid+": " + e.getMessage()));
			throw new ServletException("Error creating new eperson", e);
		    }
		    finally {
			context.setCurrentUser(null);
		    }			
		}

		
		// Logged in OK.
		Authenticate.loggedIn(context, request, eperson);
			
		log.info(LogManager.getHeader(context,
                    "login",
                    "type=um id"));
                
		// Save the ldap object in the session
		session.setAttribute("dspace.current.user.ldap", ldap);

		// resume previous request
		Authenticate.resumeInterruptedRequest(request, response);
		
		return;
	    }

	    // If we reach here, supplied uid/password was duff.
	    log.info(LogManager.getHeader(context,
					  "failed_login",
					  "uid=" + uid));
	    JSPManager.showJSP(request, response, "/login/incorrect-um.jsp");
        }

	catch (NamingException ne) {
	    log.info(LogManager.getHeader(context,
					  "failed_login",
					  "ldap NamingException error:\n" + ne.getMessage()));
	    JSPManager.showJSP(request, response, "/login/password.jsp");
	}

	finally {
	    ldap.close();
	}
    }
}
