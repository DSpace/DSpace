/*
 * X509CertificateServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * X509 certificate authentication servlet. This is an
 * access point for interactive certificate auth that will
 * not be implicit (i.e. not immediately performed
 * because the resource is being accessed via HTTP
 * 
 * @author Robert Tansley
 * @author Mark Diggory
 * @version $Revision$
 */
public class X509CertificateServlet extends DSpaceServlet
{
    /** serialization id */
    private static final long serialVersionUID = -3571151231655696793L;
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(X509CertificateServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Obtain the certificate from the request, if any
        X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");

        if ((certs == null) || (certs.length == 0))
        {
            log.info(LogManager.getHeader(context, "failed_login",
                    "type=x509certificate"));

            JSPManager.showJSP(request, response, "/login/no-valid-cert.jsp");
        }
        else
        {

            Context ctx = UIUtil.obtainContext(request);

            EPerson eperson = ctx.getCurrentUser();

            // Do we have an active e-person now?
            if ((eperson != null) && eperson.canLogIn())
            {
                // Everything OK - they should have already been logged in.
                // resume previous request
                Authenticate.resumeInterruptedRequest(request, response);

                return;
            }

            // If we get to here, no valid cert
            log.info(LogManager.getHeader(context, "failed_login",
                    "type=x509certificate"));
            JSPManager.showJSP(request, response, "/login/no-valid-cert.jsp");
        }
    }
}
