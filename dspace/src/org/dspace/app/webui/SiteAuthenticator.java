/*
 * SiteAuthenticator.java
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

package org.dspace.app.webui;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.core.Context;


/**
 * Interface allowing DSpace instances to provide custom authentication
 * procedures.  Each site can either use the default implementation to allow
 * simple password authentication, or provide a custom implementation,
 * specifying the class name of that implementation in <code>dspace.cfg</code>.
 *
 * @author  Robert Tansley
 * @version $Id$
 */
public interface SiteAuthenticator
{
    /**
     * Start the authentication process.  <code>request</code> is the original
     * request that led to the authentication process being invoked.  The
     * data in the request has already been stored in the session when this
     * is invoked, so this method can either redirect the user to a login
     * screen, or if some other form of authentication is used, the user in
     * the context can be set, and 
     * <code>org.dspace.app.webui.util.UIUtil.resumeOriginalRequest<code> can
     * be invoked to continue the original operation.
     *
     * @param context   current DSpace context object
     * @param request   the request leading up to authentication being required
     * @param response  the associated HTTP response
     */
    public void startAuthentication(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException;


    /**
     * Register a new user in the system.  This is invoked when a user enters
     * an e-mail address into the "register with DSpace" box.  This 
    
    
    /**
     * Work out if the current user is implicitly a member of any groups.
     * This may include checking an IP address etc.
     *
     * @param context   current DSpace context object
     * @param request   the request leading up to authentication being required
     *
     * @return  the IDs of groups the user is implicitly in
     */
    public int[] getSpecialGroups(Context context,
        HttpServletRequest request)
        throws SQLException;
}
