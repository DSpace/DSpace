/*
 * PasswordServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;


/**
 * Simple username and password authentication servlet.  Displays the
 * login form <code>/login/password.jsp</code> on a GET,
 * otherwise process the parameters as an email and password.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class PasswordServlet extends DSpaceServlet
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(PasswordServlet.class);

    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Simply forward to the plain form
        JSPManager.showJSP(request, response, "/login/password.jsp");
    }
    
    
    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Process the POSTed email and password
        String email = request.getParameter("login_email");
        String password = request.getParameter("login_password");
        
        // Locate the eperson
        EPerson eperson = EPerson.findByEmail(context, email);
        boolean loggedIn = false;

        // Verify the password
        if (eperson != null && eperson.getActive())
        {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate())
            {
                // they must use a certificate
                JSPManager.showJSP(request,
                    response,
                    "/error/require-certificate.jsp");
                return;
            }
            else if (eperson.checkPassword(password))
            {
                // Logged in OK.
                Authenticate.loggedIn(context, request, eperson);

		log.info(LogManager.getHeader(context,
                    "login",
                    "type=password"));
                
                // resume previous request
                Authenticate.resumeInterruptedRequest(request, response);
                
                return;
            }
        }

        // If we reach here, supplied email/password was duff.
        log.info(LogManager.getHeader(context,
            "failed_login",
            "email=" + email));
        JSPManager.showJSP(request, response, "/login/incorrect.jsp");
    }
}
