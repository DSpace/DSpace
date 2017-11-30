/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.discovery;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.plugin.CollectionHomeProcessor;
import org.dspace.plugin.CommunityHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

public class SideBarFacetProcessor implements CollectionHomeProcessor,
        CommunityHomeProcessor, SiteHomeProcessor
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SideBarFacetProcessor.class);

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Community community)
            throws PluginException, AuthorizeException
    {
        process(context, request, response, (DSpaceObject) community);
    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Collection collection)
            throws PluginException, AuthorizeException
    {
        process(context, request, response, (DSpaceObject) collection);
    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response) throws PluginException,
            AuthorizeException
    {
        process(context, request, response, (DSpaceObject) null);
    }

    private void process(Context context, HttpServletRequest request,
            HttpServletResponse response, DSpaceObject scope)
    {
        DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(context,
                request, scope, true);
        queryArgs.setMaxResults(0);
        DiscoverResult qResults;
        try
        {
            qResults = SearchUtils.getSearchService().search(context, scope,
                    queryArgs);
            request.setAttribute("discovery.fresults",
                    qResults.getFacetResults());
            DiscoveryConfiguration discoveryConfiguration = SearchUtils
                    .getDiscoveryConfiguration(scope);
            List<DiscoverySearchFilterFacet> availableFacet = discoveryConfiguration
                    .getSidebarFacets();
            
            request.setAttribute("facetsConfig",
                    availableFacet != null ? availableFacet
                            : new ArrayList<DiscoverySearchFilterFacet>());
            if (scope !=null)
            {
                request.setAttribute("discovery.searchScope",
                        "/handle/" + scope.getHandle());
            }
        }
        catch (SearchServiceException e)
        {
            log.error(LogManager.getHeader(context,
                    "discovery-process-sidebar", "scope=" + scope));
        }
    }
}
