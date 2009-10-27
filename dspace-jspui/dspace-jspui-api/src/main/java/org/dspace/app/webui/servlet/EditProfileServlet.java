/*
 * EditProfileServlet.java
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
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Servlet for handling editing user profiles
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class EditProfileServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(EditProfileServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // A GET displays the edit profile form. We assume the authentication
        // filter means we have a user.
        log.info(LogManager.getHeader(context, "view_profile", ""));

        request.setAttribute("eperson", context.getCurrentUser());

        JSPManager.showJSP(request, response, "/register/edit-profile.jsp");
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Get the user - authentication should have happened
        EPerson eperson = context.getCurrentUser();

        // Find out if they're trying to set a new password
        boolean settingPassword = false;

        if ((eperson.getRequireCertificate() == false)
                && (request.getParameter("password") != null)
                && !request.getParameter("password").equals(""))
        {
            settingPassword = true;
        }

        // Set the user profile info
        boolean ok = updateUserProfile(eperson, request);

        if (!ok)
        {
            request.setAttribute("missing.fields", new Boolean(true));
        }

        String passwordProblem = null;

        if (ok && settingPassword)
        {
            // They want to set a new password.
            ok = confirmAndSetPassword(eperson, request);

            if (!ok)
            {
                request.setAttribute("password.problem", new Boolean(true));
            }
        }

        if (ok)
        {
            // Update the DB
            log.info(LogManager.getHeader(context, "edit_profile",
                    "password_changed=" + settingPassword));
            eperson.update();

            // Show confirmation
            request.setAttribute("password.updated", new Boolean(
                    settingPassword));
            JSPManager.showJSP(request, response,
                    "/register/profile-updated.jsp");

            context.complete();
        }
        else
        {
            log.info(LogManager.getHeader(context, "view_profile",
                    "problem=true"));

            request.setAttribute("eperson", eperson);

            JSPManager.showJSP(request, response, "/register/edit-profile.jsp");
        }
    }

    /**
     * Update a user's profile information with the information in the given
     * request. This assumes that authentication has occurred. This method
     * doesn't write the changes to the database (i.e. doesn't call update.)
     * 
     * @param eperson
     *            the e-person
     * @param request
     *            the request to get values from
     * 
     * @return true if the user supplied all the required information, false if
     *         they left something out.
     */
    public static boolean updateUserProfile(EPerson eperson,
            HttpServletRequest request)
    {
        // Get the parameters from the form
        String lastName = request.getParameter("last_name");
        String firstName = request.getParameter("first_name");
        String phone = request.getParameter("phone");
        String language = request.getParameter("language");

        // Update the eperson
        eperson.setFirstName(firstName);
        eperson.setLastName(lastName);
        eperson.setMetadata("phone", phone);
        eperson.setLanguage(language);

        // Check all required fields are there
        if ((lastName == null) || lastName.equals("") || (firstName == null)
                || firstName.equals(""))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Set an eperson's password, if the passwords they typed match and are
     * acceptible. If all goes well and the password is set, null is returned.
     * Otherwise the problem is returned as a String.
     * 
     * @param eperson
     *            the eperson to set the new password for
     * @param request
     *            the request containing the new password
     * 
     * @return true if everything went OK, or false
     */
    public static boolean confirmAndSetPassword(EPerson eperson,
            HttpServletRequest request)
    {
        // Get the passwords
        String password = request.getParameter("password");
        String passwordConfirm = request.getParameter("password_confirm");

        // Check it's there and long enough
        if ((password == null) || (password.length() < 6))
        {
            return false;
        }

        // Check the two passwords entered match
        if (!password.equals(passwordConfirm))
        {
            return false;
        }

        // Everything OK so far, change the password
        eperson.setPassword(password);

        return true;
    }
}
