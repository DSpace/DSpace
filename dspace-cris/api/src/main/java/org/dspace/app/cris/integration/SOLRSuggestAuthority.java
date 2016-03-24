/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class SOLRSuggestAuthority implements ChoiceAuthority {

    /** The logger */
    private static Logger log = Logger.getLogger(SOLRSuggestAuthority.class);
    
    private DSpace dspace = new DSpace();

	private SearchService searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(),
			SearchService.class);

	@Override
	public Choices getBestMatch(String field, String text, int collection, String locale) {
		return getMatches(field, text, collection, 0, 1, locale);
	}

	@Override
	public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
		String facetname = org.dspace.core.ConfigurationManager
				.getProperty(SOLRSuggestAuthority.class.getSimpleName() + "." + field+".facetname") + "_ac";
		SolrQuery sQuery = new SolrQuery("*:*");
		sQuery.setRows(0);
		sQuery.setFacet(true);
		sQuery.setFacetLimit(50);
		sQuery.addFacetField(facetname);
		sQuery.setFacetPrefix(facetname, text.toLowerCase());
		List<Choice> proposals = new ArrayList<Choice>();
		QueryResponse qResp;
		try {
			qResp = searchService.search(sQuery);
			FacetField facet = qResp.getFacetField(facetname);
			if (facet.getValueCount() > 0) {
				for (Count c : facet.getValues()) {
					String value = c.getName().split("\\|\\|\\|")[1];
					proposals.add(new Choice(null, value, value));
				}
			}
		} catch (SearchServiceException e) {
			log.error(e.getMessage(), e);
		}
		Choice[] propArray = new Choice[proposals.size()];
		propArray = proposals.toArray(propArray);
		Choices result = new Choices(propArray, 0, proposals.size(), Choices.CF_ACCEPTED, false);
		return result;
	}

	@Override
	public String getLabel(String field, String key, String locale) {
		return null;
	}
}
