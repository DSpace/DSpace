/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.authority.SolrAuthority;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityValueFinder {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AuthorityValueFinder.class);

    /**
     * Item.ANY does not work here.
     */
    public AuthorityValue findByUID(Context context, String authorityID) {
        //Ensure that if we use the full identifier to match on
        String queryString = "id:\"" + authorityID + "\"";
        List<AuthorityValue> findings = find(context, queryString);
        return findings.size() > 0 ? findings.get(0) : null;
    }

    public List<AuthorityValue> findByExactValue(Context context, String field, String value) {
        String queryString = "value:\"" + value + "\" AND field:" + field;
        return find(context, queryString);
    }

    public List<AuthorityValue> findByValue(Context context, String field, String value) {
        String queryString = "value:" + value + " AND field:" + field;
        return find(context, queryString);
    }

    public List<AuthorityValue> findByValue(Context context, String schema, String element, String qualifier, String value) {
        String field = fieldParameter(schema, element, qualifier);
        return findByValue(context, field, qualifier);
    }

    public AuthorityValue findByOrcidID(Context context, String orcid_id) {
        String queryString = "orcid_id:" + orcid_id;
        List<AuthorityValue> findings = find(context, queryString);
        return findings.size() > 0 ? findings.get(0) : null;
    }

    public List<AuthorityValue> findByName(Context context, String schema, String element, String qualifier, String name) {
        String field = fieldParameter(schema, element, qualifier);
        String queryString = "first_name:" + name + " OR last_name:" + name + " OR name_variant:" + name + " AND field:" + field;
        return find(context, queryString);
    }

    public List<AuthorityValue> findByAuthorityMetadata(Context context, String schema, String element, String qualifier, String value) {
        String field = fieldParameter(schema, element, qualifier);
        String queryString = "all_Labels:" + value + " AND field:" + field;
        return find(context, queryString);
    }

    public List<AuthorityValue> findOrcidHolders(Context context) {
        String queryString = "orcid_id:*";
        return find(context, queryString);
    }

    public List<AuthorityValue> findAll(Context context) {
        String queryString = "*:*";
        return find(context, queryString);
    }

    private List<AuthorityValue> find(Context context, String queryString) {
        List<AuthorityValue> findings = new ArrayList<AuthorityValue>();
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(filtered(queryString));
            log.debug("AuthorityValueFinder makes the query: " + queryString);
            QueryResponse queryResponse = SolrAuthority.getSearchService().search(solrQuery);
            if (queryResponse != null && queryResponse.getResults() != null && 0 < queryResponse.getResults().getNumFound()) {
                for (SolrDocument document : queryResponse.getResults()) {
                    AuthorityValue authorityValue = AuthorityValue.fromSolr(document);
                    findings.add(authorityValue);
                    log.debug("AuthorityValueFinder found: " + authorityValue.getValue());
                }
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while retrieving AuthorityValue from solr", "query: " + queryString),e);
        }

        return findings;
    }

    private String filtered(String queryString) throws InstantiationException, IllegalAccessException {
        String instanceFilter = "-deleted:true";
        if (StringUtils.isNotBlank(instanceFilter)) {
            queryString += " AND " + instanceFilter;
        }
        return queryString;
    }

    private String fieldParameter(String schema, String element, String qualifier) {
        return schema + "_" + element + ((qualifier != null) ? "_" + qualifier : "");
    }


}
