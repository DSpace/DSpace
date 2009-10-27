/*
 * AdminOnlyFilter.java
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
package org.dspace.app.webui.filter;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * DSpace filter that only allows requests from authenticated administrators to
 * proceed. Anonymous requests prompt the authentication procedure. Requests
 * from authenticated non-admins result in an authorisation error.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class AdminOnlyFilter implements Filter
{
    /** log4j category */
    private static Logger log = Logger.getLogger(RegisteredOnlyFilter.class);

    public void init(FilterConfig config)
    {
        // Do nothing
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws ServletException, IOException
    {
        Context context = null;

        // We need HTTP request objects
        HttpServletRequest hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = (HttpServletResponse) response;

        try
        {
            // Obtain a context
            context = UIUtil.obtainContext(hrequest);

            // Continue if logged in or startAuthentication finds a user;
            // otherwise it will issue redirect so just return.
            if (context.getCurrentUser() != null ||
                Authenticate.startAuthentication(context, hrequest, hresponse))
            {
                // User is authenticated
                if (AuthorizeManager.isAdmin(context))
                {
                    // User is an admin, allow request to proceed
                    chain.doFilter(hrequest, hresponse);
                }
                else
                {
                    // User is not an admin
                    log.info(LogManager.getHeader(context, "admin_only", ""));
                    JSPManager.showAuthorizeError(hrequest, hresponse, null);
                }
            }
        }
        catch (SQLException se)
        {
            log.warn(LogManager.getHeader(context, "database_error", se
                    .toString()), se);
            JSPManager.showInternalError(hrequest, hresponse);
        }

        // Abort the context if it's still valid
        if ((context != null) && context.isValid())
        {
            context.abort();
        }
    }

    public void destroy()
    {
        // Nothing
    }
}
