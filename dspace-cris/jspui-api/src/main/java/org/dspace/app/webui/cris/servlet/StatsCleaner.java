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
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
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
		SolrQuery solrQuery = new SolrQuery("*:*");
		solrQuery.addTermsField("userAgent");
		solrQuery.addTermsField("dns");
		solrQuery.addTermsField("domaindns");
		solrQuery.setTerms(true);
		solrQuery.setTermsLimit(100);
		solrQuery.setRequestHandler("/terms");
		QueryResponse resp = solrServer.query(solrQuery);
		List<Term> terms = resp.getTermsResponse().getTerms("userAgent");
		request.setAttribute("terms", terms);
		List<Term> dnsTerms = resp.getTermsResponse().getTerms("dns");
		request.setAttribute("dnsterms", dnsTerms);
		List<Term> domainDnsTerms = resp.getTermsResponse().getTerms("domaindns");
		request.setAttribute("domaindnsterms", domainDnsTerms);
		JSPManager.showJSP(request, response, "/dspace-admin/stats-cleaner.jsp");
		}catch (SolrServerException e){
			log.error(e.getMessage(), e);
		}
		
		return;

	}
}

