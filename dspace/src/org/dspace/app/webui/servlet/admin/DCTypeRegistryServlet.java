/*
 * DCTypeRegistry.java
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

import org.dspace.administer.DCType;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Servlet for editing the Dublin Core registry
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class DCTypeRegistryServlet extends DSpaceServlet
{
    /** User wants to edit a format */
    public static final int START_EDIT = 1;

    /** User wants to delete a format */
    public static final int START_DELETE = 2;
    
    /** User confirms edit of a format */
    public static final int CONFIRM_EDIT = 3;

    /** User confirms delete of a format */
    public static final int CONFIRM_DELETE = 4;

    /** User wants to create a new format */
    public static final int CREATE = 4;

    /** Logger */
    private static Logger log = Logger.getLogger(DCTypeRegistryServlet.class);


    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // GET just displays the list of type
        showTypes(context, request, response);
    }
    
    
    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_update"))
        {
            // Update the metadata for a DC type
            DCType dc = DCType.find(context,
                UIUtil.getIntParameter(request, "dc_type_id"));

            dc.setElement(request.getParameter("element"));
            
            String qual = request.getParameter("qualifier");
            if (qual.equals(""))
            {
                qual = null;
            }
            dc.setQualifier(qual);
            
            dc.setScopeNote(request.getParameter("scope_note"));

            dc.update();
            
            showTypes(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_add"))
        {
            // Add a new DC type - simply add to the list, and let the user
            // edit with the main form
            DCType dc = DCType.create(context);
            
            dc.update();
            
            showTypes(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            DCType dc = DCType.find(context,
                UIUtil.getIntParameter(request, "dc_type_id"));
            request.setAttribute("type", dc);
            JSPManager.showJSP(request, response,
                "/admin/confirm-delete-dctype.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            DCType dc = DCType.find(context,
                UIUtil.getIntParameter(request, "dc_type_id"));
            dc.delete();
            
            showTypes(context, request, response);
            context.complete();
        }            
        else
        {
            // Cancel etc. pressed - show list again
            showTypes(context, request, response);
        }
    }


    /**
     * Show list of DC type
     *
     * @param context   Current DSpace context
     * @param request   Current HTTP request
     * @param response  Current HTTP response
     */
    private void showTypes(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        DCType[] types = DCType.findAll(context);
        
        request.setAttribute("types", types);
        JSPManager.showJSP(request, response, "/admin/list-dc-types.jsp");
    }
}
