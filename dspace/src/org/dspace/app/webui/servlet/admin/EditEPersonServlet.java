/*
 * EditEPersonServlet.java
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
 * @author  Robert Tansley
 * @version $Revision$
 */
public class EditEPersonServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(EditEPersonServlet.class);


    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // GET just displays the list of e-people
        showEPeople(context, request, response);
    }
    
    
    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_update"))
        {
            // Update the metadata for an e-person
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));
                
            e.setEmail(request.getParameter("email").trim());

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

            e.setCanLogIn(request.getParameter("active") != null &&
                        request.getParameter("active").equals("true"));
            
            e.setRequireCertificate(
                request.getParameter("require_certificate") != null &&
                request.getParameter("require_certificate").equals("true"));

            e.update();
            
            showEPeople(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_add"))
        {
            // Add a new E-person - simply add to the list, and let the user
            // edit with the main form
            EPerson e = EPerson.create(context);
            
            e.update();
            
            showEPeople(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));

            request.setAttribute("eperson", e);

            JSPManager.showJSP(request, response,
                "/admin/confirm-delete-eperson.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            EPerson e = EPerson.find(context,
                UIUtil.getIntParameter(request, "eperson_id"));

            e.delete();
            
            showEPeople(context, request, response);
            context.complete();
        }
        else
        {
            // Cancel etc. pressed - show list again
            showEPeople(context, request, response);
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
}
