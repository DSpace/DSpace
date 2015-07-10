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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidQueue;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.AccountManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.PasswordHash;
import org.dspace.kernel.ServiceManager;
import org.dspace.submit.lookup.SubmissionLookupUtils;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.utils.DSpace;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gr.ekt.bte.core.Record;


/**
 * 
 * 
 * @author l.pascarelli
 *
 */
public class JSONOrcidQueueServlet extends JSONRequest{

	Logger log = Logger.getLogger(JSONOrcidQueueServlet.class);

	/* (non-Javadoc)
	 * @see org.dspace.app.webui.json.JSONRequest#doJSONRequest(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doJSONRequest(Context context, HttpServletRequest req,
			HttpServletResponse resp) throws AuthorizeException, IOException {

		Gson json = new Gson();
		String id = req.getParameter("id");
		
		ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName("applicationService", ApplicationService.class);
		List<OrcidQueue> queue = new ArrayList<OrcidQueue>(); 
		if(StringUtils.isNotBlank(id)){			
			queue = applicationService.findOrcidQueueByResearcherId(Integer.parseInt(id));
		}
		List<Map<String, Object>> dto = null;
		try {
			dto = getLightResultList(context, applicationService, queue);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
        
        JsonObject jo = new JsonObject();
        if(dto==null || dto.isEmpty()) {
        	jo.addProperty("status", false);	
        }
        else {
        	jo.addProperty("status", true);
        	JsonElement tree = json.toJsonTree(dto);
        	jo.add("result", tree);
        }        
        resp.setContentType("text/plain");
//        if you works in localhost mode and use IE10 to debug the feature uncomment the follow line
//        resp.setHeader("Access-Control-Allow-Origin","*");
        resp.getWriter().write(jo.toString());
	}
	
	
    private List<Map<String, Object>> getLightResultList(Context context, ApplicationService applicationService,
            List<OrcidQueue> records) throws SQLException
    {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        if (records != null && records.size() > 0)
        {
            for (OrcidQueue record : records)
            {
                Integer itemId = record.getItemId();
                Integer projectId = record.getProjectId();
                Integer rpId = record.getResearcherId();
                DSpaceObject dso = null;
                if(itemId != null) {
                	dso = Item.find(context, itemId);
                } else {
                    if(projectId != null) {
                    	dso = applicationService.get(Project.class, projectId);
                    } else {
                    	dso = applicationService.get(ResearcherPage.class, rpId);
                    }
                }
                 
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("id", record.getId());
                data.put("uuid", dso.getHandle());
                data.put("eId", dso.getID());
                data.put("tText", dso.getTypeText());
                data.put("name", dso.getName());
                data.put("mode", record.getMode());
                results.add(data);
            }
        }
        return results;
    }

}
