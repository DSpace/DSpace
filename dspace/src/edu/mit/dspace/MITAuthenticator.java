/*
 * MITAuthenticator.java
 *
 * $Id$
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

package edu.mit.dspace;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.SiteAuthenticator;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * MIT implementation of DSpace Web UI authentication.  This version detects
 * whether the user is an MIT user, and if so, the user is redirected to the
 * certificate login page.  Otherwise, the email/password page is used
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class MITAuthenticator implements SiteAuthenticator
{
    public void startAuthentication(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        if (isMITUser(request))
        {
            try
            {
                // add the user to the special group "MIT Users"
                Group MITGroup = Group.findByName(context, "MIT Users");

                if( MITGroup != null )
                {
                    context.setSpecialGroup( MITGroup.getID() );
                }
            }
            catch(SQLException e)
            {
                // FIXME: quietly fail if we caught SQLException 
            }
        
            // Try and get a certificate by default
            response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/certificate-login"));
        }
        else
        {
            // Present the username/password screen (with cert option)
            response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/password-login"));
        }
    }


    /**
     * Check to see if the user is an MIT user.  At present, it just
     * checks the source IP address. Note this is independent of user
     * authentication - if the user is an off-site MIT user, this will
     * still return false.
     *
     * @param request current request
     *
     * @return  true if the user is an MIT user.
     */
    public static boolean isMITUser(HttpServletRequest request)
    {
        String addr = request.getRemoteAddr();

        final String[] mitIPs =
        {
            "18.",
            "128.52.",       // AI
            "129.55.",       // Lincoln
            "192.52.65.",    // Haystack
            "192.52.61.",    // Haystack
            "198.125.160.",  // Physicists/ESnet ranges purchased
            "198.125.161.",  // ...
            "198.125.162.",  // ...
            "198.125.163.",  // ...
            "198.125.176.",  // ...
            "198.125.177.",  // ...
            "198.125.178.",  // ...
            "198.125.179."   // ...
        };

        for (int i = 0; i < mitIPs.length; i++)
        {
            if (addr.startsWith(mitIPs[i]))
            {
                return true;
            }
        }

        return false;
    }
}
