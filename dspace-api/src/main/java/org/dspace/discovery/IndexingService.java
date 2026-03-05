/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Collection;
import org.dspace.core.Context;

/**
 * Interface used for indexing IndexableObject into discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface IndexingService {

    void indexContent(Context context, IndexableObject dso)
        throws SQLException;

    void indexContent(Context context, IndexableObject dso,
                      boolean force) throws SQLException;

    void indexContent(Context context, IndexableObject dso,
                      boolean force, boolean commit) throws SQLException, SearchServiceException;

    /**
     * Index a given DSO
     * @param context   The DSpace Context
     * @param dso       The DSpace Object to index
     * @param force     Force update even if not stale
     * @param commit    Commit the changes
     * @param preDb     Add a "preDB" status to the index (only applicable to Items)
     */
    void indexContent(Context context, IndexableObject dso,
                      boolean force, boolean commit, boolean preDb) throws SQLException, SearchServiceException;

    /**
     * Apply the provided plugins as atomic Solr updates for the given object, leaving all other index
     * fields intact.
     *
     * @param context  The DSpace Context
     * @param dso      The object to update
     * @param force    unused; kept for symmetry with the other indexContent overloads
     * @param commit   if true, commit after applying the plugins
     * @param plugins  the plugins to apply
     */
    void indexContent(Context context, IndexableObject dso,
                      boolean force, boolean commit, List<SolrServiceIndexPlugin> plugins)
            throws SQLException, SearchServiceException;

    void unIndexContent(Context context, IndexableObject dso)
        throws SQLException, IOException;

    void unIndexContent(Context context, IndexableObject dso, boolean commit)
        throws SQLException, IOException;

    void unIndexContent(Context context, String uniqueSearchID)
        throws IOException;

    void unIndexContent(Context context, String uniqueSearchID, boolean commit)
        throws IOException;

    void reIndexContent(Context context, IndexableObject dso)
        throws SQLException, IOException;

    void createIndex(Context context) throws SQLException, IOException;

    void updateIndex(Context context);

    void updateIndex(Context context, boolean force);

    void updateIndex(Context context, boolean force, String type);

    /**
     * Iterate over all indexed objects and apply the provided plugins as atomic updates,
     * leaving all other index fields intact.
     *
     * @param context  The DSpace Context
     * @param force    unused; kept for symmetry with the other updateIndex overloads
     * @param plugins  the plugins to apply
     */
    void updateIndex(Context context, boolean force, List<SolrServiceIndexPlugin> plugins);

    void cleanIndex() throws IOException, SQLException, SearchServiceException;

    void deleteIndex();

    void commit() throws SearchServiceException;

    void optimize() throws SearchServiceException;

    void buildSpellCheck() throws SearchServiceException, IOException;

    /**
     * Atomically update the index of a single field for an object
     * @param context       The DSpace context
     * @param uniqueIndexId The unqiue index ID of the object to update the index for
     * @param field         The field to update
     * @param fieldModifier The modifiers for the field to update. More information on how to atomically update a solr
     *                      field using a field modifier can be found here: https://yonik.com/solr/atomic-updates/
     */
    void atomicUpdate(Context context, String uniqueIndexId, String field, Map<String,Object> fieldModifier)
            throws SolrServerException, IOException;

    /**
     * Index all items in the given collection.
     * When {@code numThreads} is greater than 1, items are indexed in parallel using a fixed
     * thread pool. The caller's {@code context} is used only to pre-collect item IDs; each
     * worker thread opens its own read-only {@link Context}.
     *
     * @param context    calling context
     * @param collection collection whose items should be indexed
     * @param force      when {@code true} every item is re-indexed regardless of modification date
     * @param numThreads number of parallel worker threads; 1 means sequential
     * @return number of items indexed
     * @throws SQLException           on database error
     * @throws IOException            on I/O error
     * @throws SearchServiceException on Solr error
     */
    long indexItems(Context context, Collection collection, boolean force, int numThreads)
        throws SQLException, IOException, SearchServiceException;
}
