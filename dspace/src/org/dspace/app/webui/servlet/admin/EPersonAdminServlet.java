/*
 * EPersonAdminServlet.java
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

package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Servlet for editing and creating e-people
 *
 * @author  David Stuve
 * @version $Revision$
 */
public class EPersonAdminServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(EditEPersonServlet.class);

    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        doDSPost(context, request, response);

        // GET just displays the list of e-people
        //showMain(context, request, response);
    }
    
    
    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_add"))
        {
            // add an EPerson, then jump user to edit page
            EPerson e = EPerson.create(context);

            // create clever name and do update before continuing            
            e.setEmail("newuser"+e.getID());
            e.update();
            
            //showEPeople(context, request, response);

            request.setAttribute("eperson", e);

            JSPManager.showJSP(request, response,
                "/admin/eperson-edit.jsp");

            context.complete();
        }
        else if (button.equals("submit_edit"))
        {
            // edit an eperson
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));

            request.setAttribute("eperson", e);

            JSPManager.showJSP(request, response,
                "/admin/eperson-edit.jsp");

            context.complete();
        }
        else if (button.equals("submit_save"))
        {
            // Update the metadata for an e-person
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));

            // see if the user changed the email - if so, make sure
            // the new email is unique
            String oldEmail = e.getEmail();
            String newEmail = request.getParameter("email").trim();
            
            if( !newEmail.equals( oldEmail ) )
            {
                // change to email, now see if it's unique
                if( EPerson.findByEmail( context, newEmail ) == null )
                {
                    // it's unique - proceed!
                    e.setEmail( newEmail );

                    e.setFirstName(request.getParameter("firstname").equals("")
                        ? null
                        : request.getParameter("firstname"));

                    e.setLastName(request.getParameter("lastname").equals("")
                        ? null
                        : request.getParameter("lastname"));
            
                    // FIXME: More data-driven?
                    e.setMetadata("phone",
                    request.getParameter("phone").equals("")
                        ? null
                        : request.getParameter("phone"));

                    e.setCanLogIn(request.getParameter("can_log_in") != null &&
                        request.getParameter("can_log_in").equals("true"));
            
                    e.setRequireCertificate(
                        request.getParameter("require_certificate") != null &&
                        request.getParameter("require_certificate").equals("true"));

                    e.update();
            
                    showMain(context, request, response);
                    context.complete();
                }
                else
                {
                    // not unique - send error message & let try again
                    request.setAttribute("eperson", e);
                    request.setAttribute("error_message", "That EMail is in use by another EPerson.  Emails  must be unique.");

                    JSPManager.showJSP(request, response,
                        "/admin/eperson-edit.jsp");

                    context.complete();
                }
            }
            else
            {
                // no change to email
                e.setFirstName(request.getParameter("firstname").equals("")
                    ? null
                    : request.getParameter("firstname"));

                e.setLastName(request.getParameter("lastname").equals("")
                    ? null
                    : request.getParameter("lastname"));
            
                // FIXME: More data-driven?
                e.setMetadata("phone",
                request.getParameter("phone").equals("")
                    ? null
                    : request.getParameter("phone"));

                e.setCanLogIn(request.getParameter("can_log_in") != null &&
                    request.getParameter("can_log_in").equals("true"));
            
                e.setRequireCertificate(
                    request.getParameter("require_certificate") != null &&
                    request.getParameter("require_certificate").equals("true"));

                e.update();
            
                showMain(context, request, response);
                context.complete();
            }
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));

            request.setAttribute("eperson", e);

            JSPManager.showJSP(request, response,
                "/admin/eperson-confirm-delete.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));

            e.delete();
            
            showMain(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_browse"))
        {
            // user wants to browse
            
            String pageRequest = request.getParameter("page_request");
            int pageIndex = UIUtil.getIntParameter(request, "page_index");
            String sortby = request.getParameter("sortby");
            int sortField = EPerson.EMAIL; // default
            int pageSize = 50;

            if (sortby == null)
            {
            }
            else if (sortby.equals("lastname"))
            {
                sortField = EPerson.LASTNAME;
            }
            else if (sortby.equals("id"))
            {
                sortField = EPerson.ID;
            }

            if (pageIndex == -1)
            {
                pageIndex = 0;  // default page is 0
            }

            // get back "next" and "previous" messages
            //  can also insert numbers here
            if (pageRequest != null)
            {
                if (pageRequest.equals("next"))
                {
                    pageIndex++;
                }
                else
                {
                    pageIndex--;
                }
            }

            EPerson[] epeople = EPerson.findAll(context, sortField);
            
            int pageCount = ((epeople.length-1)/pageSize)+1;
                        
            request.setAttribute("epeople",    epeople  );
            request.setAttribute("page_size",  new Integer(pageSize ));
            request.setAttribute("page_count", new Integer(pageCount));
            request.setAttribute("page_index", new Integer(pageIndex));
            
            JSPManager.showJSP(request, response, "/admin/eperson-browse.jsp");            
        }
        else
        {
            // Cancel etc. pressed - show list again
            showMain(context, request, response);
        }
    }


    /**
     * Show list of E-people
     *
     * @param context   Current DSpace context
     * @param request   Current HTTP request
     * @param response  Current HTTP response
     */
    private void showEPeople(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // First get the sort field
        String sortFieldParam = request.getParameter("sortby");
        int sortField = EPerson.LASTNAME;
        
        if (sortFieldParam != null && sortFieldParam.equals("email"))
        {
            sortField = EPerson.EMAIL;
        }
        else if (sortFieldParam != null && sortFieldParam.equals("id"))
        {
            sortField = EPerson.ID;
        }

        EPerson[] epeople = EPerson.findAll(context, sortField);
        
        request.setAttribute("epeople", epeople);
        JSPManager.showJSP(request, response, "/admin/list-epeople.jsp");
    }
    
    
    private void showMain(Context c,
                    HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        JSPManager.showJSP(request, response, "/admin/eperson-main.jsp");
    }
}
