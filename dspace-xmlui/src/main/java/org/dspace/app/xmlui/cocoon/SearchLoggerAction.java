/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;
import org.dspace.usage.UsageSearchEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Fires an event each time a search occurs
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class SearchLoggerAction extends AbstractAction {

    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);
        DSpaceObject scope = getScope(context, objectModel);

        UsageSearchEvent searchEvent = new UsageSearchEvent(
                UsageEvent.Action.SEARCH,
                request,
                context,
                null, getQueries(request), scope);

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

        //Fire our event
        DSpaceServicesFactory.getInstance().getEventService().fireEvent(searchEvent);


        // Finished, allow to pass.
        return null;
    }

    protected abstract List<String> getQueries(Request request) throws SQLException;


    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @param context session context.
     * @param objectModel Cocoon object model.
     * @return The current scope.
     * @throws java.sql.SQLException passed through.
     */
    protected DSpaceObject getScope(Context context, Map objectModel) throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeString = request.getParameter("scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
        {
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        }
        else
        {
            // Get the search scope from the location parameter
            dso = handleService.resolveToObject(context, scopeString);
        }

        return dso;
    }

}
