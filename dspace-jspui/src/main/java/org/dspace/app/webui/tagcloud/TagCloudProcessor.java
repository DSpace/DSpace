/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.tagcloud;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.discovery.DiscoverUtility;
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
import org.dspace.discovery.configuration.TagCloudConfiguration;
import org.dspace.plugin.CollectionHomeProcessor;
import org.dspace.plugin.CommunityHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

/**
 * @author kstamatis
 *
 */
public class TagCloudProcessor implements CollectionHomeProcessor,
		CommunityHomeProcessor, SiteHomeProcessor {

	/** log4j category */
    private static Logger log = Logger.getLogger(TagCloudProcessor.class);
    
	/**
	 * 
	 */
	public TagCloudProcessor() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.dspace.plugin.SiteHomeProcessor#process(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void process(Context context, HttpServletRequest request,
			HttpServletResponse response) throws PluginException,
			AuthorizeException {
		
		process(context, request, response, (DSpaceObject) null);
	}

	/* (non-Javadoc)
	 * @see org.dspace.plugin.CommunityHomeProcessor#process(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.content.Community)
	 */
	@Override
	public void process(Context context, HttpServletRequest request,
			HttpServletResponse response, Community community)
			throws PluginException, AuthorizeException {
		
		process(context, request, response, (DSpaceObject) community);
	}

	/* (non-Javadoc)
	 * @see org.dspace.plugin.CollectionHomeProcessor#process(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.content.Collection)
	 */
	@Override
	public void process(Context context, HttpServletRequest request,
			HttpServletResponse response, Collection collection)
			throws PluginException, AuthorizeException {
		
		process(context, request, response, (DSpaceObject) collection);
	}

	private void process(Context context, HttpServletRequest request,
            HttpServletResponse response, DSpaceObject scope)
    {
        DiscoverQuery queryArgs = DiscoverUtility.getTagCloudDiscoverQuery(context,
                request, scope, true);
        queryArgs.setMaxResults(0);
        DiscoverResult qResults;
        try
        {
            qResults = SearchUtils.getSearchService().search(context, scope,
                    queryArgs);
            request.setAttribute("tagcloud.fresults",
                    qResults.getFacetResults());
            DiscoveryConfiguration discoveryConfiguration = SearchUtils
                    .getDiscoveryConfiguration(scope);
            List<DiscoverySearchFilterFacet> availableFacet = discoveryConfiguration
                    .getTagCloudFacetConfiguration().getTagCloudFacets();
            
            request.setAttribute("tagCloudFacetsConfig",
                    availableFacet != null ? availableFacet
                            : new ArrayList<DiscoverySearchFilterFacet>());
            
            TagCloudConfiguration tagCloudConfiguration = discoveryConfiguration.getTagCloudFacetConfiguration().getTagCloudConfiguration();
            request.setAttribute("tagCloudConfig",tagCloudConfiguration);
            		
            if (scope !=null)
            {
                request.setAttribute("tagcloud.searchScope",
                        "/handle/" + scope.getHandle());
            }
        }
        catch (SearchServiceException e)
        {
            log.error(LogManager.getHeader(context,
                    "tagcloud-process", "scope=" + scope));
        }
    }
	
}
