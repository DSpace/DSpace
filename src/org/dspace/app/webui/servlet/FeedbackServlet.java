/*
 * FeedbackServlet.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2004/12/22 17:48:35 $
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
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Servlet for handling user feedback
 * 
 * @author Peter Breton
 * @author Robert Tansley
 * @version $Revision: 1.4 $
 */
public class FeedbackServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(FeedbackServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Obtain information from request
        // The page where the user came from
        String fromPage = request.getParameter("fromPage");

        // The email address they provided
        String formEmail = request.getParameter("email");

        // Browser
        String userAgent = request.getHeader("User-Agent");

        // Session id
        String sessionID = request.getSession().getId();

        // User email from context
        EPerson currentUser = context.getCurrentUser();
        String authEmail = null;

        if (currentUser != null)
        {
            authEmail = currentUser.getEmail();
        }

        // Has the user just posted their feedback?
        if (request.getParameter("submit") != null)
        {
            String feedback = request.getParameter("feedback");

            // Check all data is there
            if ((formEmail == null) || formEmail.equals("")
                    || (feedback == null) || feedback.equals(""))
            {
                log.info(LogManager.getHeader(context, "show_feedback_form",
                        "problem=true"));
                request.setAttribute("feedback.problem", new Boolean(true));
                JSPManager.showJSP(request, response, "/feedback/form.jsp");

                return;
            }

            // All data is there, send the email
            try
            {
                Email email = ConfigurationManager.getEmail("feedback");
                email.addRecipient(ConfigurationManager
                        .getProperty("feedback.recipient"));

                email.addArgument(new Date()); // Date
                email.addArgument(formEmail); // Email
                email.addArgument(authEmail); // Logged in as
                email.addArgument(fromPage); // Referring page
                email.addArgument(userAgent); // User agent
                email.addArgument(sessionID); // Session ID
                email.addArgument(feedback); // The feedback itself

                // Replying to feedback will reply to email on form
                email.setReplyTo(formEmail);

                email.send();

                log.info(LogManager.getHeader(context, "sent_feedback", "from="
                        + formEmail));

                JSPManager.showJSP(request, response,
                        "/feedback/acknowledge.jsp");
            }
            catch (MessagingException me)
            {
                log.warn(LogManager.getHeader(context,
                        "error_mailing_feedback", ""), me);

                JSPManager.showInternalError(request, response);
            }
        }
        else
        {
            // Display feedback form
            log.info(LogManager.getHeader(context, "show_feedback_form",
                    "problem=false"));
            request.setAttribute("authenticated.email", authEmail);
            JSPManager.showJSP(request, response, "/feedback/form.jsp");
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Treat as a GET
        doDSGet(context, request, response);
    }
}