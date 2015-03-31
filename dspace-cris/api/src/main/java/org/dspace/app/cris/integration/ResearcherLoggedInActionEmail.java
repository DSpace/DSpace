/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authenticate.PostLoggedInAction;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.eperson.EPerson;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
import org.mortbay.log.Log;

public class ResearcherLoggedInActionEmail implements PostLoggedInAction
{

	Logger log = Logger.getLogger(ResearcherLoggedInActionEmail.class);
    private ApplicationService applicationService;
    private static SearchService searcher;
    private String netidSourceRef;

    @Override
    public void loggedIn(Context context, HttpServletRequest request,
            EPerson eperson)
    {
        
        try
        {
            boolean save = false;
            ResearcherPage rp = applicationService.getResearcherPageByEPersonId(eperson.getID());
            
            if(rp==null) {
            	if(eperson.getNetid()!=null && 
            			(rp = applicationService.getEntityBySourceId(netidSourceRef,eperson.getNetid(), ResearcherPage.class))!=null){
	                if(rp.getEpersonID()!=null) {
	                    if (rp.getEpersonID() != eperson.getID())
	                    {
	                        rp.setEpersonID(eperson.getID());
	                        save = true;
	                    }
	                }
	                else {
	                    rp.setEpersonID(eperson.getID());
	                    save = true;
	                }
            	}
            	else{
            		
                    ServiceManager serviceManager = new DSpace().getServiceManager();
                    // System.out.println(serviceManager.getServicesNames());
                    searcher = serviceManager.getServiceByName(
                            SearchService.class.getName(), SearchService.class);
                    SolrQuery query = new SolrQuery();
                    query.setQuery("*:*");
                    String orcid = (String) request.getAttribute("orcid");
                    String filterQuery = "";
                    if(StringUtils.isNotBlank(orcid)){
                    	filterQuery= "crisrp.orcid:\""+orcid+"\"";
                    }else{
                    	filterQuery= "crisrp.email:\""+eperson.getEmail()+"\"";
                    }
                    query.addFilterQuery(filterQuery);
                    QueryResponse qResp = searcher.search(query);
                    SolrDocumentList docList = qResp.getResults();
                    if(docList.size()>=2){
                    	log.warn("Found two or more rp with filter query:" + filterQuery);
                    }else if(docList.size()==1){
                    	SolrDocument doc = docList.get(0);
                    	String rpKey = (String)doc.getFieldValue("objectpeople_authority");
                    	rp = applicationService.getResearcherByAuthorityKey(rpKey);
    	                if(rp.getEpersonID()!=null) {
    	                    if (rp.getEpersonID() != eperson.getID())
    	                    {
    	                        rp.setEpersonID(eperson.getID());
    	                        save = true;
    	                    }
    	                }
    	                else {
    	                    rp.setEpersonID(eperson.getID());
    	                    save = true;
    	                }                    	
                    }
            	}
            }
            
            if (save)
            {
                applicationService.saveOrUpdate(ResearcherPage.class, rp);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

	public String getNetidSourceRef() {
		return netidSourceRef;
	}

	public void setNetidSourceRef(String netidSourceRef) {
		this.netidSourceRef = netidSourceRef;
	}

}
