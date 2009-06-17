/*
 * ShibbolethServlet.java
 *
 * Version: $Revision: 3705 $
 * 
 * Copyright (c) 2009, The DSpace Foundation.  All rights reserved.
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

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.eperson.Group;

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
 * @version $Revision: 3705 $
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
        while(names.hasMoreElements()) log.info("header:"+(name=names.nextElement().toString())+"="+request.getHeader(name));
        
        String jsp = null;
        
        // Locate the eperson
        int status = AuthenticationManager.authenticate(context, null, null, null, request);
        
        if (status == AuthenticationMethod.SUCCESS){
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

