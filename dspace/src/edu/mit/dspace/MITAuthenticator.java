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

import org.apache.log4j.Logger;

import org.dspace.app.webui.SiteAuthenticator;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * MIT implementation of DSpace Web UI authentication.  This version detects
 * whether the user is an MIT user, and if so, the user is redirected to the
 * certificate login page.  Otherwise, the email/password page is used.
 * <P>
 * The special group at MIT is an "MIT Users" group.  Users who are on an
 * MIT IP address, or have an e-mail ending in "mit.edu" are implictly
 * members of this group.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class MITAuthenticator implements SiteAuthenticator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SiteAuthenticator.class);

    public void startAuthentication(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        if (isMITUser(request))
        {
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


    public int[] getSpecialGroups(Context context,
        HttpServletRequest request)
        throws SQLException
    {        
        // Add user to "MIT Users" special group if they're an MIT user

        EPerson user = context.getCurrentUser();
        boolean hasMITEmail = (user != null &&
            user.getEmail().toLowerCase().endsWith("@mit.edu"));

        if (hasMITEmail || isMITUser(request))
        {
            // add the user to the special group "MIT Users"
            Group mitGroup = Group.findByName(context, "MIT Users");

            if (mitGroup == null)
            {
                // Oops - the group isn't there.
                log.warn(LogManager.getHeader(context,
                    "No MIT Group!!",
                    ""));
                return new int[0];
            }

            return new int[] {mitGroup.getID()};
        }

        return new int[0];
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
