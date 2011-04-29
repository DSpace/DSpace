/*
 * BrowserServlet.java
 *
 * Version: $Revision: 4231 $
 *
 * Date: $Date: 2009-08-24 23:17:33 -0400 (Mon, 24 Aug 2009) $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.ItemIterator;
import org.apache.log4j.Logger;

/**
 * Servlet for browsing through indices, as they are defined in 
 * the configuration.  This class can take a wide variety of inputs from
 * the user interface:
 * 
 * - type:	the type of browse (index name) being performed
 * - order: (ASC | DESC) the direction for result sorting
 * - value: A specific value to find items around.  For example the author name or subject
 * - month: integer specification of the month of a date browse
 * - year: integer specification of the year of a date browse
 * - starts_with: string value at which to start browsing
 * - vfocus: start browsing with a value of this string
 * - focus: integer id of the item at which to start browsing
 * - rpp: integer number of results per page to display
 * - sort_by: integer specification of the field to search on
 * - etal: integer number to limit multiple value items specified in config to
 * 
 * @author Richard Jones
 * @version $Revision: 4231 $
 */
public class BrowserServlet extends AbstractBrowserServlet
{

    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractBrowserServlet.class);

    /**
     * Do the usual DSpace GET method.  You will notice that browse does not currently
     * respond to POST requests.
     */
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // all browse requests currently come to GET.
        BrowserScope scope = getBrowserScopeForRequest(context, request, response);

        if (scope.getBrowseIndex() == null)
        {
            throw new ServletException("There is no browse index for the request");
        }

        // Is this a request to export the metadata, or a normal browse request?
        if ("submit_export_metadata".equals(UIUtil.getSubmitButton(request, "submit")))
        {
            exportMetadata(context, request, response, scope);
        }
        else
        {
            // execute browse request
            processBrowse(context, scope, request, response);
        }
    }

    
    /**
     * Display the error page
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showError(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        JSPManager.showJSP(request, response, "/browse/error.jsp");
    }

    /**
     * Display the No Results page
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showNoResultsPage(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        
        JSPManager.showJSP(request, response, "/browse/no-results.jsp");
    }

    /**
     * Display the single page.  This is the page which lists just the single values of a 
     * metadata browse, not individual items.  Single values are links through to all the items
     * that match that metadata value
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showSinglePage(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        
        JSPManager.showJSP(request, response, "/browse/single.jsp");
    }

    /**
     * Display a full item listing.
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showFullPage(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        
        JSPManager.showJSP(request, response, "/browse/full.jsp");
    }

    /**
     * Export the metadata from a browse
     *
     * @param context The DSpace context
     * @param request The request object
     * @param response The response object
     * @param scope The browse scope
     * @throws IOException
     * @throws ServletException
     */
    protected void exportMetadata(Context context, HttpServletRequest request,
                                  HttpServletResponse response, BrowserScope scope)
            throws IOException, ServletException
    {
        try
        {
            // Log the attempt
            log.info(LogManager.getHeader(context, "metadataexport", "exporting_browse"));

            // Ensure we export all results
            scope.setOffset(0);
            scope.setResultsPerPage(Integer.MAX_VALUE);

            // Export a browse view
            BrowseEngine be = new BrowseEngine(context);
            BrowseInfo binfo = be.browse(scope);
            ArrayList iids = new ArrayList();
            for (BrowseItem bi : binfo.getBrowseItemResults())
            {
                iids.add(bi.getID());
            }
            ItemIterator ii = new ItemIterator(context, iids);
            MetadataExport exporter = new MetadataExport(context, ii, false);

            // Perform the export
            DSpaceCSV csv = exporter.export();

            // Return the csv file
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=browse-result.csv");
            PrintWriter out = response.getWriter();
            out.write(csv.toString());
            out.flush();
            out.close();
            log.info(LogManager.getHeader(context, "metadataexport", "exported_file:browse-results.csv"));
            return;
        }
        catch (BrowseException be)
        {
            // Not sure what happended here!
            JSPManager.showIntegrityError(request, response);
        }
    }
}
