/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.cris.integration.orcid.ORCIDWebHookCallbackProcessor;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.orcid.ns.record.Record;

/**
 * Servlet to receive an ORCID WebHook callback
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * 
 */
public class ORCIDWebHookCallbackServlet extends DSpaceServlet {
	/** Logger */
	private static Logger log = Logger.getLogger(ORCIDWebHookCallbackServlet.class);

	private List<ORCIDWebHookCallbackProcessor> processors = new DSpace().getServiceManager()
			.getServicesByType(ORCIDWebHookCallbackProcessor.class);

	private OrcidService orcidService = OrcidService.getOrcid();
	
	private ApplicationService applicationService = new Researcher().getApplicationService();
	
	private SearchService searchService = new DSpace().getServiceManager().getServiceByName(
            "org.dspace.discovery.SearchService", SearchService.class);
	
	protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException, AuthorizeException {
        // Get the ORCID from the URL
        String orcid = request.getPathInfo();

        if (orcid == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Remove leading slash if any:
        if (orcid.startsWith("/"))
        {
            orcid = orcid.substring(1);
        }

        final String[] split = orcid.split("/");
		if (split.length != 2) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
		}
		
		String secret = split[0];
		orcid = split[1];
		
		if (!secret.equalsIgnoreCase(ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook.secret"))) {
			if (ConfigurationManager.getBooleanProperty("authentication-oauth", "orcid-webhook.invalid.unregister")) {
        		orcidService.unregisterWebHook(secret, orcid);
        	}
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
		}
        
        // check if the orcid is syntatically valid
        if (!orcidService.isValid(orcid)) {
        	if (ConfigurationManager.getBooleanProperty("authentication-oauth", "orcid-webhook.invalid.unregister")) {
        		orcidService.unregisterWebHook(orcid);
        	}
        	response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        SolrQuery query = new SolrQuery("crisrp.orcid:\"" + orcid + "\"");
        query.addFilterQuery("search.resourcetype:"+CrisConstants.RP_TYPE_ID);
        QueryResponse resp = null;
		try {
			resp = searchService.search(query);
		} catch (SearchServiceException e) {
			log.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
        if (resp != null && resp.getResults().getNumFound() == 1) {
			ResearcherPage rp = applicationService.get(ResearcherPage.class,
					(Integer) resp.getResults().get(0).getFirstValue("search.resourceid"));
			
	        if (rp != null) {
	        	for (ORCIDWebHookCallbackProcessor processor : processors) {
	        		if (!processor.processChange(context, rp, orcid, request)) {
	        			context.abort();
	        			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        			return;
	        		}
	        	}
	        	context.complete();
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				return;
	        }
        }
        if (ConfigurationManager.getBooleanProperty("authentication-oauth", "orcid-webhook.invalid.unregister")) {
    		orcidService.unregisterWebHook(orcid);
    	}
    	response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
	}
}
