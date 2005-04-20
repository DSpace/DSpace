/*
 * BitstreamFormatRegistry.java
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
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;

/**
 * Servlet for editing the bitstream format registry
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class BitstreamFormatRegistry extends DSpaceServlet
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
    private static Logger log = Logger.getLogger(BitstreamFormatRegistry.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET just displays the list of formats
        showFormats(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_update"))
        {
            // Update the metadata for a bitstream format
            BitstreamFormat bf = BitstreamFormat.find(context, UIUtil
                    .getIntParameter(request, "format_id"));

            bf.setMIMEType(request.getParameter("mimetype"));
            bf.setShortDescription(request.getParameter("short_description"));
            bf.setDescription(request.getParameter("description"));
            bf
                    .setSupportLevel(UIUtil.getIntParameter(request,
                            "support_level"));
            bf.setInternal((request.getParameter("internal") != null)
                    && request.getParameter("internal").equals("true"));

            // Separate comma-separated extensions
            List extensions = new LinkedList();
            String extParam = request.getParameter("extensions");

            while (extParam.length() > 0)
            {
                int c = extParam.indexOf(',');

                if (c > 0)
                {
                    extensions.add(extParam.substring(0, c).trim());
                    extParam = extParam.substring(c + 1).trim();
                }
                else
                {
                    if (extParam.trim().length() > 0)
                    {
                        extensions.add(extParam.trim());
                        extParam = "";
                    }
                }
            }

            // Set extensions in the format - convert to array
            String[] extArray = (String[]) extensions
                    .toArray(new String[extensions.size()]);
            bf.setExtensions(extArray);

            bf.update();

            showFormats(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_add"))
        {
            // Add a new bitstream - simply add to the list, and let the user
            // edit with the main form
            BitstreamFormat bf = BitstreamFormat.create(context);

            // We set the "internal" flag to true, so that the empty bitstream
            // format doesn't show up in the submission UI yet
            bf.setInternal(true);
            bf.update();

            showFormats(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            BitstreamFormat bf = BitstreamFormat.find(context, UIUtil
                    .getIntParameter(request, "format_id"));
            request.setAttribute("format", bf);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/confirm-delete-format.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of format
            BitstreamFormat bf = BitstreamFormat.find(context, UIUtil
                    .getIntParameter(request, "format_id"));
            bf.delete();

            showFormats(context, request, response);
            context.complete();
        }
        else
        {
            // Cancel etc. pressed - show list again
            showFormats(context, request, response);
        }
    }

    /**
     * Show list of bitstream formats
     * 
     * @param context
     *            Current DSpace context
     * @param request
     *            Current HTTP request
     * @param response
     *            Current HTTP response
     */
    private void showFormats(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        BitstreamFormat[] formats = BitstreamFormat.findAll(context);

        request.setAttribute("formats", formats);
        JSPManager.showJSP(request, response, "/dspace-admin/list-formats.jsp");
    }
}
