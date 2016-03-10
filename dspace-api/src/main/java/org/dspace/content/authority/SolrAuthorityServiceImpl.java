/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthorityValue;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extended Solr Authority Service supporting Editable Authority Control.
 *
 * 1.) Adds AuthorityValues as Concepts in Appropriate Scheme.
 * 2.) AuthorityConceptEventConsumer assures they are indexed into Solr.
 * 3.) Query API provides Terms Completion and Lookup
 *
 * @author Lantian Gai, Mark Diggory, Kevin Van de Velde
 */
public class SolrAuthorityServiceImpl implements EditableAuthorityIndexingService, AuthoritySearchService {

    private static final Logger log = Logger.getLogger(SolrAuthorityServiceImpl.class);


    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private CommonsHttpSolrServer solr = null;


    /**
     * Non-Static Singelton instance of Configuration Service
     */
    private ConfigurationService configurationService;

    protected CommonsHttpSolrServer getSolr() throws MalformedURLException, SolrServerException {
        if (solr == null) {

            String solrService = ConfigurationManager.getProperty("solr.authority.server");

            log.debug("Solr authority URL: " + solrService);

            solr = new CommonsHttpSolrServer(solrService);
            solr.setBaseURL(solrService);

            SolrQuery solrQuery = new SolrQuery().setQuery("*:*");

            solr.query(solrQuery);
        }

        return solr;
    }


    /* This method will only be called from AuthorityIndexClient */
    public void indexContent(AuthorityValue value, boolean force) {


        String field = value.getField(); // ChoiceAuthorityManager.makeFieldKey(dcValue.schema,dcValue.element,dcValue.qualifier)
       /// Find concept and reindex it or make new concept and index it........

        String schemeId = ConfigurationManager.getProperty("solrauthority.searchscheme." + field);

        if(schemeId != null)
        {
            Context context = null;

            try
            {
                context = new Context();

                context.turnOffAuthorisationSystem();

                Scheme scheme = Scheme.findByIdentifier(context, schemeId);

                if (scheme!=null) {

                    Concept newConcept = null;

                    if(value.getId() != null)
                    {
                        List<Concept> newConcepts = Concept.findByIdentifier(context, value.getId());
                        if(newConcepts != null && newConcepts.size() > 0 && newConcepts.get(0).getPreferredLabel().equals(value.getValue()))
                            newConcept = newConcepts.get(0);
                    }
                    else
                    {
                        log.info("AuthorityValue:"+value.getId() +" has a unsaved concept :"+ value.getValue());
                    }

                    if(newConcept == null)
                    {
                        Concept newConcepts[] = Concept.findByPreferredLabel(context, value.getValue() ,scheme.getID());
                        if(newConcepts!=null && newConcepts.length>0){
                            newConcept = newConcepts[0];
                        }
                    }

                    if(newConcept==null){
                        newConcept = scheme.createConcept(context);
                        newConcept.setStatus(context, Concept.Status.ACCEPTED.name());
                        newConcept.setSource(context, value.getAuthorityType());
                        value.updateConceptFromAuthorityValue(context, newConcept);
                        Term term = newConcept.createTerm(context, value.getValue(),Term.prefer_term);
                        context.complete();
                    }

                }

            } catch (Exception e) {

                 log.error(e.getMessage(), e);

                if(context != null)
                {
                    context.abort();
                }

            }


        }
        ///internalIndexContent(value, force);

    }

    private void internalIndexContent(AuthorityValue value, boolean force) {
        SolrInputDocument doc = value.getSolrInputDocument();
        //TODO STILL NEED LOGIC TO ONLY UPDATE SOLR IF CONCEPT IS OLDER
        try{
            writeDocument(doc);
        }catch (Exception e){
            log.error("Error while writing authority value to the index: " + value.toString(), e);
        }
    }


    @Override
    /* this method is called from Consumer that update off concept changes. */
    public void indexContent(Context context, Concept concept, boolean force) throws SQLException {
        if(Concept.Status.ACCEPTED.name().equals(concept.getStatus()))
            internalIndexContent(AuthorityValue.fromConcept(concept), force);

    }

    /**
     * Unindex a Document in the Lucene Index.
     * @param context the dspace context
     * @param identifier the identifier of the object to be deleted
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    public void unIndexContent(Context context, String identifier, boolean commit)
            throws SQLException, IOException {

        try {
            getSolr().deleteById(identifier);
            if(commit)
            {
                getSolr().commit();
            }
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void unIndexContent(Context context, Concept concept) throws SQLException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cleanIndex() throws Exception {
        try{
            getSolr().deleteByQuery("*:*");
        } catch (Exception e){
            log.error("Error while cleaning authority solr server index", e);
            throw new Exception(e);
        }
    }

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
    public void updateIndex(Context context, boolean force) {
        //To change body of implemented methods use File | Settings | File Templates.

        try {
            Concept[] concepts = Concept.findAll(context, AuthorityObject.ID);
            try {
                for (Concept concept : concepts) {

                    indexContent(context, concept, force);

                }
            } catch (Exception e)
            {
                log.error(e.getMessage());
            }

            getSolr().commit();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void optimize() {
        try {
            long start = System.currentTimeMillis();
            System.out.println(this.getClass().getName() + " - Optimize -- Process Started:"+start);
            getSolr().optimize();
            long finish = System.currentTimeMillis();
            System.out.println(this.getClass().getName() + " - Optimize -- Process Finished:"+finish);
            System.out.println(this.getClass().getName() + " - Optimize -- Total time taken:"+(finish-start) + " (ms).");
        } catch (SolrServerException sse) {
            System.err.println(sse.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    /**
     * Write the document to the solr index
     * @param doc the solr document
     * @throws java.io.IOException
     */
    private void writeDocument(SolrInputDocument doc) throws IOException {

        try {
            getSolr().add(doc);
        } catch (Exception e) {
            try {
                log.error("An error occurred for document: " + doc.getField("id").getFirstValue() + ", source: " + doc.getField("source").getFirstValue() + ", field: " + doc.getField("field").getFirstValue() + ", full-text: " + doc.getField("full-text").getFirstValue());
            } catch (Exception e1) {
                //shouldn't happen
            }
            log.error(e.getMessage(), e);
        }
    }

    public QueryResponse search(SolrQuery query) throws SolrServerException, MalformedURLException {
        return getSolr().query(query);
    }

    /**
     * Retrieves all the metadata fields which are indexed in the authority control
     * @return a list of metadata fields
     */
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
