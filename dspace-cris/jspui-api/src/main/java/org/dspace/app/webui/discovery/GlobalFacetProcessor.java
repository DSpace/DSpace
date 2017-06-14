/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.Hash;
import org.dspace.app.webui.discovery.GlobalFacetProcessorConfigurator.InnerGlobalFacetProcessorConfigurator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration.SORT_ORDER;
import org.dspace.plugin.CollectionHomeProcessor;
import org.dspace.plugin.CommunityHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;

public class GlobalFacetProcessor implements SiteHomeProcessor
{
    /** log4j category */
    private static Logger log = Logger.getLogger(GlobalFacetProcessor.class);

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
    	
    	Map<String, String> mapsFirstLevel = new HashMap<String, String>();
    	Map<String, String> mapsSecondLevel = new HashMap<String, String>();
    	//invert configuration from key:value to value:key (normally the value must be unique)
    	GlobalFacetProcessorConfigurator globalFacetProcessorConfigurator = new DSpace().getSingletonService(GlobalFacetProcessorConfigurator.class);
    	for(String key : globalFacetProcessorConfigurator.getGroups().keySet()) {
    		for(InnerGlobalFacetProcessorConfigurator innerGlobalFacetProcessorConfigurator : globalFacetProcessorConfigurator.getGroups().get(key)) {
    			mapsFirstLevel.put(innerGlobalFacetProcessorConfigurator.getName(), key);
    			if(innerGlobalFacetProcessorConfigurator.getSecondLevelFacet()!=null && !innerGlobalFacetProcessorConfigurator.getSecondLevelFacet().isEmpty()) {
    				for(String nameSecondLevel : innerGlobalFacetProcessorConfigurator.getSecondLevelFacet()) {
    					mapsSecondLevel.put(nameSecondLevel, innerGlobalFacetProcessorConfigurator.getName());
    				}
    			}
    		}
    	}
        DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(context,
                request, null, DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME, true);
        
        for(String keyMapSecondLevel : mapsSecondLevel.keySet()) {
			queryArgs.addFacetField(new DiscoverFacetField(keyMapSecondLevel,
					DiscoveryConfigurationParameters.TYPE_TEXT, -1, SORT.COUNT, false));
        }
        queryArgs.setMaxResults(0);
        DiscoverResult qResults;
        try
        {
            qResults = SearchUtils.getSearchService().search(context, scope,
                    queryArgs);
            request.setAttribute("discovery.global.fresults",
                    qResults.getFacetResults());
            DiscoveryConfiguration globalConfiguration = SearchUtils
                    .getGlobalConfiguration();
            
        	DiscoverySearchFilterFacet facet = new DiscoverySearchFilterFacet();
        	facet.setIndexFieldName(globalConfiguration.getCollapsingConfiguration().getGroupIndexFieldName());
            List<DiscoverySearchFilterFacet> availableFacet = new ArrayList<DiscoverySearchFilterFacet>();            
            availableFacet.addAll(globalConfiguration.getSidebarFacets());
            for(String keyMapSecondLevel : mapsSecondLevel.keySet()) {
            	DiscoverySearchFilterFacet facet2 = new DiscoverySearchFilterFacet();
            	facet2.setIndexFieldName(keyMapSecondLevel);
            	availableFacet.add(facet2);
            }
            availableFacet.add(facet);
            request.setAttribute("processorGlobal","global");
            request.setAttribute("facetGlobalName", globalConfiguration.getCollapsingConfiguration().getGroupIndexFieldName());
            request.setAttribute("facetGlobalFirstLevel", mapsFirstLevel);
            request.setAttribute("facetGlobalSecondLevel", mapsSecondLevel);
            request.setAttribute("facetsGlobalConfig",
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
