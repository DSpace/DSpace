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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.model.orcid.OrcidQueue;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * 
 * @author l.pascarelli
 *
 */
public class JSONOrcidQueueServlet extends JSONRequest {

	Logger log = Logger.getLogger(JSONOrcidQueueServlet.class);

	private OrcidPreferencesUtils orcidPreferencesUtils = new DSpace().getServiceManager().getServiceByName("orcidPreferencesUtils", OrcidPreferencesUtils.class);
	private ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName("applicationService",
			ApplicationService.class);	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dspace.app.webui.json.JSONRequest#doJSONRequest(org.dspace.core.
	 * Context, javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doJSONRequest(Context context, HttpServletRequest req, HttpServletResponse resp)
			throws AuthorizeException, IOException {

		Gson json = new Gson();
		String crisId = req.getParameter("id");

		JsonObject jo = new JsonObject();
		if (req.getPathInfo().contains("rest")) {
			String uuId = req.getParameter("uuid");
			String owner = req.getParameter("owner");
			boolean ok = false;
			//send to ORCID Registry
			if (req.getPathInfo().trim().endsWith("crisrp")) {
				ok = orcidPreferencesUtils.sendOrcidProfile(owner, uuId);
			} else if (req.getPathInfo().trim().endsWith("crispj")) {
				ok = orcidPreferencesUtils.sendOrcidFunding(owner, uuId);
			} else {
				ok = orcidPreferencesUtils.sendOrcidWork(owner, uuId);
			}
			if(ok) {				
				jo.addProperty("status", true);
			} else {
				jo.addProperty("status", false);
			}
		} else {
			List<OrcidQueue> queue = new ArrayList<OrcidQueue>();
			if (StringUtils.isNotBlank(crisId)) {
				queue = applicationService.findOrcidQueueByResearcherId(crisId);
			}
			List<Map<String, Object>> dto = null;
			try {
				dto = getLightResultList(context, applicationService, queue);
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
			
			if (dto == null || dto.isEmpty()) {
				dto = new ArrayList<Map<String, Object>>();
				jo.addProperty("status", false);
				JsonElement tree = json.toJsonTree(dto);
				jo.add("result", tree);
			} else {
				jo.addProperty("status", true);
				JsonElement tree = json.toJsonTree(dto);
				jo.add("result", tree);
			}
			jo.addProperty("iTotalRecords", queue.size());
			jo.addProperty("iTotalDisplayRecords", queue.size());
		}
		resp.setContentType("application/json");
		// if you works in localhost mode and use IE10 to debug the feature
		// uncomment the follow line
		// resp.setHeader("Access-Control-Allow-Origin","*");
		resp.getWriter().write(jo.toString());
	}

	private List<Map<String, Object>> getLightResultList(Context context, ApplicationService applicationService,
			List<OrcidQueue> records) throws SQLException {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		if (records != null && records.size() > 0) {
			for (OrcidQueue record : records) {
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("id", record.getId());
				data.put("uuid", record.getFastlookupUuid());
				data.put("eId", record.getEntityId());
				data.put("ttext", CrisConstants.getEntityTypeText(record.getTypeId()));
				data.put("name", record.getFastlookupObjectName());
				data.put("owner", record.getOwner());
				data.put("mode", record.getMode());
				results.add(data);
			}
		}
		return results;
	}

}
