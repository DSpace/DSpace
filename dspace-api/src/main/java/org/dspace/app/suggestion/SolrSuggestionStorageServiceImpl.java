/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Service to deal with the local suggestion solr core used by the
 * SolrSuggestionProvider(s)
 *
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 *
 */
public class SolrSuggestionStorageServiceImpl implements SolrSuggestionStorageService {

    private static final Logger log = LogManager.getLogger(SolrSuggestionStorageServiceImpl.class);

    protected SolrClient solrSuggestionClient;

    @Autowired
    private ItemService itemService;

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
    public void addSuggestion(Suggestion suggestion, boolean force, boolean commit)
            throws SolrServerException, IOException {
        if (force || !exist(suggestion)) {
            ObjectMapper jsonMapper = new JsonMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            SolrInputDocument document = new SolrInputDocument();
            document.addField(SOURCE, suggestion.getSource());
            // suggestion id is written as concatenation of
            // source + ":" + targetID + ":" + idPart (of externalDataObj)
            String suggestionFullID = suggestion.getID();
            document.addField(SUGGESTION_FULLID, suggestionFullID);
            document.addField(SUGGESTION_ID, suggestionFullID.split(":", 3)[2]);
            document.addField(TARGET_ID, suggestion.getTarget().getID().toString());
            document.addField(DISPLAY, suggestion.getDisplay());
            document.addField(TITLE, getFirstValue(suggestion, "dc", "title", null));
            document.addField(DATE, getFirstValue(suggestion, "dc", "date", "issued"));
            document.addField(CONTRIBUTORS, getAllValues(suggestion, "dc", "contributor", "author"));
            document.addField(ABSTRACT, getFirstValue(suggestion, "dc", "description", "abstract"));
            document.addField(CATEGORY, getAllValues(suggestion, "dc", "source", null));
            document.addField(EXTERNAL_URI, suggestion.getExternalSourceUri());
            document.addField(SCORE, suggestion.getScore());
            document.addField(PROCESSED, false);
            document.addField(EVIDENCES, jsonMapper.writeValueAsString(suggestion.getEvidences()));
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
            .filter(st -> StringUtils.isNotBlank(st.getValue())
                && StringUtils.equals(st.getSchema(), schema)
                        && StringUtils.equals(st.getElement(), element)
                        && StringUtils.equals(st.getQualifier(), qualifier))
                .map(st -> st.getValue()).findFirst().orElse(null);
    }

    @Override
    public boolean exist(Suggestion suggestion) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(
                SUGGESTION_FULLID + ":\"" + suggestion.getID() + "\" AND " + PROCESSED + ":true");
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

    @Override
    public long countAllTargets(Context context, String source) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(SOURCE + ":" + source);
        solrQuery.addFilterQuery(PROCESSED + ":false");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TARGET_ID);
        solrQuery.setFacetLimit(Integer.MAX_VALUE);
        QueryResponse response = getSolr().query(solrQuery);
        return response.getFacetField(TARGET_ID).getValueCount();
    }

    @Override
    public long countUnprocessedSuggestionByTarget(Context context, String source, UUID target)
        throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
            SOURCE + ":" + source,
            TARGET_ID + ":" + target.toString(),
            PROCESSED + ":false");

        QueryResponse response = getSolr().query(solrQuery);
        return response.getResults().getNumFound();
    }

    @Override
    public List<Suggestion> findAllUnprocessedSuggestions(Context context, String source, UUID target,
        int pageSize, long offset, boolean ascending) throws SolrServerException, IOException {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(pageSize);
        solrQuery.setStart((int) offset);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
            SOURCE + ":" + source,
            TARGET_ID + ":" + target.toString(),
            PROCESSED + ":false");

        if (ascending) {
            solrQuery.addSort(SortClause.asc("trust"));
        } else {
            solrQuery.addSort(SortClause.desc("trust"));
        }

        solrQuery.addSort(SortClause.desc("date"));
        solrQuery.addSort(SortClause.asc("title"));

        QueryResponse response = getSolr().query(solrQuery);
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        for (SolrDocument solrDoc : response.getResults()) {
            suggestions.add(convertSolrDoc(context, solrDoc, source));
        }
        return suggestions;

    }

    @Override
    public List<SuggestionTarget> findAllTargets(Context context, String source, int pageSize, long offset)
        throws SolrServerException, IOException {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(SOURCE + ":" + source);
        solrQuery.addFilterQuery(PROCESSED + ":false");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TARGET_ID);
        solrQuery.setParam(FacetParams.FACET_OFFSET, String.valueOf(offset));
        solrQuery.setFacetLimit((int) (pageSize));
        QueryResponse response = getSolr().query(solrQuery);
        FacetField facetField = response.getFacetField(TARGET_ID);
        List<SuggestionTarget> suggestionTargets = new ArrayList<SuggestionTarget>();
        int idx = 0;
        for (Count c : facetField.getValues()) {
            SuggestionTarget target = new SuggestionTarget();
            target.setSource(source);
            target.setTotal((int) c.getCount());
            target.setTarget(findItem(context, c.getName()));
            suggestionTargets.add(target);
            idx++;
        }
        return suggestionTargets;

    }

    @Override
    public Suggestion findUnprocessedSuggestion(Context context, String source, UUID target, String id)
        throws SolrServerException, IOException {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(1);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
            SOURCE + ":" + source,
            TARGET_ID + ":" + target.toString(),
            SUGGESTION_ID + ":\"" + id + "\"",
            PROCESSED + ":false");

        SolrDocumentList results = getSolr().query(solrQuery).getResults();
        return isEmpty(results) ? null : convertSolrDoc(context, results.get(0), source);
    }

    @Override
    public SuggestionTarget findTarget(Context context, String source, UUID target)
        throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(SOURCE + ":" + source);
        solrQuery.addFilterQuery(
            TARGET_ID + ":" + target.toString(),
            PROCESSED + ":false");
        QueryResponse response = getSolr().query(solrQuery);
        SuggestionTarget sTarget = new SuggestionTarget();
        sTarget.setSource(source);
        sTarget.setTotal((int) response.getResults().getNumFound());
        Item itemTarget = findItem(context, target);
        if (itemTarget != null) {
            sTarget.setTarget(itemTarget);
        } else {
            return null;
        }
        return sTarget;
    }

    private Suggestion convertSolrDoc(Context context, SolrDocument solrDoc, String sourceName) {
        Item target = findItem(context, (String) solrDoc.getFieldValue(TARGET_ID));

        Suggestion suggestion = new Suggestion(sourceName, target, (String) solrDoc.getFieldValue(SUGGESTION_ID));
        suggestion.setDisplay((String) solrDoc.getFieldValue(DISPLAY));
        suggestion.getMetadata()
            .add(new MetadataValueDTO("dc", "title", null, null, (String) solrDoc.getFieldValue(TITLE)));
        suggestion.getMetadata()
            .add(new MetadataValueDTO("dc", "date", "issued", null, (String) solrDoc.getFieldValue(DATE)));
        suggestion.getMetadata().add(
            new MetadataValueDTO("dc", "description", "abstract", null, (String) solrDoc.getFieldValue(ABSTRACT)));

        suggestion.setExternalSourceUri((String) solrDoc.getFieldValue(EXTERNAL_URI));
        if (solrDoc.containsKey(CATEGORY)) {
            for (Object o : solrDoc.getFieldValues(CATEGORY)) {
                suggestion.getMetadata().add(
                    new MetadataValueDTO("dc", "source", null, null, (String) o));
            }
        }
        if (solrDoc.containsKey(CONTRIBUTORS)) {
            for (Object o : solrDoc.getFieldValues(CONTRIBUTORS)) {
                suggestion.getMetadata().add(
                    new MetadataValueDTO("dc", "contributor", "author", null, (String) o));
            }
        }
        String evidencesJson = (String) solrDoc.getFieldValue(EVIDENCES);
        ObjectMapper jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<SuggestionEvidence> evidences = new LinkedList<SuggestionEvidence>();
        try {
            evidences = jsonMapper.readValue(evidencesJson, new TypeReference<List<SuggestionEvidence>>() {});
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        suggestion.getEvidences().addAll(evidences);
        return suggestion;
    }

    private Item findItem(Context context, UUID itemId) {
        try {
            return itemService.find(context, itemId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Item findItem(Context context, String itemId) {
        return findItem(context, UUIDUtils.fromString(itemId));
    }
}
