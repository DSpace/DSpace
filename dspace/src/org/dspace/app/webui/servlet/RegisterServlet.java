/*
 * RegisterServlet.java
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

package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.AccountManager;
import org.dspace.eperson.EPerson;


/**
 * Servlet for handling user registration and forgotten passwords.
 * <P>
 * This servlet handles both forgotten passwords and initial registration of
 * users.  Which it handles depends on the initialisation parameter
 * "register" - if it's "true", it is treated as an initial registration
 * and the user is asked to input their personal information.
 * <P>
 * The sequence of events is this:  The user clicks on "register" or "I forgot
 * my password."  This servlet then displays the relevant "enter your e-mail"
 * form.  An e-mail address is POSTed back, and if this is valid, a token
 * is created and e-mailed, otherwise an error is displayed, with another
 * "enter your e-mail" form.
 * <P>
 * When the user clicks on the token URL mailed to them, this servlet receives
 * a GET with the token as the parameter "KEY".  If this is a valid token,
 * the servlet then displays the "edit profile" or "edit password" screen
 * as appropriate.
 */
public class RegisterServlet extends DSpaceServlet
{

    /** Logger */
    private static Logger log = Logger.getLogger(RegisterServlet.class);

    /** The "enter e-mail" step */
    public static final int ENTER_EMAIL_PAGE = 1;
	
    /** The "enter personal info" page */
    public static final int PERSONAL_INFO_PAGE = 2;
	
    /** The simple "enter new password" page */
    public static final int NEW_PASSWORD_PAGE = 3;

    /** true = registering users, false = forgotten passwords */
    private boolean registering;


    public void init()
    {
        registering = getInitParameter("register").equalsIgnoreCase("true");
    }


    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
       /* Respond to GETs.  A simple GET with no parameters will display
        * the relevant "type in your e-mail" form.  A GET with a "key"
        * parameter will go to the "enter personal info" or "enter new
        * password" page as appropriate.
        */

        // Get the key
        String key = request.getParameter("token");

        if (key == null)
        {
            // Simple "enter your e-mail" page
            if (registering)
            {
                // Registering a new user
                JSPManager.showJSP(request, response, "/register/new-user.jsp");
            }
            else
            {
                // User forgot their password
                JSPManager.showJSP(request,
                    response,
                    "/register/forgot-password.jsp");
            }
        }
        else
        {
            // Find out who the key is for
            EPerson eperson = AccountManager.getEPerson(context, key);

            /* Display an error if it's:
             *  An invalid token
             *  An active eperson is trying to register
             *  An active eperson who needs to use certs is trying to set a p/w
             *  An inactive (unregistered) eperson is trying to set a new p/w
             */
            if (eperson == null ||
               (eperson.getActive() && registering) ||
               (eperson.getActive() && eperson.getRequireCertificate() && registering) ||
               (!registering && !eperson.getActive()))
            {
                // Invalid token
                JSPManager.showJSP(request,
                    response,
                    "/register/invalid-token.jsp");
                return;
            }

            // Both forms need an EPerson object
            request.setAttribute("eperson", eperson);

            // And the token
            request.setAttribute("key", key);

            // Put up the relevant form
            if (registering)
            {
                JSPManager.showJSP(request,
                    response,
                    "/register/registration-form.jsp");
            }
            else
            {
                JSPManager.showJSP(request,
                    response,
                    "/register/new-password.jsp");
            }
        }
    }


    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        /*
         * POSTs are the result of entering an e-mail in the
         * "forgot my password" or "new user" forms, or the "enter profile
         * information" or "enter new password" forms.
         */

        // First get the step
        int step = UIUtil.getIntParameter(request, "step");

        switch (step)
        {
        case ENTER_EMAIL_PAGE:
            processEnterEmail(context, request, response);
            break;

        case PERSONAL_INFO_PAGE:
            processPersonalInfo(context, request, response);
            break;

        case NEW_PASSWORD_PAGE:
            processNewPassword(context, request, response);
            break;

        default:
            log.warn(LogManager.getHeader(context,
                "integrity_error",
                UIUtil.getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Process information from the "enter e-mail" page.  If the e-mail
     * corresponds to a valid user of the system, a token is generated
     * and sent to that user.
     *
     * @param context   current DSpace context
     * @param request   current servlet request object
     * @param response  current servlet response object
     */
    private void processEnterEmail(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String email = request.getParameter("email");

        EPerson eperson = EPerson.findByEmail(context, email);
        
        if (eperson != null)
        {
            // Can't register an already active user
            if (eperson.getActive() && registering)
            {
                log.info(LogManager.getHeader(context,
                    "already_registered",
                    "email=" + email));

                JSPManager.showJSP(request,
                    response,
                    "/register/already-registered.jsp");
                return;
            }

            // Can't give new password to inactive user
            if (!eperson.getActive() && !registering)
            {
                log.info(LogManager.getHeader(context,
                    "unregistered_forgot_password",
                    "email=" + email));

                JSPManager.showJSP(request,
                    response,
                    "/register/inactive-account.jsp");
                return;
            }

            // User that requires certificate can't get password
            if (eperson.getRequireCertificate() && !registering)
            {
                log.info(LogManager.getHeader(context,
                    "certificate_user_forgot_password",
                    "email=" + email));

                    JSPManager.showJSP(request,
                        response,
                        "/error/require-certificate.jsp");
                    return;
            }

            // Now send info.
            try
            {
                if (registering)
                {
                    log.info(LogManager.getHeader(context,
                        "sendtoken_register",
                        "email=" + email));

                    AccountManager.sendRegistrationInfo(context, email);
                    JSPManager.showJSP(request,
                        response,
                        "/register/registration-sent.jsp");
                }
                else
                {
                    log.info(LogManager.getHeader(context,
                        "sendtoken_forgotpw",
                        "email=" + email));

                    AccountManager.sendForgotPasswordInfo(context, email);
                    JSPManager.showJSP(request,
                        response,
                        "/register/password-token-sent.jsp");
                }

                // Context needs completing to write registration data
                context.complete();
            }
            catch (MessagingException me)
            {
                log.info(LogManager.getHeader(context,
                    "error_emailing",
                    "email=" + email),
                    me);

                JSPManager.showInternalError(request, response);
            }
        }
        else
        {
            log.info(LogManager.getHeader(context,
                "unknown_email",
                "email=" + email));

            request.setAttribute("retry", new Boolean(true));

            if (registering)
            {
                JSPManager.showJSP(request,
                    response,
                    "/register/new-user.jsp");
            }
            else
            {
                JSPManager.showJSP(request,
                    response,
                    "/register/forgot-password.jsp");
            }                
        }
    }

    /**
     * Process information from "Personal information page"
     *
     * @param context   current DSpace context
     * @param request   current servlet request object
     * @param response  current servlet response object
     */
    private void processPersonalInfo(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Get the key
        String key = request.getParameter("key");

        // Get the eperson associated with the registration
        EPerson eperson = AccountManager.getEPerson(context, key);
		
        // If the token isn't valid, show an error
        if (eperson == null)
        {
            log.info(LogManager.getHeader(context,
                "invalid_token",
                "token=" + key));

            // Invalid token
            JSPManager.showJSP(request,
                response,
                "/register/invalid-token.jsp");
            return;
        }
		
        // If the token is valid, we set the current user of the context
        // to the user associated with the token, so they can update their
        // info
        context.setCurrentUser(eperson);

        // Set the user profile info
        boolean infoOK = EditProfileServlet.updateUserProfile(eperson, request);

        boolean passwordOK = true;
        
        if (eperson.getRequireCertificate() == false)
        {
            passwordOK = EditProfileServlet.confirmAndSetPassword(eperson,
                request);
        }

        if (!infoOK)
        {
            request.setAttribute("missing.fields", new Boolean(true));
        }
        
        if (!passwordOK)
        {
            request.setAttribute("password.problem", new Boolean(true));
        }
		
        request.setAttribute("eperson", eperson);

        // Forward to appropriate page
        if (infoOK && passwordOK)
        {
            log.info(LogManager.getHeader(context,
                "usedtoken_register",
                "email=" + eperson.getEmail()));

            // delete the token
            AccountManager.deleteToken(context, key);
			
            // Set the user as active
            eperson.setActive(true);
            eperson.update();
			
            JSPManager.showJSP(request, response, "/register/registered.jsp");
            context.complete();
        }
        else
        {
            request.setAttribute("key", key);

            JSPManager.showJSP(request,
                response,
                "/register/registration-form.jsp");
        }
    }


    /**
     * Process information from "enter new password"
     *
     * @param context   current DSpace context
     * @param request   current servlet request object
     * @param response  current servlet response object
     */
    private void processNewPassword(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Get the key
        String key = request.getParameter("key");

        // Get the eperson associated with the password change
        EPerson eperson = AccountManager.getEPerson(context, key);

        // If the token isn't valid, show an error
        if (eperson == null)
        {
            log.info(LogManager.getHeader(context,
                "invalid_token",
                "token=" + key));

            // Invalid token
            JSPManager.showJSP(request,
                response,
                "/register/invalid-token.jsp");
            return;
        }
		
        // If the token is valid, we set the current user of the context
        // to the user associated with the token, so they can update their
        // info
        context.setCurrentUser(eperson);

        // Confirm and set the password
        boolean passwordOK = EditProfileServlet.confirmAndSetPassword(
            eperson, request);

        if (passwordOK)
        {
            log.info(LogManager.getHeader(context,
                "usedtoken_forgotpw",
                "email=" + eperson.getEmail()));

            AccountManager.deleteToken(context, key);
			
            JSPManager.showJSP(request,
                response,
                "/register/password-changed.jsp");
            context.complete();
        }
        else
        {
            request.setAttribute("password.problem", new Boolean(true));
            request.setAttribute("key", key);
            request.setAttribute("eperson", eperson);

            JSPManager.showJSP(request,
                response,
                "/register/new-password.jsp");
        }
    }
}
