/*
 * MetadataImportServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.bulkedit.MetadataImport;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.*;

/**
 * Servlet to import metadata as CSV (comma separated values)
 *
 * @author Stuart Lewis
 */
public class MetadataImportServlet extends DSpaceServlet
{
    /** Upload limit */
    private int limit;

    /** log4j category */
    private static Logger log = Logger.getLogger(MetadataImportServlet.class);

    /**
     * Initalise the servlet
     */
    public void init()
    {
        // Set the lmimt to the number of items that may be changed in one go, default to 20
        limit = ConfigurationManager.getIntProperty("bulkedit.gui-item-limit", 20);
        log.debug("Setting bulk edit limit to " + limit + " items");
    }

    /**
     * Respond to a post request for metadata bulk importing via csv
     *
     * @param context a DSpace Context object
     * @param request the HTTP request
     * @param response the HTTP response
     *
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // First, see if we have a multipart request (uploading a metadata file)
        String contentType = request.getContentType();
        HttpSession session = request.getSession(true);        
        if ((contentType != null) && (contentType.indexOf("multipart/form-data") != -1))
        {
            // Process the file uploaded
            try
            {
                // Get the changes
                log.info(LogManager.getHeader(context, "metadataimport", "loading file"));
                ArrayList<BulkEditChange> changes = processUpload(context, request);
                log.debug(LogManager.getHeader(context, "metadataimport", changes.size() + " items with changes identifed"));                

                // Were there any changes detected?
                if (changes.size() != 0)
                {
                    request.setAttribute("changes", changes);
                    request.setAttribute("changed", false);

                    // Is the user allowed to make this many changes?
                    if (changes.size() <= limit)
                    {
                        request.setAttribute("allow", true);
                    }
                    else
                    {
                        request.setAttribute("allow", false);
                        session.removeAttribute("csv");
                        log.info(LogManager.getHeader(context, "metadataimport", "too many changes: " +
                                                      changes.size() + " (" + limit + " allowed)"));
                    }

                    JSPManager.showJSP(request, response, "/dspace-admin/metadataimport-showchanges.jsp");
                }
                else
                {
                    request.setAttribute("message", "No changes detected");
                    JSPManager.showJSP(request, response, "/dspace-admin/metadataimport.jsp");
                }
            }
            catch (Exception e)
            {
                request.setAttribute("message", e.getMessage());
                log.debug(LogManager.getHeader(context, "metadataimport", "Error encountered while looking for changes: " + e.getMessage()));                
                JSPManager.showJSP(request, response, "/dspace-admin/metadataimport-error.jsp");
            }
        }
        else if ("confirm".equals(request.getParameter("type")))
        {
            // Get the csv lines from the session
            DSpaceCSV csv = (DSpaceCSV)session.getAttribute("csv");

            // Make the changes
            try
            {
                MetadataImport mImport = new MetadataImport(context, csv.getCSVLines());
                ArrayList<BulkEditChange> changes = mImport.runImport(true, false, false, false);

                // Commit the changes
                context.commit();
                log.debug(LogManager.getHeader(context, "metadataimport", changes.size() + " items changed"));

                // Blank out the session data
                session.removeAttribute("csv");

                request.setAttribute("changes", changes);
                request.setAttribute("changed", true);
                request.setAttribute("allow", true);
                JSPManager.showJSP(request, response, "/dspace-admin/metadataimport-showchanges.jsp");
            }
            catch (Exception e)
            {
                request.setAttribute("message", e.getMessage());
                log.debug(LogManager.getHeader(context, "metadataimport", "Error encountered while making changes: " + e.getMessage()));
                JSPManager.showJSP(request, response, "/dspace-admin/metadataimport-error.jsp");
            }
        }
        else if ("cancel".equals(request.getParameter("type")))
        {
            // Blank out the session data
            session.removeAttribute("csv");

            request.setAttribute("message", "Changes cancelled. No items have been modified.");
            log.debug(LogManager.getHeader(context, "metadataimport", "Changes cancelled"));
            JSPManager.showJSP(request, response, "/dspace-admin/metadataimport.jsp");
        }
        else
        {
            // Show the upload screen
            JSPManager.showJSP(request, response, "/dspace-admin/metadataimport.jsp");
        }
    }

    /**
     * GET request is only ever used to show the upload form
     * 
     * @param context
     *            a DSpace Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     *
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Show the upload screen
        JSPManager.showJSP(request, response, "/dspace-admin/metadataimport.jsp");
    }

    /**
     * Process the uploaded file.
     *
     * @param context The DSpace Context
     * @param request The request object
     * @return The response object
     * @throws Exception Thrown if an error occurs
     */
    private ArrayList<BulkEditChange> processUpload(Context context,
                                                    HttpServletRequest request) throws Exception
    {
        // Wrap multipart request to get the submission info
        FileUploadRequest wrapper = new FileUploadRequest(request);
        File f = wrapper.getFile("file");

        // Run the import
        DSpaceCSV csv = new DSpaceCSV(f);
        MetadataImport mImport = new MetadataImport(context, csv.getCSVLines());
        ArrayList<BulkEditChange> changes = mImport.runImport(false, false, false, false);

        // Store the csv lines in the session
        HttpSession session = request.getSession(true);
        session.setAttribute("csv", csv);

        // Remove temp file
        f.delete();

        // Return the changes
        return changes;
    }
}