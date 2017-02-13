package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.statistics.SolrLogger;

public class StatsCleaner extends DSpaceServlet {
	protected HttpSolrServer solrServer;
	
	Logger log = Logger.getLogger(StatsCleaner.class);

	public StatsCleaner() {
		solrServer = new HttpSolrServer(ConfigurationManager.getProperty(SolrLogger.CFG_STAT_MODULE, "server"));
	}

	@Override
	protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException, AuthorizeException {
		doDSPost(context, request, response);
	}

	@Override
	protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException, AuthorizeException {

		boolean canClean = ConfigurationManager.getBooleanProperty("usage-statistics","webui.statistics.showCleaner");
		
		if(!canClean){
			throw new AuthorizeException(); 
		}
		try {
		String name = "userAgent";
		String deleteUserAgent ="";
		String dns = request.getParameter("dns");
		String domainDns = request.getParameter("domaindns");
		if (StringUtils.isNotBlank(dns)) {
			name = "dns";
			deleteUserAgent = dns;
		}else if(StringUtils.isNotBlank(domainDns)){
			name = "domaindns";
			deleteUserAgent = domainDns;
		}else{
			 deleteUserAgent = request.getParameter(name);
		}
		
		if (StringUtils.isNotBlank(deleteUserAgent)) {
			QueryResponse resp = solrServer.query(new SolrQuery(name+":\""+deleteUserAgent+"\""));
			System.out.println(name+":\""+deleteUserAgent+"\"");
			System.out.println(resp.getResults().getNumFound());
			solrServer.deleteByQuery(name+":\""+deleteUserAgent+"\"");
			solrServer.optimize();
			request.setAttribute("deleted", true);
		}
		
		String userAgentOffsetParam = request.getParameter("useragentoffset");
		int userAgentOffset = 0;
		SolrQuery solrQuery = new SolrQuery("*:*");
		solrQuery.setFacet(true);
		if(StringUtils.isNotBlank(userAgentOffsetParam)){
			solrQuery.setParam("facet.offset", userAgentOffsetParam);
			userAgentOffset = Integer.parseInt(userAgentOffsetParam);
			request.setAttribute("previoususeragent", userAgentOffset - 100);	
		}
		solrQuery.addFacetField("userAgent");
		solrQuery.setFacetLimit(100);
		QueryResponse resp = solrServer.query(solrQuery);
		List<Count> counts = resp.getFacetField("userAgent").getValues();
		request.setAttribute("terms", counts);
		if(counts.size()==100){
			request.setAttribute("nextuseragent", userAgentOffset + 100);
		}


		String dnsOffsetParam = request.getParameter("dnsoffset");		
		int dnsOffset = 0;
		SolrQuery solrQueryDns = new SolrQuery("*:*");
		solrQueryDns.setFacet(true);
		if(StringUtils.isNotBlank(dnsOffsetParam)){
			solrQueryDns.setParam("facet.offset", dnsOffsetParam);
			dnsOffset = Integer.parseInt(dnsOffsetParam);
			request.setAttribute("previousdns", dnsOffset - 100);	
		}
		solrQueryDns.addFacetField("dns");
		solrQueryDns.setFacetLimit(100);
		QueryResponse respDns = solrServer.query(solrQueryDns);
		List<Count> countsDns = respDns.getFacetField("dns").getValues();
		request.setAttribute("dnsterms", countsDns);
		if(counts.size()==100){
			request.setAttribute("nextdns", dnsOffset + 100);
		}

		
		String domainDnsOffsetParam = request.getParameter("domaindnsoffset");		
		int domainDnsOffset = 0;
		SolrQuery solrQueryDomainDns = new SolrQuery("*:*");
		solrQueryDomainDns.setFacet(true);
		if(StringUtils.isNotBlank(dnsOffsetParam)){
			solrQueryDomainDns.setParam("facet.offset", domainDnsOffsetParam);
			domainDnsOffset = Integer.parseInt(domainDnsOffsetParam);
			request.setAttribute("previousdomaindns", domainDnsOffset - 100);	
		}
		solrQueryDomainDns.addFacetField("domaindns");
		solrQueryDomainDns.setFacetLimit(100);
		QueryResponse respDomainDns = solrServer.query(solrQueryDomainDns);
		List<Count> countsDomainDns = respDomainDns.getFacetField("domaindns").getValues();
		request.setAttribute("domaindnsterms", countsDomainDns);
		if(counts.size()==100){
			request.setAttribute("nextdomaindns", domainDnsOffset + 100);
		}

		JSPManager.showJSP(request, response, "/dspace-admin/stats-cleaner.jsp");
		}catch (SolrServerException e){
			log.error(e.getMessage(), e);
		}
		
		return;

	}
}

