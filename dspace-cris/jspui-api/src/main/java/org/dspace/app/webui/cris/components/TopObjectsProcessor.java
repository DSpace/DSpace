/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.components.RecentSubmissions;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewConfiguration;
import org.dspace.utils.DSpace;

public class TopObjectsProcessor implements ExploreProcessor {
	private String type;
	private DiscoveryViewConfiguration viewConfiguration;
	private String sortField;
	private String order;
	private List<String> extraInfo;
	private int num = 10;

	public void setType(String type) {
		this.type = type;
	}
	
	public void setViewConfiguration(DiscoveryViewConfiguration viewConfiguration) {
		this.viewConfiguration = viewConfiguration;
	}
	
	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public void setExtraInfo(List<String> extraInfo) {
		this.extraInfo = extraInfo;
	}

	public void setNum(int num) {
		this.num = num;
	}

	@Override
	public Map<String, Object> process(String configurationName, DiscoveryConfiguration discoveryConfiguration,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		Context context = UIUtil.obtainContext(request);
		DiscoverQuery query = new DiscoverQuery();
		for (String fq : discoveryConfiguration.getDefaultFilterQueries()) {
			query.addFilterQueries(fq);
		}
		query.setMaxResults(num);
		query.addSearchField("search.resourceid");
		query.addSearchField("search.resourcetype");
		if (extraInfo != null) {
			for (String ei : extraInfo) {
				query.addSearchField(ei);
			}
		}
		query.setSortField(sortField, "ASC".equalsIgnoreCase(order) ? SORT_ORDER.asc : SORT_ORDER.desc);
		
		SearchService searchService = new DSpace().getSingletonService(SearchService.class);
		
		DiscoverResult result = searchService.search(context, query);
		List<DSpaceObject> dsos = result.getDspaceObjects();
		if(dsos.isEmpty()) {
		    return null;
		}
		DSpaceObject[] dsoArray = new DSpaceObject[dsos.size()];
		dsoArray = dsos.toArray(dsoArray);
		
		RecentSubmissions rs = new RecentSubmissions(dsoArray);
		rs.setConfiguration(viewConfiguration);

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("top_"+type, rs);
		return model;
	}
}
