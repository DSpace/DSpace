/*
 * X509CertificateServlet.java
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.X509Manager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;


/**
 * X509 certificate authentication servlet.  Attempts to obtain a certificate
 * from the user.
 * <ul>
 * <li>If the certificate is valid, and corresponds to an eperson in the DB,
 *     the user is logged in.</li>
 * <li>If the certificate is valid, but there is no corresponding eperson in
 *     the DB, an error message is displayed explaining this.</li>
 * <li>If there is a certificate, but it is valid, the email/password form is
 *     displayed with a suitable message.</li>
 * <li>If there's no certificate, the email/password form is displayed with
 *     a suitable message.<li>
 * </ul>
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class X509CertificateServlet extends DSpaceServlet
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(X509CertificateServlet.class);


    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Obtain the certificate from the request, if any
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(
            "javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0)
        {
            // No certificate - show login form
            
            // Was this reached by clicking on the "I have a cert" link on the
            // password form?
            if (request.getParameter("from_form") != null)
            {
                // If from "I have cert" link, display the login form again
                // with a suitable message
		log.info(LogManager.getHeader(context,
                    "failed_login",
                    "type=x509certificate"));

                JSPManager.showJSP(request, response,
                    "/login/no-valid-cert.jsp");
            }
            else
            {
                // We came here straight from an authentication invocation
                // Just display the login form which gives cert + password
                // options.
                JSPManager.showJSP(request, response,
                    "/login/password.jsp");
            }
        }
        else
        {
            // We have a cert
            try
            {
                if (X509Manager.isValid(certs[0]))
                {
                    // And it's valid - try and get an e-person
                    EPerson eperson = X509Manager.getUser(context, certs[0]);

                    // Do we have an e-person?
                    if (eperson != null && eperson.getActive())
                    {
                        // Everything OK - log them in.
                        Authenticate.loggedIn(context, request, eperson);

		        log.info(LogManager.getHeader(context,
                            "login",
                            "type=x509certificate"));

                        // resume previous request
                        Authenticate.resumeInterruptedRequest(request,
                            response);

                        return;
                    }
                    else
                    {
                        // Valid certificate, but no e-person record
		        log.info(LogManager.getHeader(context,
                            "failed_login",
                            "type=cert_but_no_record"));
                        JSPManager.showJSP(request, response,
                            "/login/not-in-records.jsp");
                    }
                }
                else
                {
                    // Not a valid cert
		    log.info(LogManager.getHeader(context,
                        "failed_login",
                        "type=x509certificate"));
                    JSPManager.showJSP(request, response,
                        "/login/no-valid-cert.jsp");
                }
            }
            catch (CertificateException ce)
            {
                log.warn(LogManager.getHeader(context,
                    "certificate_exception",
                    ""),
                    ce);
                JSPManager.showInternalError(request, response);
            }
        }
    }
}
