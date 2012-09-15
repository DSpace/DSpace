/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.discovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchUtils;

import flexjson.JSONSerializer;

public class DiscoveryJSONRequest extends JSONRequest
{

    public void doJSONRequest(Context context, HttpServletRequest request,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {
        String reqPath = request.getPathInfo();
        // remove the first slash if present
        if (reqPath.startsWith("/"))
        {
            reqPath = reqPath.substring(1);
        }

        if (reqPath.equalsIgnoreCase(getSubPath() + "/autocomplete"))
        {
            doAutocomplete(context, request, resp);
            return;
        }
        // unkwon action (in future we can implement ajax for pagination, etc.)
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    private void doAutocomplete(Context context, HttpServletRequest request,
            HttpServletResponse resp)
    {
        try
        {
            DSpaceObject scope = DiscoverUtility.getSearchScope(context,
                    request);
            DiscoverQuery autocompleteQuery = DiscoverUtility
                    .getDiscoverAutocomplete(context, request, scope);
            DiscoverResult qResults = SearchUtils.getSearchService().search(
                    context, autocompleteQuery);
            // extract the only facet present in the result response
            Set<String> facets = qResults.getFacetResults().keySet();
            List<FacetResult> fResults = new ArrayList<DiscoverResult.FacetResult>();
            if (facets != null && facets.size() > 0)
            {
                String autocompleteField = (String) facets.toArray()[0];
                fResults = qResults
                        .getFacetResult(autocompleteField);                
            }
            JSONSerializer serializer = new JSONSerializer();
            serializer.rootName("autocomplete");
            serializer.exclude("class","asFilterQuery");
            serializer.deepSerialize(fResults, resp.getWriter());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
