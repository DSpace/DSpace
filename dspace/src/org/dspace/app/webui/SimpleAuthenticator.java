/*
 * SimpleAuthenticator.java
 *
 * $Id$
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * MIT implementation of DSpace Web UI authentication. This version detects
 * whether the user is an MIT user, and if so, the user is redirected to the
 * certificate login page. Otherwise, the email/password page is used.
 * <P>
 * The special group at MIT is an "MIT Users" group. Users who are on an MIT IP
 * address, or have an e-mail ending in "mit.edu" are implictly members of this
 * group.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class SimpleAuthenticator implements SiteAuthenticator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SiteAuthenticator.class);

    /** ldap enabled */
    private static boolean ldap_enabled = ConfigurationManager.getBooleanProperty("ldap.enable");

    public void startAuthentication(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // Present the username/password screen
        if (ldap_enabled) response.sendRedirect(response.encodeRedirectURL(request
                .getContextPath()
                + "/ldap-login"));
        else response.sendRedirect(response.encodeRedirectURL(request
                .getContextPath()
                + "/password-login"));
    }

    public int[] getSpecialGroups(Context context, HttpServletRequest request)
            throws SQLException
    {
        // Return a list of special group IDs.
        return new int[0];
    }

    /**
     * Indicate whether or not a particular self-registering user can set
     * themselves a password in the profile info form.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide
     * @param email
     *            e-mail address of user attempting to register
     *  
     */
    public boolean allowSetPassword(Context context,
            HttpServletRequest request, String email) throws SQLException
    {
        // Anyone can set themselves a password
        return true;
    }

    /**
     * Indicate whether or not a particular user can self-register, based on
     * e-mail address.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide
     * @param email
     *            e-mail address of user attempting to register
     *  
     */
    public boolean canSelfRegister(Context context, HttpServletRequest request,
            String email) throws SQLException
    {
        // Anyone can register
        return true;
    }

    /**
     * Initialise a new e-person record for a self-registered new user.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed
     * @param eperson
     *            newly created EPerson record - email + information from the
     *            registration form will have been filled out.
     *  
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
        // Any default fields set in an e-person record would be set here
    }
}