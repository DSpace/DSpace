package org.ssu.service;

import org.dspace.app.webui.servlet.AbstractBrowserServlet;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.core.Context;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class BrowseContext extends AbstractBrowserServlet {
    @Override
    protected void showError(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {

    }

    @Override
    protected void showNoResultsPage(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {

    }

    @Override
    protected void showSinglePage(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {

    }

    @Override
    protected void showFullPage(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
    }

    public BrowseInfo getBrowseInfo(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        BrowserScope scope = getBrowserScopeForRequest(context, request, response);

        if (scope == null || scope.getBrowseIndex() == null)
        {
            String requestURL = request.getRequestURI();
            if (request.getQueryString() != null)
            {
                requestURL += "?" + request.getQueryString();
            }

            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        else
        {
            // execute browse request
            processBrowse(context, scope, request, response);
            return (BrowseInfo)request.getAttribute("browse.info");
        }
    }
}
