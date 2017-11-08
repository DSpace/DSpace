/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * This SpringMVC controller has been added to handle RP details URL also with
 * the form:<br> 
 * [hub-url]/rp/rp/details.html?id=[rpidentifier]
 * <br>
 * doing a simple redirect to the canonical URL: [hub-url]/rp/[rpidentifier] 
 * 
 * @author cilea
 * 
 */
public class RedirectResearcherPageDetailsController extends
        ParameterizableViewController
{
	private SearchService searchService;
	
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
    /** log4j category */
    private static Logger log = Logger
            .getLogger(RedirectResearcherPageDetailsController.class);

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {        
        String paramRPId = request.getParameter("id");
        String auth = null;
        if (StringUtils.isBlank(paramRPId))
        {
            try
            {
                paramRPId = request.getParameter("crisid");
                if (StringUtils.isNotBlank(paramRPId))
                {
                	auth = paramRPId;    
                }
                else
                {
                	paramRPId = request.getParameter("sourceid");
                	
                	if (StringUtils.isNotBlank(paramRPId)) {
	                	String paramSourceRef = request.getParameter("sourceref");
	                	auth = ResearcherPageUtils.getRPIdentifierByStaffno(paramRPId, paramSourceRef);
                	}
                	else {
                		String value = request.getParameter("lv");
                		String key = request.getParameter("lt");
                		DiscoverQuery discoverQuery = new DiscoverQuery();
                		discoverQuery.addFilterQueries("search.resourcetype:9");
                		discoverQuery.setQuery("crisrp."+ClientUtils.escapeQueryChars(key)+":\""+
                		value.replaceAll("\"", "\\\"")
                				+ "\"");
                		Context context = UIUtil.obtainContext(request);
                		DiscoverResult result = searchService.search(context, discoverQuery);
                		if (result.getTotalSearchResults() == 1) {
                			auth = ((ResearcherPage) result.getDspaceObjects().get(0)).getCrisID();
                		}
                		System.out.println(discoverQuery.getQuery());
                		System.out.println(auth);
                	}
                }
                
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
                JSPManager.showInvalidIDError(request, response, paramRPId,
                        CrisConstants.RP_TYPE_ID);
            }
        }
        else
        {
            auth = ResearcherPageUtils.getPersistentIdentifier(
                    Integer.parseInt(paramRPId), ResearcherPage.class);
        }
        if (StringUtils.isBlank(auth))
        {
            // JSPManager.showInternalError(request, response);
            JSPManager.showInvalidIDError(request, response, paramRPId,
                    CrisConstants.RP_TYPE_ID);
        }
        return new ModelAndView("redirect:/cris/rp/" + auth);
    }

 
}
