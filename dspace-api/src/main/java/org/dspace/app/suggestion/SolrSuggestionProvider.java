/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import static org.dspace.app.suggestion.SolrSuggestionStorageService.ABSTRACT;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.CATEGORY;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.CONTRIBUTORS;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.DATE;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.DISPLAY;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.EXTERNAL_URI;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.PROCESSED;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.SOURCE;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.SUGGESTION_ID;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.TARGET_ID;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.TITLE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Suggestion provider that read the suggestion from the local suggestion solr
 * core
 *
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 *
 */
public class SolrSuggestionProvider implements SuggestionProvider {
    protected SolrClient solrSuggestionClient;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SolrSuggestionStorageService solrSuggestionStorageService;

    private String sourceName;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Get sorl client which use suggestion core
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
    public long countAllTargets(Context context) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(SOURCE + ":" + sourceName);
        solrQuery.addFilterQuery(PROCESSED + ":false");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TARGET_ID);
        solrQuery.setFacetLimit(Integer.MAX_VALUE);
        QueryResponse response;
        try {
            response = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return response.getFacetField(TARGET_ID).getValueCount();
    }

    @Override
    public long countSuggestionByTarget(Context context, UUID target) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
                SOURCE + ":" + sourceName,
                TARGET_ID + ":" + target.toString(),
                PROCESSED + ":false");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            return response.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Suggestion> findAllSuggestions(Context context, UUID target, int pageSize, long offset) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(pageSize);
        solrQuery.setStart((int) offset);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
                SOURCE + ":" + sourceName,
                TARGET_ID + ":" + target.toString(),
                PROCESSED + ":false");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            List<Suggestion> suggestions = new ArrayList<Suggestion>();
            for (SolrDocument solrDoc : response.getResults()) {
                suggestions.add(convertSolrDoc(context, solrDoc));
            }
            return suggestions;
        } catch (SolrServerException | IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SuggestionTarget> findAllTargets(Context context, int pageSize, long offset) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(SOURCE + ":" + sourceName);
        solrQuery.addFilterQuery(PROCESSED + ":false");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(TARGET_ID);
        solrQuery.setFacetLimit((int) (pageSize + offset));
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            FacetField facetField = response.getFacetField(TARGET_ID);
            List<SuggestionTarget> suggestionTargets = new ArrayList<SuggestionTarget>();
            int idx = 0;
            for (Count c : facetField.getValues()) {
                if (idx < offset) {
                    idx++;
                    continue;
                }
                SuggestionTarget target = new SuggestionTarget();
                target.setSource(sourceName);
                target.setTotal((int) c.getCount());
                target.setTarget(itemService.find(context, UUID.fromString(c.getName())));
                suggestionTargets.add(target);
                idx++;
            }
            return suggestionTargets;
        } catch (SolrServerException | IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Suggestion findSuggestion(Context context, UUID target, String id) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(1);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
                SOURCE + ":" + sourceName,
                TARGET_ID + ":" + target.toString(),
                SUGGESTION_ID + ":\"" + id + "\"",
                PROCESSED + ":false");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            for (SolrDocument solrDoc : response.getResults()) {
                return convertSolrDoc(context, solrDoc);
            }
        } catch (SolrServerException | IOException | SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public SuggestionTarget findTarget(Context context, UUID target) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(SOURCE + ":" + sourceName);
        solrQuery.addFilterQuery(TARGET_ID + ":" + target.toString(),
                PROCESSED + ":false");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            SuggestionTarget sTarget = new SuggestionTarget();
            sTarget.setSource(sourceName);
            sTarget.setTotal((int) response.getResults().getNumFound());
            Item itemTarget = itemService.find(context, target);
            if (itemTarget != null) {
                sTarget.setTarget(itemTarget);
            } else {
                return null;
            }
            return sTarget;
        } catch (SolrServerException | IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rejectSuggestion(Context context, UUID target, String idPart) {
        Suggestion suggestion = findSuggestion(context, target, idPart);
        try {
            solrSuggestionStorageService.flagSuggestionAsProcessed(suggestion);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Suggestion convertSolrDoc(Context context, SolrDocument solrDoc) throws SQLException {
        Suggestion suggestion = new Suggestion(sourceName,
                itemService.find(context, UUID.fromString((String) solrDoc.getFieldValue(TARGET_ID))),
                (String) solrDoc.getFieldValue(SUGGESTION_ID));
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
        return suggestion;
    }

}
