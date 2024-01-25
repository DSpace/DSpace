/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Suggestion provider that read the suggestion from the local suggestion solr
 * core
 *
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 *
 */
public abstract class SolrSuggestionProvider implements SuggestionProvider {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SolrSuggestionProvider.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected SolrSuggestionStorageService solrSuggestionStorageService;

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

    @Override
    public long countAllTargets(Context context) {
        try {
            return this.solrSuggestionStorageService.countAllTargets(context, sourceName);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countUnprocessedSuggestionByTarget(Context context, UUID target) {
        try {
            return this.solrSuggestionStorageService.countUnprocessedSuggestionByTarget(context, sourceName, target);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Suggestion> findAllUnprocessedSuggestions(Context context, UUID target, int pageSize, long offset,
            boolean ascending) {

        try {
            return this.solrSuggestionStorageService.findAllUnprocessedSuggestions(context, sourceName,
                target, pageSize, offset, ascending);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<SuggestionTarget> findAllTargets(Context context, int pageSize, long offset) {
        try {
            return this.solrSuggestionStorageService.findAllTargets(context, sourceName, pageSize, offset);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Suggestion findUnprocessedSuggestion(Context context, UUID target, String id) {
        try {
            return this.solrSuggestionStorageService.findUnprocessedSuggestion(context, sourceName, target, id);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SuggestionTarget findTarget(Context context, UUID target) {
        try {
            return this.solrSuggestionStorageService.findTarget(context, sourceName, target);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rejectSuggestion(Context context, UUID target, String idPart) {
        Suggestion suggestion = findUnprocessedSuggestion(context, target, idPart);
        try {
            solrSuggestionStorageService.flagSuggestionAsProcessed(suggestion);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flagRelatedSuggestionsAsProcessed(Context context, ExternalDataObject externalDataObject) {
        if (!isExternalDataObjectPotentiallySuggested(context, externalDataObject)) {
            return;
        }
        try {
            solrSuggestionStorageService.flagAllSuggestionAsProcessed(sourceName, externalDataObject.getId());
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * check if the externalDataObject may have suggestion
     * @param context
     * @param externalDataObject
     * @return true if the externalDataObject could be suggested by this provider
     *         (i.e. it comes from a DataProvider used by this suggestor)
     */
    protected abstract boolean isExternalDataObjectPotentiallySuggested(Context context,
            ExternalDataObject externalDataObject);
}
