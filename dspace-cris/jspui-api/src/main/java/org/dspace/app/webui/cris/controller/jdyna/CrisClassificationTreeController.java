/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.dto.classificationtree.NodeDTO;
import org.dspace.app.cris.model.dto.classificationtree.StateDTO;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerDO;
import org.dspace.app.cris.service.ApplicationService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import flexjson.JSONSerializer;

//TODO
public class CrisClassificationTreeController extends ParameterizableViewController {
	
	private ApplicationService applicationService;
	
	private CrisSearchService searchService;

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Map<String, Object> model = new HashMap<String, Object>();
		
		String rootObjectId = request.getParameter("id");
        String treeObjectType = request.getParameter("type");
        String metadataBuilder = request.getParameter("builder");
        String method = request.getParameter("method");
        
        JSONSerializer serializer = new JSONSerializer();
        
        if(StringUtils.isNotEmpty(method)) {
        	
        	switch (method) {
			case "buildtree":
				
				DynamicPropertiesDefinition dpdMetadataBuilder = applicationService.get(DynamicPropertiesDefinition.class, Integer.parseInt(metadataBuilder));
				String shortNameMetadataBuilder = dpdMetadataBuilder.getShortName();
				
				List<NodeDTO> dto = new ArrayList<NodeDTO>();
				
	            SolrQuery query = new SolrQuery();
	            query.setQuery("*:*");
	            
	            String nameField = "cris"+treeObjectType+"."+treeObjectType+"name";
	            String metadataBuilderSolr = "cris"+treeObjectType+"."+shortNameMetadataBuilder+"_authority";
	            
	            query.addFilterQuery(nameField+":[* TO *]");      
				query.setFields("search.resourceid",nameField, metadataBuilderSolr);
	            query.setRows(Integer.MAX_VALUE);
				
	            QueryResponse responseSolr = searchService.search(query);
	            SolrDocumentList resultsSolr = responseSolr.getResults();
	            
	            String fieldInternalSearch = "cris"+treeObjectType+"."+"this_authority";
	            for (SolrDocument doc : resultsSolr)
	            {
	            	String authority = (String)doc.getFirstValue(metadataBuilderSolr);
	            	String nodeName= (String)doc.getFirstValue(nameField);
	            	Integer nodeId = (Integer)doc.getFirstValue("search.resourceid");
	            	
	            	SolrQuery queryInternal = new SolrQuery();
	            	queryInternal.setQuery(fieldInternalSearch+":"+authority);
		            QueryResponse responseInternalSolr = searchService.search(queryInternal);
		            SolrDocumentList resultsInternalSolr = responseInternalSolr.getResults();
		            
		            String parentId = null;
		            for (SolrDocument docInternal : resultsInternalSolr)
		            {
		            	Integer parentIdInteger = (Integer)docInternal.getFirstValue("search.resourceid");
		            	parentId = ""+parentIdInteger;
		            	break;		            	
		            }
		            
	            	NodeDTO node = new NodeDTO();
	            	node.setId(nodeId);
	            	if(parentId!=null) { 
	            		node.setParent(parentId);
	            	}
	            	else {
	            		node.setParent("#");
	            	}
	            	node.setText(nodeName);
	            	dto.add(node);
	            }
		
				serializer.exclude("class").exclude("state.class").rootName(null).deepSerialize(dto, response.getWriter());
				
				break;
        		
			case "filldropdownwithresearchobject":
		        List<ResearchObject> ros = applicationService.getResearchObjectByShortNameType(treeObjectType);
		        serializer.include("ID", "name").exclude("*").serialize(ros, response.getWriter());
		        break;
		        
			case "filldropdownwithmetadatadefinition":
		        List<DynamicPropertiesDefinition> dpd = applicationService.likePropertiesDefinitionsByShortName(DynamicPropertiesDefinition.class, treeObjectType);
		        List<DynamicPropertiesDefinition> results = new ArrayList<DynamicPropertiesDefinition>();
		        for(DynamicPropertiesDefinition dp : dpd) {
		        	if(dp.getRendering() instanceof WidgetPointerDO) {
		        		results.add(dp);
		        	}
		        }
		        serializer.include("id", "label").exclude("*").serialize(results, response.getWriter());
		        break;
		        
			default:
				break;
			}
	        
        }
        
        response.setContentType("application/json");
        return null;
	}
	
	
	public ApplicationService getApplicationService() {
		return applicationService;
	}

	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	public CrisSearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(CrisSearchService searchService) {
		this.searchService = searchService;
	}
	
}
