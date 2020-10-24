/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Service to deal with the local suggestion solr core used by the
 * SolrSuggestionProvider(s)
 *
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 *
 */
public class SolrSuggestionStorageServiceImpl implements SolrSuggestionStorageService {
    protected SolrClient solrSuggestionClient;

    /**
     * Get solr client which use suggestion core
     * 
     * @return solr client
     */
    protected SolrClient getSolr() {
        if (solrSuggestionClient == null) {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getProperty("suggestion.solr.server", "http://localhost:8983/solr/suggestion");
            solrSuggestionClient = new HttpSolrClient.Builder(solrService).build();
        }
        return solrSuggestionClient;
    }

    @Override
    public void addSuggestion(Suggestion suggestion, boolean commit) throws SolrServerException, IOException {
        if (!exist(suggestion)) {
            SolrInputDocument document = new SolrInputDocument();
            document.addField(SOURCE, suggestion.getSource());
            String suggestionFullID = suggestion.getID();
            document.addField(SUGGESTION_FULLID, suggestionFullID);
            document.addField(SUGGESTION_ID, suggestionFullID.substring(suggestionFullID.lastIndexOf(":") + 1));
            document.addField(TARGET_ID, suggestion.getTarget().getID().toString());
            document.addField(DISPLAY, suggestion.getDisplay());
            document.addField(TITLE, getFirstValue(suggestion, "dc", "title", null));
            document.addField(DATE, getFirstValue(suggestion, "dc", "date", "issued"));
            document.addField(CONTRIBUTORS, getAllValues(suggestion, "dc", "contributor", "author"));
            document.addField(ABSTRACT, getFirstValue(suggestion, "dc", "description", "abstract"));
            document.addField(CATEGORY, getAllValues(suggestion, "dc", "source", null));
            document.addField(EXTERNAL_URI, suggestion.getExternalSourceUri());
            document.addField(PROCESSED, false);
            getSolr().add(document);
            if (commit) {
                getSolr().commit();
            }
        }
    }

    @Override
    public void commit() throws SolrServerException, IOException {
        getSolr().commit();
    }

    private List<String> getAllValues(Suggestion suggestion, String schema, String element, String qualifier) {
        return suggestion.getMetadata().stream()
                .filter(st -> StringUtils.isNotBlank(st.getValue()) && StringUtils.equals(st.getSchema(), schema)
                        && StringUtils.equals(st.getElement(), element)
                        && StringUtils.equals(st.getQualifier(), qualifier))
                .map(st -> st.getValue()).collect(Collectors.toList());
    }

    private String getFirstValue(Suggestion suggestion, String schema, String element, String qualifier) {
        return suggestion.getMetadata().stream()
                .filter(st -> StringUtils.isNotBlank(st.getValue()) && StringUtils.equals(st.getSchema(), schema)
                        && StringUtils.equals(st.getElement(), element)
                        && StringUtils.equals(st.getQualifier(), qualifier))
                .map(st -> st.getValue()).findFirst().orElse(null);
    }

    @Override
    public boolean exist(Suggestion suggestion) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(SUGGESTION_FULLID + ":\"" + suggestion.getID() + "\"");
        return getSolr().query(query).getResults().getNumFound() == 1;
    }

    @Override
    public void deleteSuggestion(Suggestion suggestion) throws SolrServerException, IOException {
        getSolr().deleteById(suggestion.getID());
        getSolr().commit();
    }

    @Override
    public void flagSuggestionAsProcessed(Suggestion suggestion) throws SolrServerException, IOException {
        SolrInputDocument sdoc = new SolrInputDocument();
        sdoc.addField(SUGGESTION_FULLID, suggestion.getID());
        Map<String, Object> fieldModifier = new HashMap<>(1);
        fieldModifier.put("set", true);
        sdoc.addField(PROCESSED, fieldModifier); // add the map as the field value
        getSolr().add(sdoc);
        getSolr().commit();
    }

    @Override
    public void flagAllSuggestionAsProcessed(String source, String idPart) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(SOURCE + ":" + source + " AND " + SUGGESTION_ID + ":\"" + idPart + "\"");
        query.setRows(Integer.MAX_VALUE);
        query.setFields(SUGGESTION_FULLID);
        SolrDocumentList results = getSolr().query(query).getResults();
        if (results.getNumFound() > 0) {
            for (SolrDocument rDoc : results) {
                SolrInputDocument sdoc = new SolrInputDocument();
                sdoc.addField(SUGGESTION_FULLID, rDoc.getFieldValue(SUGGESTION_FULLID));
                Map<String, Object> fieldModifier = new HashMap<>(1);
                fieldModifier.put("set", true);
                sdoc.addField(PROCESSED, fieldModifier); // add the map as the field value
                getSolr().add(sdoc);
            }
        }
        getSolr().commit();
    }

    @Override
    public void deleteTarget(SuggestionTarget target) throws SolrServerException, IOException {
        getSolr().deleteByQuery(
                SOURCE + ":" + target.getSource() + " AND " + TARGET_ID + ":" + target.getTarget().getID().toString());
        getSolr().commit();
    }
}
