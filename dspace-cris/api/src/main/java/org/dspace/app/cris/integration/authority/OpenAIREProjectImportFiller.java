/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.openaireproject.OpenAIREProjectService;
import org.dspace.authority.openaireproject.OpenAireProject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class OpenAIREProjectImportFiller extends ItemMetadataImportFiller {
	
	private static Logger log = Logger.getLogger(OpenAIREProjectImportFiller.class);
	
	//	private ApplicationService applicationService;
	private OpenAIREProjectService openAIREProjectService;
	private CrisSearchService searchService;
	
	public void setOpenAIREProjectService(OpenAIREProjectService openAIREProjectService) {
		this.openAIREProjectService = openAIREProjectService;
	}
	
	public void setSearchService(CrisSearchService searchService) {
		this.searchService = searchService;
	}
	
	@Override
	public void fillRecord(Context context, Item item, List<Metadatum> metadata, String openAireID, ACrisObject crisObject) {
		populateProject((Project) crisObject, openAireID);
		super.fillRecord(context, item, metadata, openAireID, crisObject);
	}
	
	private void populateProject(Project project,String id) {
		List<OpenAireProject> pjs = openAIREProjectService.getProjects(OpenAIREProjectService.QUERY_FIELD_ID, id, 0, 1);
		OpenAireProject openAireproject = null;
		if(pjs != null && pjs.size() > 0) {
			openAireproject = pjs.get(0);
		}

//		filler should not add title as this is done by authority
//		String title = openAireproject.getTitle();
//		if (StringUtils.isNotBlank(title)) {
//			log.warn("Title:" + title);
//			ResearcherPageUtils.buildTextValue(project, title, OpenAIREProjectService.PROJECT_TITLE);
//		}
		
		String fundingProgram = openAireproject.getFundingProgram();
		if (StringUtils.isNotBlank(fundingProgram)) {
			log.warn("FundingProgram:" + fundingProgram);
			ResearcherPageUtils.buildTextValue(project, fundingProgram, OpenAIREProjectService.PROJECT_FUNDING_PROGRAM);
		}
		
		String code = openAireproject.getCode();
		if (StringUtils.isNotBlank(code)) {
			log.warn("Code:" + code);
			ResearcherPageUtils.buildTextValue(project, code, OpenAIREProjectService.PROJECT_CODE);
		}
		
		String fundName = openAireproject.getFunder();
		SolrQuery solrQuery = new SolrQuery("crisou.name:\""+fundName.replaceAll("\"","\\\"")+"\"");
		solrQuery.addField("cris-id");
		solrQuery.addFilterQuery("search.resourcetype:" + CrisConstants.OU_TYPE_ID);
		solrQuery.setRows(1);
		try {
			QueryResponse response = searchService.search(solrQuery);
			SolrDocumentList results = response.getResults();
			if (results.getNumFound() == 1) {
				SolrDocument doc = results.get(0);
				String funderCrisID = (String) doc.getFieldValue("cris-id");
				log.warn("funderCrisID:" + funderCrisID);
				ResearcherPageUtils.buildGenericValue(project, funderCrisID, OpenAIREProjectService.PROJECT_FUNDER, 1);
			}
			else {
				OrganizationUnit funder = new OrganizationUnit();
				ResearcherPageUtils.buildTextValue(funder, fundName, OrganizationUnit.NAME, 1);
				applicationService.saveOrUpdate(OrganizationUnit.class, funder);
				ResearcherPageUtils.buildGenericValue(project, funder, OpenAIREProjectService.PROJECT_FUNDER, 1);
				log.info("funder:" + fundName);
			}
		} catch (SearchServiceException e) {
			log.warn(e.getMessage());
		}
		String openAireid= OpenAIREProjectService.OPENAIRE_INFO_PREFIX+fundName+"/"+fundingProgram+"/"+code;
		ResearcherPageUtils.buildTextValue(project, openAireid, OpenAIREProjectService.PROJECT_OPENAIRE_ID);
		applicationService.saveOrUpdate(Project.class, project);
	}
}
