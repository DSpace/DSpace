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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * Servlet for handling editing user profiles
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class EditProfileServlet extends DSpaceServlet
{
    /** Logger */
    private static final Logger log = Logger.getLogger(EditProfileServlet.class);
    
    protected transient EPersonService personService
             = EPersonServiceFactory.getInstance().getEPersonService();

    @Override
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

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Get the user - authentication should have happened
        EPerson eperson = context.getCurrentUser();

        // Find out if they're trying to set a new password
        boolean settingPassword = false;

        if (!eperson.getRequireCertificate() && !StringUtils.isEmpty(request.getParameter("password")))
        {
            settingPassword = true;
        }

        // Set the user profile info
        boolean ok = updateUserProfile(context, eperson, request);

        if (!ok)
        {
            request.setAttribute("missing.fields", Boolean.TRUE);
        }

        if (ok && settingPassword)
        {
            // They want to set a new password.
            ok = confirmAndSetPassword(eperson, request);

            if (!ok)
            {
                request.setAttribute("password.problem", Boolean.TRUE);
            }
        }

        if (ok)
        {
            // Update the DB
            log.info(LogManager.getHeader(context, "edit_profile",
                    "password_changed=" + settingPassword));
            personService.update(context, eperson);

            // Show confirmation
            request.setAttribute("password.updated", settingPassword);
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
    public boolean updateUserProfile(Context context, EPerson eperson,
            HttpServletRequest request) throws SQLException
    {
        // Get the parameters from the form
        String lastName = request.getParameter("last_name");
        String firstName = request.getParameter("first_name");
        String phone = request.getParameter("phone");
        String language = request.getParameter("language");

        // Update the eperson
        eperson.setFirstName(context, firstName);
        eperson.setLastName(context, lastName);
        personService.setMetadataSingleValue(context, eperson, "eperson" , "phone", null, null, phone);
        eperson.setLanguage(context, language);

        // Check all required fields are there
        return (!StringUtils.isEmpty(lastName) && !StringUtils.isEmpty(firstName));
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
    public  boolean confirmAndSetPassword(EPerson eperson,
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
        personService.setPassword(eperson, password);

        return true;
    }
}
