/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
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
 * @version $Revision$
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

        if (scope == null || scope.getBrowseIndex() == null)
        {
            String requestURL = request.getRequestURI();
            if (request.getQueryString() != null)
            {
                requestURL += "?" + request.getQueryString();
            }
            log.warn("We were unable to parse the browse request (e.g. an unconfigured index or sort option was used). Will send a 400 Bad Request. Requested URL was: " + requestURL);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
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
        JSPManager.showInternalError(request, response);
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
			Iterator<Item> iterator = binfo.getBrowseItemResults().iterator();
			MetadataExport exporter = new MetadataExport(context, iterator, false);

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
            // Not sure what happened here!
            JSPManager.showIntegrityError(request, response);
        }
    }
}
