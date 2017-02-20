/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;


import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class ProfileResearcherNetworkServlet extends DSpaceServlet {
	
	
	/** log4j category */
	private static Logger log = Logger
			.getLogger(ProfileResearcherNetworkServlet.class);

	private DSpace dspace = new DSpace();
	private VisualizationGraphSolrService service = dspace.getServiceManager()
	.getServiceByName("visualNetworkSolrService",
			VisualizationGraphSolrService.class);
	
	public static Pattern patternRP = Pattern.compile("rp[0-9]{5}$");
	
	@Override
	protected void doDSGet(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException {
		Researcher util = new Researcher();

		ApplicationService applicationService = util.getApplicationService();
		CrisSearchService searchService = (CrisSearchService) util
				.getCrisSearchService();
		
		
		String idString = request.getPathInfo();
		String[] pathInfo = idString.split("/", 2);
		String authority = pathInfo[1];

		String target = request.getParameter("target");
		String depth = request.getParameter("depth");
		String root = request.getParameter("root");
		String typo = request.getParameter("typo");
		
		Pattern patt = Pattern.compile("rp[0-9]{5}$");
		if (root == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		String focus = authority;
		if(!root.equals(authority)) {
			focus = root;
		}
	
		Matcher matcher1 = patt.matcher(focus);
		if(matcher1.find()) {
		    typo = "rp";
		    request.setAttribute("fullname", ResearcherPageUtils.getFullName(focus));
		}
		
		ResearcherPage researcherTarget = new ResearcherPage();
		String authorityTarget = target;
		if (target != null) {
			
			Matcher matcher = patt.matcher(target);
			if (matcher.find()) {
				researcherTarget = applicationService
						.getResearcherByAuthorityKey(target);

			} else {
				researcherTarget.setInternalRP(false);
				researcherTarget.setFullName(target);
			}
		}
				
		request.setAttribute("researchertarget", researcherTarget);
		request.setAttribute("authoritytarget", authorityTarget);
		request.setAttribute("authority", focus);
		Map<String,Integer> relations = getRelationsInformation(focus, authorityTarget);
		request.setAttribute("relations",relations);
		request.setAttribute("depth",depth);
		request.setAttribute("typo",typo);
		
		if(!focus.equals(authority) && false) { //disabled programmatically
			
			JSPManager.showJSP(request, response, "/graph/profilefragmenttwice.jsp");
		}
		else {
			JSPManager.showJSP(request, response, "/graph/profilefragment.jsp");
		}

	}

	private Map<String,Integer> getRelationsInformation(String from, String to) {

		Map<String,Integer> result = null;
		
				try{
				result = getRelations(from, to);
			} catch (Exception e) {
				log.error(e.getMessage(), e);				
			}

		
		return result;
	}
	
	
	public Map<String,Integer> getRelations(String from, String to) throws SearchServiceException {
		Map<String,Integer> result = new HashMap<String, Integer>();
		SolrQuery solrQuery = new SolrQuery();
		
		
        Matcher matcher = patternRP.matcher(from);
        String field1 = "";
        String field2 = "";
        
        if (matcher.find())
        {                            
        	field1 = "a_auth";
        }
        else
        {
        	field1 = "a_val";
        }
        
        matcher = patternRP.matcher(to);
        

        if (matcher.find())
        {                            
        	field2 = "b_auth";
        }
        else
        {
        	field2 = "b_val";
        }
        
        
		solrQuery.setQuery(
				
				"("+field1 + ":\"" + from + "\" AND "+ field2 + ":\""+ to +"\"" + ") OR ("+field2 + ":\"" + from + "\" AND "+ field1 + ":\""+ to +"\"" + ")"				
								
		);
		solrQuery.addFilterQuery("entity:" + ConstantNetwork.ENTITY_RP);
		solrQuery.setFacet(true);
		solrQuery.addFacetField("type");
		solrQuery.setFacetLimit(Integer.MAX_VALUE);
		solrQuery.setFacetMinCount(1);
		solrQuery.setRows(0);

		QueryResponse rsp = service.search(solrQuery);

		FacetField facets = rsp.getFacetField("type");
		for(Count facet : facets.getValues()) {
			result.put(facet.getName(), Integer.valueOf((int)facet.getCount()));			
		}		
		return result;
	}
}
