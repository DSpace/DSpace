/*
 * MITSpecialGroup.java
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
package edu.mit.dspace;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.AuthenticationMethod;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Identify members of "MIT Community" and give them membership in
 * a special group.  It actually does two things:
 * <p>
 * 1. When an MIT user logs in, put them in the special MIT group (so
 *    they get access to materials restricted to the MIT community).
 *    The membership test is by IP address and/or email address.
 * <p>
 * 2. When a new user is registered, set the property that requires
 *    certificate authentication if they have an "@mit.edu" email
 *    address -- and thus presumably an MIT personal web cert.
 * <p>
 * Note that this method does <strong>not</strong> actually authenticate
 * anyone, it just adds a special group.  With stackable authentication it
 * can do its work from within the stack and let other methods handle
 * the authentication.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class MITSpecialGroup
    implements AuthenticationMethod {

    /** log4j category */
    private static Logger log = Logger.getLogger(MITSpecialGroup.class);

    /**
     * Name of DSpace group to which MIT-community clients are
     * automatically added.  The DSpace Admin must create this group.
     */
    public static final String MIT_GROUPNAME = "MIT Users";

    /**
     * We don't care about self registering here.
     * Let a real auth method return true if it wants.
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException
    {
        return false;
    }

    /**
     * Initialize new EPerson.
     *  Policy: Require certificate access for MIT users.
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson)
        throws SQLException
    {
        // If an MIT user, they must use a certificate
        if (isMITEmail(eperson.getEmail()))
        {
            eperson.setRequireCertificate(true);
        }
    }

    /**
     * Predicate, is user allowed to set EPerson password.
     * Anyone whose email address ends with @mit.edu must use a Web cert
     * to log in, so can't set a password
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException
    {
        return !isMITEmail(username);
    }

    /*
     * This is an implicit method, although it doesn't do authentication.
     * The email and IP-based checks should be run in the implicit stack.
     */
    public boolean isImplicit()
    {
        return true;
    }

    /**
     * Add user to special MIT group if they're a member of MIT community.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        EPerson user = context.getCurrentUser();
        boolean hasMITEmail = ((user != null) && isMITEmail(user.getEmail()));

        try {
            if (hasMITEmail || (request != null && isFromMITCommunity(request)))
            {
                log.debug(LogManager.getHeader(context, "getSpecialGroups",
                            "Got an MIT user, looking for group"));

                Group mitGroup = Group.findByName(context, MIT_GROUPNAME);
                if (mitGroup == null)
                {
                    // Oops - the group isn't there.
                    log.warn(LogManager.getHeader(context,
                      "No MIT Group found!! Admin needs to create group named \""+
                       MIT_GROUPNAME+"\"", ""));
             
                    return new int[0];
                }
             
                return new int[] { mitGroup.getID() };
            }
            else
                log.debug(LogManager.getHeader(context, "getSpecialGroups",
                            "Not an MIT user, no groups for you."));

        }
        catch (java.sql.SQLException e)
        {
        }
        return new int[0];
    }

    /**
     * This method is not used.
     * This class is only for special groups and enforcement of cert policy.
     * Use X509Authentication to authenticate.
     *
     * @return One of: SUCCESS, BAD_CREDENTIALS, NO_SUCH_USER, BAD_ARGS
     */
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException
    {
        return BAD_ARGS;
    }

    /*
     * Returns URL to which to redirect to obtain credentials (either password
     * prompt or e.g. HTTPS port for client cert.); null means no redirect.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    public String loginPageURL(Context context,
                            HttpServletRequest request,
                            HttpServletResponse response)
    {
        return null;
    }

    public String loginPageTitle(Context context)
    {
        return null;
    }

    /**
     * Crude way to identify an MIT community member: does their
     * email end in @mit.edu ?  This will be true for anyone who
     * "logs in" with an MIT client web cert, but could also be
     * a false positive.  Someday perhaps use Data Warehouse feed to check.
     */
    private static boolean isMITEmail(String email)
    {
        return email.toLowerCase().trim().endsWith("@mit.edu");
    }

    /**
     * Check to see if the user is an MIT user. At present, it just checks the
     * source IP address. Note this is independent of user authentication - if
     * the user is an off-site MIT user, this will still return false.
     * <p>
     * XXX Note: The list of IP addresses really ought to be in a
     * configuration property, not hardcoded, since it can change on
     * short notice!
     *
     * @param request
     *            current request
     *
     * @return true if the user is an MIT user.
     */
    private static boolean isFromMITCommunity(HttpServletRequest request)
    {
        String addr = request.getRemoteAddr();

        log.debug("checking MIT membership of IP addr="+addr);

        final String[] mitIPs = {
                "18.",     // Good old Net 18
                "128.52.", // AI
                "129.55.", // Lincoln
                "192.52.65.", // Haystack
                "192.52.61.", // Haystack
                "198.125.160.", // Physicists/ESnet ranges purchased
                "198.125.161.", // ...
                "198.125.162.", // ...
                "198.125.163.", // ...
                "198.125.176.", // ...
                "198.125.177.", // ...
                "198.125.178.", // ...
                "198.125.179." // ...
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
