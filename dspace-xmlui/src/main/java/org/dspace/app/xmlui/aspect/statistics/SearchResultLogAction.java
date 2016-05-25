/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Action that fires a search event and redirects the user to the view page of
 * the object clicked on in the search results.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SearchResultLogAction extends AbstractAction {

    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);
        DSpaceObject result = HandleUtil.obtainHandle(objectModel);

        DSpaceObject scope = null;
        if(StringUtils.isNotBlank(request.getParameter("current-scope")))
        {
            scope = handleService.resolveToObject(context, request.getParameter("current-scope"));
        }

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

        return new HashMap();
    }
}
