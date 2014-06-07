/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import it.cilea.osd.jdyna.model.IContainable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.app.webui.cris.dto.ExportParametersDTO;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller is responsible to handle request of export
 * 
 * @author cilea
 * 
 */
public class ExportFormController extends BaseFormController {

	private static final DateFormat dateFormat = new SimpleDateFormat(
			"dd-MM-yyyy HH:mm");

	private CrisSearchService searchService;
	
	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		
		Map<String, Object> map =  new HashMap<String, Object>();
		map.put("tabs", applicationService.getList(TabResearcherPage.class));
		return map;
	}
	
	@Override
	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		Context context = UIUtil.obtainContext(request);
		if (!AuthorizeManager.isAdmin(context)) {
			throw new AuthorizeException(
					"Only system administrator can access to the export functionality");
		}
		return super.formBackingObject(request);
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		ExportParametersDTO exportParameters = (ExportParametersDTO) command;

		List<String> f = new LinkedList<String>();
		List<String> q = new LinkedList<String>();
		addToTempQuery("names", exportParameters.getNames(), f, q,
				!exportParameters.getAdvancedSyntax());

		addToTempQuery("dept", exportParameters.getDept(), f, q,
				!exportParameters.getAdvancedSyntax());

		addToTempQuery("interests", exportParameters.getInterests(), f, q,
				!exportParameters.getAdvancedSyntax());

		addToTempQuery("media", exportParameters.getMedia(), f, q,
				!exportParameters.getAdvancedSyntax());

		String[] temp_query = new String[] {};
		temp_query = q.toArray(temp_query);
		String[] temp_fields = new String[] {};
		temp_fields = f.toArray(temp_fields);
		List<ResearcherPage> list = null;
		try {
			//TODO
//			list = searchService.search(temp_fields, temp_query,
//					exportParameters.getStatus(),
//					exportParameters.getCreationStart(),
//					exportParameters.getCreationEnd(),
//					exportParameters.getStaffNoStart(),
//					exportParameters.getStaffNoEnd(),
//					exportParameters.getRpIdStart(),
//					exportParameters.getRpIdEnd(),
//					exportParameters.getDefaultOperator(), ResearcherPage.class);
		} catch (Exception e) {
			errors.reject("jsp.layout.hku.export.validation.notvalid.query");
			return showForm(request, errors, getFormView());

		}

		// export all tab
		List<IContainable> containables = new LinkedList<IContainable>();
		if (exportParameters.getTabToExport() == null || exportParameters.getTabToExport().isEmpty()) {
			containables = applicationService
			.findAllContainables(RPPropertiesDefinition.class);
		} else {
			for(Integer tab : exportParameters.getTabToExport()) {
				for(BoxResearcherPage box : applicationService.<BoxResearcherPage, TabResearcherPage>findPropertyHolderInTab(TabResearcherPage.class, tab)) {
					containables.addAll(box.getMask());
					applicationService.findOtherContainablesInBoxByConfiguration(box.getShortName(), containables);
				}
			}
		}

		if (exportParameters.getMainMode() == null) {
			response.setContentType("application/excel");
			response.addHeader("Content-Disposition",
					"attachment; filename=rpdata.xls");
			ImportExportUtils.exportData(list, applicationService,
					response.getOutputStream(), containables);
			response.getOutputStream().flush();
			response.getOutputStream().close();
		} else {
			response.setContentType("application/xml;charset=UTF-8");
			response.addHeader("Content-Disposition",
					"attachment; filename=rpdata.xml");
	        ImportExportUtils.exportXML(response.getWriter(),
	                    applicationService, containables,
	                    list);
			response.getWriter().flush();
			response.getWriter().close();
		}

		return null;
	}

	private void addToTempQuery(String fieldName, String value, List<String> f,
			List<String> q, boolean escape) {
		if (StringUtils.isNotBlank(value)) {
			q.add(escape ? ClientUtils.escapeQueryChars(value) : value);
			f.add(fieldName);
		}
	}

	public CrisSearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(CrisSearchService searchService) {
		this.searchService = searchService;
	}
}
