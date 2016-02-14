/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;
import org.dspace.usage.UsageSearchEvent;

/**
 * Every time a user clicks on a search result he will be redirected through this servlet
 * this servlet will retrieve all query information & store this for the search statistics
 * Once everything has been stored the user will be
 * redirected to the dso he clicked on (indicated by the redirectUrl parameter)
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SearchResultLogServlet extends DSpaceServlet
{
	private final transient HandleService handleService
            = HandleServiceFactory.getInstance().getHandleService();

    @Override
    protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException, AuthorizeException {
        String redirectUrl = request.getParameter("redirectUrl");
        String scopeHandle = request.getParameter("scope");
        DSpaceObject scope = handleService.resolveToObject(context, scopeHandle);
        String resultHandle = StringUtils.substringAfter(redirectUrl, "/handle/");
        DSpaceObject result = handleService.resolveToObject(context, resultHandle);

        //Fire an event to log our search result
        UsageSearchEvent searchEvent = new UsageSearchEvent(
                UsageEvent.Action.SEARCH,
                request,
                context,
                result,
                Arrays.asList(request.getParameterValues("query")), scope);

        if(!StringUtils.isBlank(request.getParameter("rpp"))){
            searchEvent.setRpp(Integer.parseInt(request.getParameter("rpp")));
        }
        if(!StringUtils.isBlank(request.getParameter("sort_by"))){
            searchEvent.setSortBy(request.getParameter("sort_by"));
        }
        if(!StringUtils.isBlank(request.getParameter("order"))){
            searchEvent.setSortOrder(request.getParameter("order"));
        }
        if(!StringUtils.isBlank(request.getParameter("page"))){
            searchEvent.setPage(Integer.parseInt(request.getParameter("page")));
        }

        DSpaceServicesFactory.getInstance().getEventService().fireEvent(
                searchEvent);


        response.sendRedirect(redirectUrl);

    }
}
