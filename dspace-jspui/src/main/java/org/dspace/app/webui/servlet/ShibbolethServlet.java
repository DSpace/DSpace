/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;

/**
 * Shibbolize dspace. Follow instruction at 
 * http://mams.melcoe.mq.edu.au/zope/mams/pubs/Installation/dspace15
 *
 * Pull information from the header as released by Shibboleth target.
 * The header required are:
 * <ol><li>user email</li>
 * <li>first name (optional)</li>
 * <li>last name (optional)</li>
 * <li>user roles</li>
 * </ol>.
 *
 * All these info are configurable from the configuration file (dspace.cfg).
 *
 * @author  <a href="mailto:bliong@melcoe.mq.edu.au">Bruc Liong, MELCOE</a>
 * @author  <a href="mailto:kli@melcoe.mq.edu.au">Xiang Kevin Li, MELCOE</a>
 * @version $Revision$
 */
public class ShibbolethServlet extends DSpaceServlet {
    /** log4j logger */
    private static Logger log = Logger.getLogger(ShibbolethServlet.class);
    
    protected void doDSGet(Context context,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException, SQLException, AuthorizeException {
        //debugging, show all headers
        java.util.Enumeration names = request.getHeaderNames();
        String name;
        while(names.hasMoreElements())
        {
            name = names.nextElement().toString();
            log.info("header:" + name + "=" + request.getHeader(name));
        }
        
        String jsp = null;
        
        // Locate the eperson
        int status = AuthenticationManager.authenticate(context, null, null, null, request);
        
        if (status == AuthenticationMethod.SUCCESS){
            try {
                // the AuthenticationManager updates the last_active field of the
                // eperson that logged in. We need to commit the context, to store
                // the updated field in the database.
                context.commit();
            } catch (SQLException ex) {
                // We can log the SQLException, but we should not interrupt the 
                // users interaction here.
                log.error("Failed to write an updated last_active field of an "
                        + "EPerson into the databse.", ex);
            }

            // Logged in OK.
            Authenticate.loggedIn(context, request, context.getCurrentUser());
            
            log.info(LogManager.getHeader(context, "login", "type=shibboleth"));
            
            // resume previous request
            Authenticate.resumeInterruptedRequest(request, response);
            
            return;
        }else if (status == AuthenticationMethod.CERT_REQUIRED){
            jsp = "/error/require-certificate.jsp";
        }else if(status == AuthenticationMethod.NO_SUCH_USER){
            jsp = "/login/no-single-sign-out.jsp";
        }else if(status == AuthenticationMethod.BAD_ARGS){
            jsp = "/login/no-email.jsp";
        }
        
        // If we reach here, supplied email/password was duff.
        log.info(LogManager.getHeader(context, "failed_login","result="+String.valueOf(status)));
        JSPManager.showJSP(request, response, jsp);
        return;
    }
}

