/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.dspace.authority.indexer.AuthorityIndexingService;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.ConfigurationManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthoritySolrServiceImpl implements AuthorityIndexingService, AuthoritySearchService {

    private static final Logger log = Logger.getLogger(AuthoritySolrServiceImpl.class);

    protected AuthoritySolrServiceImpl()
    {

    }

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    protected HttpSolrServer solr = null;

    protected HttpSolrServer getSolr() throws MalformedURLException, SolrServerException {
        if (solr == null) {

            String solrService = ConfigurationManager.getProperty("solr.authority.server");

            log.debug("Solr authority URL: " + solrService);

            solr = new HttpSolrServer(solrService);
            solr.setBaseURL(solrService);

            SolrQuery solrQuery = new SolrQuery().setQuery("*:*");

            solr.query(solrQuery);
        }

        return solr;
    }

    @Override
    public void indexContent(AuthorityValue value, boolean force) {
        SolrInputDocument doc = value.getSolrInputDocument();

        try{
            writeDocument(doc);
        }catch (Exception e){
            log.error("Error while writing authority value to the index: " + value.toString(), e);
        }
    }

    @Override
    public void cleanIndex() throws Exception {
        try{
            getSolr().deleteByQuery("*:*");
        } catch (Exception e){
            log.error("Error while cleaning authority solr server index", e);
            throw new Exception(e);
        }
    }

    @Override
    public void commit() {
        try {
            getSolr().commit();
        } catch (SolrServerException e) {
            log.error("Error while committing authority solr server", e);
        } catch (IOException e) {
            log.error("Error while committing authority solr server", e);
        }
    }

    @Override
    public boolean isConfiguredProperly() {
        boolean solrReturn = false;
        try {
            solrReturn = (getSolr()!=null);
        } catch (Exception e) {
            log.error("Authority solr is not correctly configured, check \"solr.authority.server\" property in the dspace.cfg", e);
        }
        return solrReturn;
    }

    /**
     * Write the document to the solr index
     * @param doc the solr document
     * @throws IOException if IO error
     */
    protected void writeDocument(SolrInputDocument doc) throws IOException {

        try {
            getSolr().add(doc);
        } catch (Exception e) {
            try {
                log.error("An error occurred for document: " + doc.getField("id").getFirstValue() + ", source: " + doc.getField("source").getFirstValue() + ", field: " + doc.getField("field").getFirstValue() + ", full-text: " + doc.getField("full-text").getFirstValue(), e);
            } catch (Exception e1) {
                //shouldn't happen
            }
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public QueryResponse search(SolrQuery query) throws SolrServerException, MalformedURLException {
        return getSolr().query(query);
    }

    /**
     * Retrieves all the metadata fields which are indexed in the authority control
     * @return a list of metadata fields
     * @throws Exception if error
     */
    @Override
    public List<String> getAllIndexedMetadataFields() throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.addFacetField("field");

        QueryResponse response = getSolr().query(solrQuery);

        List<String> results = new ArrayList<String>();
        FacetField facetField = response.getFacetField("field");
        if(facetField != null){
            List<FacetField.Count> values = facetField.getValues();
            if(values != null){
                for (FacetField.Count facetValue : values) {
                    if (facetValue != null && facetValue.getName() != null) {
                        results.add(facetValue.getName());
                    }
                }
            }
        }
        return results;
    }
}
