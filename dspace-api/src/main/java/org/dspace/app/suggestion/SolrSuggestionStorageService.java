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

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.Context;

/**
 * Service to deal with the local suggestion solr core used by the
 * SolrSuggestionProvider(s)
 *
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 * @author Luca Giamminonni (luca.giamminonni at 4science dot it)
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
    public static final String SCORE = "trust";
    public static final String EVIDENCES = "evidences";

    /**
     * Add a new suggestion to SOLR
     * 
     * @param suggestion
     * @param force true if the suggestion must be reindexed
     * @param commit
     * @throws IOException
     * @throws SolrServerException
     */
    public void addSuggestion(Suggestion suggestion, boolean force, boolean commit)
            throws SolrServerException, IOException;

    /**
     * Return true if the suggestion is already in SOLR and flagged as processed
     * 
     * @param suggestion
     * @return true if the suggestion is already in SOLR and flagged as processed
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

    /**
     * Performs an explicit commit, causing pending documents to be committed for
     * indexing.
     *
     * @throws SolrServerException
     * @throws IOException
     */
    void commit() throws SolrServerException, IOException;

    /**
     * Flag all the suggestion related to the given source and id as processed.
     *
     * @param  source              the source name
     * @param  idPart              the id's last part
     * @throws SolrServerException
     * @throws IOException
     */
    void flagAllSuggestionAsProcessed(String source, String idPart) throws SolrServerException, IOException;

    /**
     * Count all the targets related to the given source.
     *
     * @param  source              the source name
     * @return                     the target's count
     * @throws IOException
     * @throws SolrServerException
     */
    long countAllTargets(Context context, String source) throws SolrServerException, IOException;

    /**
     * Count all the unprocessed suggestions related to the given source and target.
     *
     * @param  context             the DSpace Context
     * @param  source              the source name
     * @param  target              the target id
     * @return                     the suggestion count
     * @throws SolrServerException
     * @throws IOException
     */
    long countUnprocessedSuggestionByTarget(Context context, String source, UUID target)
        throws SolrServerException, IOException;

    /**
     * Find all the unprocessed suggestions related to the given source and target.
     *
     * @param  context             the DSpace Context
     * @param  source              the source name
     * @param  target              the target id
     * @param  pageSize            the page size
     * @param  offset              the page offset
     * @param  ascending           true to retrieve the suggestions ordered by score
     *                             ascending
     * @return                     the found suggestions
     * @throws SolrServerException
     * @throws IOException
     */
    List<Suggestion> findAllUnprocessedSuggestions(Context context, String source, UUID target,
        int pageSize, long offset, boolean ascending) throws SolrServerException, IOException;

    /**
     *
     * Find all the suggestion targets related to the given source.
     *
     * @param  context             the DSpace Context
     * @param  source              the source name
     * @param  pageSize            the page size
     * @param  offset              the page offset
     * @return                     the found suggestion targets
     * @throws SolrServerException
     * @throws IOException
     */
    List<SuggestionTarget> findAllTargets(Context context, String source, int pageSize, long offset)
        throws SolrServerException, IOException;

    /**
     * Find an unprocessed suggestion by the given source, target id and suggestion
     * id.
     *
     * @param  context             the DSpace Context
     * @param  source              the source name
     * @param  target              the target id
     * @param  id                  the suggestion id
     * @return                     the suggestion, if any
     * @throws SolrServerException
     * @throws IOException
     */
    Suggestion findUnprocessedSuggestion(Context context, String source, UUID target, String id)
        throws SolrServerException, IOException;

    /**
     * Find a suggestion target by the given source and target.
     * 
     * @param  context             the DSpace Context
     * @param  source              the source name
     * @param  target              the target id
     * @return                     the suggestion target, if any
     * @throws SolrServerException
     * @throws IOException
     */
    SuggestionTarget findTarget(Context context, String source, UUID target) throws SolrServerException, IOException;
}
