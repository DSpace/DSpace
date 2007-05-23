/*
 * EPersonAdminServlet.java
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
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.EPersonDeletionException;

/**
 * Servlet for editing and creating e-people
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class EPersonAdminServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(EPersonAdminServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        showMain(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_add"))
        {
            // add an EPerson, then jump user to edit page
            EPerson e = EPerson.create(context);

            // create clever name and do update before continuing
            e.setEmail("newuser" + e.getID());
            e.update();

            request.setAttribute("eperson", e);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/eperson-edit.jsp");

            context.complete();
        }
        else if (button.equals("submit_edit"))
        {
            // edit an eperson
            EPerson e = EPerson.find(context, UIUtil.getIntParameter(request,
                    "eperson_id"));
            
            // Check the EPerson exists
            if (e == null)
            {
            	request.setAttribute("no_eperson_selected", new Boolean(true));
            	showMain(context, request, response);
            }
            else 
            {            
	            // what groups is this person a member of?
	            Group[] groupMemberships = Group.allMemberGroups(context, e);
	
	            request.setAttribute("eperson", e);
	            request.setAttribute("group.memberships", groupMemberships);
	
	            JSPManager.showJSP(request, response,
	                    "/dspace-admin/eperson-edit.jsp");
	
	            context.complete();
            }
        }
        else if (button.equals("submit_save"))
        {
            // Update the metadata for an e-person
            EPerson e = EPerson.find(context, UIUtil.getIntParameter(request,
                    "eperson_id"));

            // see if the user changed the email - if so, make sure
            // the new email is unique
            String oldEmail = e.getEmail();
            String newEmail = request.getParameter("email").trim();
            String netid = request.getParameter("netid");

            if (!newEmail.equals(oldEmail))
            {
                // change to email, now see if it's unique
                if (EPerson.findByEmail(context, newEmail) == null)
                {
                    // it's unique - proceed!
                    e.setEmail(newEmail);

                    e
                            .setFirstName(request.getParameter("firstname")
                                    .equals("") ? null : request
                                    .getParameter("firstname"));

                    e
                            .setLastName(request.getParameter("lastname")
                                    .equals("") ? null : request
                                    .getParameter("lastname"));

                    if (netid != null)
                    {
                        e.setNetid(netid.equals("") ? null : netid);
                    }
                    else
                    {
                        e.setNetid(null);
                    }

                    // FIXME: More data-driven?
                    e.setMetadata("phone", request.getParameter("phone")
                            .equals("") ? null : request.getParameter("phone"));
 
                    e.setMetadata("language", request.getParameter("language")
                           .equals("") ? null : request.getParameter("language"));
                    
                    e.setCanLogIn((request.getParameter("can_log_in") != null)
                            && request.getParameter("can_log_in")
                                    .equals("true"));

                    e.setRequireCertificate((request
                            .getParameter("require_certificate") != null)
                            && request.getParameter("require_certificate")
                                    .equals("true"));

                    e.update();

                    showMain(context, request, response);
                    context.complete();
                }
                else
                {
                    // not unique - send error message & let try again
                    request.setAttribute("eperson", e);
                    request.setAttribute("email_exists", new Boolean(true));

                    JSPManager.showJSP(request, response,
                            "/dspace-admin/eperson-edit.jsp");

                    context.complete();
                }
            }
            else
            {
                // no change to email
                if (netid != null)
                {
                    e.setNetid(netid.equals("") ? null : netid);
                }
                else
                {
                    e.setNetid(null);
                }

                e
                        .setFirstName(request.getParameter("firstname").equals(
                                "") ? null : request.getParameter("firstname"));

                e
                        .setLastName(request.getParameter("lastname")
                                .equals("") ? null : request
                                .getParameter("lastname"));

                // FIXME: More data-driven?
                e.setMetadata("phone",
                        request.getParameter("phone").equals("") ? null
                                : request.getParameter("phone"));
                
                e.setMetadata("language", request.getParameter("language")
                        .equals("") ? null : request.getParameter("language"));
                         
                e.setCanLogIn((request.getParameter("can_log_in") != null)
                        && request.getParameter("can_log_in").equals("true"));

                e.setRequireCertificate((request
                        .getParameter("require_certificate") != null)
                        && request.getParameter("require_certificate").equals(
                                "true"));

                e.update();

                showMain(context, request, response);
                context.complete();
            }
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            EPerson e = EPerson.find(context, UIUtil.getIntParameter(request,
                    "eperson_id"));
            
            // Check the EPerson exists
            if (e == null)
            {
            	request.setAttribute("no_eperson_selected", new Boolean(true));
            	showMain(context, request, response);
            }
            else 
            {       
	            request.setAttribute("eperson", e);
	
	            JSPManager.showJSP(request, response,
	                    "/dspace-admin/eperson-confirm-delete.jsp");
            }
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            EPerson e = EPerson.find(context, UIUtil.getIntParameter(request,
                    "eperson_id"));

            try
            {
                e.delete();
            }
            catch (EPersonDeletionException ex)
            {
                request.setAttribute("eperson", e);
                request.setAttribute("tableList", ex.getTables());
                JSPManager.showJSP(request, response,
                        "/dspace-admin/eperson-deletion-error.jsp");
            }

            showMain(context, request, response);
            context.complete();
        }
        else
        {
            // Cancel etc. pressed - show list again
            showMain(context, request, response);
        }
    }

    private void showMain(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        JSPManager.showJSP(request, response, "/dspace-admin/eperson-main.jsp");
    }
}
