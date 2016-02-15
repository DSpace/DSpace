/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.cris.components.ExploreMapProcessors;
import org.dspace.app.webui.cris.components.ExploreProcessor;
import org.dspace.app.webui.discovery.DiscoverUtility;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

import it.cilea.osd.common.controller.BaseAbstractController;

public class ExploreController extends BaseAbstractController {

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();

		String configurationName = request.getPathInfo().substring("/explore/".length());
		Context context = UIUtil.obtainContext(request);

		DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfigurationByName(configurationName);


        DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(context,
                request, null, configurationName, true);
        queryArgs.setMaxResults(0);
        DiscoverResult qResults;

        qResults = SearchUtils.getSearchService().search(context, null,
                queryArgs);
        request.setAttribute("discovery.fresults",
                qResults.getFacetResults());

        List<DiscoverySearchFilterFacet> availableFacet = discoveryConfiguration
                .getSidebarFacets();
        request.setAttribute("location",configurationName);
        request.setAttribute("facetsConfig",
                availableFacet != null ? availableFacet
                        : new ArrayList<DiscoverySearchFilterFacet>());

        request.setAttribute("totalObjects", qResults.getTotalSearchResults());
        
        request.setAttribute("filters", discoveryConfiguration.getSearchFilters());

		ExploreMapProcessors processorsMap = new DSpace().getSingletonService(ExploreMapProcessors.class);
		if (processorsMap != null) {
			Map<String, List<ExploreProcessor>> procMap = processorsMap.getProcessorsMap();
			if (procMap != null) {
				List<ExploreProcessor> processors = procMap.get(configurationName);
		
				if (processors != null) {
					for (ExploreProcessor expProc : processors) {
						Map<String, Object> process = expProc.process(configurationName, discoveryConfiguration, request, response);
						if (process != null) {
							model.putAll(process);
						}
					}
				}
			}
		}
		return new ModelAndView(getDetailsView(), model);
	}

}
