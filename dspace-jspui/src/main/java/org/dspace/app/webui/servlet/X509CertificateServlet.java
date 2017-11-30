/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
