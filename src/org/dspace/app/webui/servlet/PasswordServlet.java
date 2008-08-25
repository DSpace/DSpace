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
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.naming.NamingException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.servlet.RegisterServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

import edu.yale.its.tp.cas.client.ProxyTicketValidator;

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
    /** log4j logger */
    private static Logger log = Logger.getLogger(PasswordServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
	String checkcas = request.getParameter("check-cas");

	if (checkcas != null) {
	    // Checking for existing CAS login

	    if (checkcas.equals("start")) {
		// Callout to CAS, return here with check-cas=done
		String returnurl = 
		    ConfigurationManager.getProperty("dspace.url")
		    + "/password-login"
		    + "?check-cas=done"
		    ;

		String url = 
		    ConfigurationManager.getProperty("cas.loginUrl")
		    + "?service="
		    + URLEncoder.encode(returnurl, Constants.DEFAULT_ENCODING)
		    + "&gateway=true"
		    ;

		response.sendRedirect(response.encodeRedirectURL(url));

	    } else if (checkcas.equals("done")) {
		// Back from CAS

		String ticket = request.getParameter("ticket");

		if (ticket == null || ticket.equals("")) {
		    // The user is not already logged in to CAS

		    // Give the user an option on how to login
		    JSPManager.showJSP(request, response, "/login/password.jsp");
		} else {
		    // The user is logged in to CAS
		    String strCasUser = getCASAuthenticatedUser(request, ticket);
		    doUmLogin(context, request, response, strCasUser);
		}
	    }
	    else {
		throw new ServletException("Invalid value for checkcas parameter");
	    }
	}
    }


    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Process the POSTed information
        String email = request.getParameter("login_email");
        String password = request.getParameter("login_password");

	if (email != null) {
	    doLocalLogin(context, request, response, email, password);
	} else {
	    // Callout to CAS, return here with check-cas=done
	    String returnurl = 
		ConfigurationManager.getProperty("dspace.url")
		+ "/password-login"
		+ "?check-cas=done"
		;

	    String url = 
		ConfigurationManager.getProperty("cas.loginUrl")
		+ "?service="
		+ URLEncoder.encode(returnurl, Constants.DEFAULT_ENCODING)
		;

	    response.sendRedirect(response.encodeRedirectURL(url));

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
			     String strCasUser)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
	HttpSession session = request.getSession();

	Ldap ldap = null;
	try {
	    // check for existing CAS user
	    //	    String strCasUser = (String)session.getAttribute(CASFilter.CAS_FILTER_USER);
	    String uid = strCasUser;

	    // check for valid uid
	    ldap = new Ldap(context);
	    if (ldap.checkUid(uid)) {
		ldap.close();

		String email = ldap.getEmail();
		if (email == null) {
		  email = uid + "@umd.edu";
		}

		// locate the eperson
		EPerson eperson = EPerson.findByEmail(context, email.toLowerCase());

		if (eperson == null) {
		    try {
		      eperson = ldap.registerEPerson(email);
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
	    if (ldap != null) {
		ldap.close();
	    }
	}
    }


    /**
     * Converts a ticket parameter to a username, taking into account an
     * optionally configured trusted proxy in the tier immediately in front
     * of us.  Adapted from edu.yale.its.tp.cas.client.filter.CASFilter .
     */
    public static String getCASAuthenticatedUser(HttpServletRequest request, String ticket) throws ServletException {
        ProxyTicketValidator pv = null;
        try {
            pv = new ProxyTicketValidator();

            pv.setCasValidateUrl(ConfigurationManager.getProperty("cas.validateUrl"));
            pv.setServiceTicket(ticket);
            pv.setService(ConfigurationManager.getProperty("dspace.url")
			  + "/password-login"
			  + "?check-cas=done"
			  );
	    pv.setRenew(false);

            pv.validate();

            if (!pv.isAuthenticationSuccesful()) {
                throw new ServletException(
                    "CAS authentication error: " + pv.getErrorCode() + ": " + pv.getErrorMessage());
	    }

            if (pv.getProxyList().size() != 0) {
                // ticket was proxied
		throw new ServletException("this page does not accept proxied tickets");
            }

            return pv.getUser();

        } catch (SAXException ex) {
            String xmlResponse = "";
            if (pv != null)
                xmlResponse = pv.getResponse();
            throw new ServletException(ex + " " + xmlResponse);

        } catch (ParserConfigurationException ex) {
            throw new ServletException(ex);

        } catch (IOException ex) {
            throw new ServletException(ex);
        }
    }

}
