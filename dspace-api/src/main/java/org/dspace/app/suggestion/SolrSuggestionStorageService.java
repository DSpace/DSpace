/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

/**
 * Service to deal with the local suggestion solr core used by the
 * SolrSuggestionProvider(s)
 *
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 *
 */
public interface SolrSuggestionStorageService {
    public static final String SOURCE = "source";
    /** This is the URI Part of the suggestion source:target:id */
    public static final String SUGGESTION_FULLID = "suggestion_fullid";
    public static final String SUGGESTION_ID = "suggestion_id";
    public static final String TARGET_ID = "target_id";
    public static final String TITLE = "title";
    public static final String DATE = "date";
    public static final String DISPLAY = "display";
    public static final String CONTRIBUTORS = "contributors";
    public static final String ABSTRACT = "abstract";
    public static final String CATEGORY = "category";
    public static final String EXTERNAL_URI = "external-uri";
    public static final String PROCESSED = "processed";

    /**
     * Add a new suggestion to SOLR (only if not yet present)
     * 
     * @param suggestion
     * @param commit
     * @throws IOException
     * @throws SolrServerException
     */
    public void addSuggestion(Suggestion suggestion, boolean commit) throws SolrServerException, IOException;

    /**
     * Return true if the suggestion is already in SOLR
     * 
     * @param suggestion
     * @return true if the suggestion is already in SOLR
     * @throws IOException
     * @throws SolrServerException
     */
    public boolean exist(Suggestion suggestion) throws SolrServerException, IOException;

    /**
     * Delete a suggestion from SOLR if any
     * 
     * @param suggestion
     * @throws IOException
     * @throws SolrServerException
     */
    public void deleteSuggestion(Suggestion suggestion) throws SolrServerException, IOException;

    /**
     * Flag a suggestion as processed in SOLR if any
     * 
     * @param suggestion
     * @throws IOException
     * @throws SolrServerException
     */
    public void flagSuggestionAsProcessed(Suggestion suggestion) throws SolrServerException, IOException;

    /**
     * Delete all the suggestions from SOLR if any related to a specific target
     * 
     * @param target
     * @throws IOException
     * @throws SolrServerException
     */
    public void deleteTarget(SuggestionTarget target) throws SolrServerException, IOException;

    void commit() throws SolrServerException, IOException;

    void flagAllSuggestionAsProcessed(String source, String idPart) throws SolrServerException, IOException;
}
