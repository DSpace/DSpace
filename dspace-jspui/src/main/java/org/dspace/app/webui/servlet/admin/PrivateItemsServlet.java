/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.webui.servlet.AbstractBrowserServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.sort.SortOption;
import org.apache.log4j.Logger;

/**
 * Servlet for browsing through private items:
 * 
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class PrivateItemsServlet extends AbstractBrowserServlet
{

    /** log4j category */
    private static Logger log = Logger.getLogger(PrivateItemsServlet.class);

    /**
     * Do the usual DSpace GET method.  You will notice that browse does not currently
     * respond to POST requests.
     */
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        try
        {
            // all browse requests currently come to GET.
            BrowserScope scope = getBrowserScopeForRequest(context, request, response);
    
            // Check that we are doing an item browse
            if (scope.getBrowseIndex() == null || scope.getBrowseIndex().isItemIndex())
            {
                // And override the index in the scope with the private items
                scope.setBrowseIndex(BrowseIndex.getPrivateBrowseIndex());
            }
            else
            {
                showError(context, request, response);
            }
            
            // execute browse request
            processBrowse(context, scope, request, response);
        }
        catch (BrowseException be)
        {
            log.error("caught exception: ", be);
            throw new ServletException(be);
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
        request.setAttribute("useAdminLayout", "yes");

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
        request.setAttribute("browsePrivate", "yes");
        
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
        // Show an error as this currently isn't supported
        showError(context, request, response);
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
        request.setAttribute("browsePrivate", "yes");
        
        JSPManager.showJSP(request, response, "/browse/full.jsp");
    }


}
