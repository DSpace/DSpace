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
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

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

    void cleanIndex() throws IOException, SQLException, SearchServiceException;

    void deleteIndex();

    void commit() throws SearchServiceException;

    void optimize() throws SearchServiceException;

    void buildSpellCheck() throws SearchServiceException, IOException;

    /**
     * Atomically update the index of a single field for an object
     * @param context       The DSpace context
     * @param uniqueIndexId The unique index ID of the object to update the index for
     * @param field         The field to update
     * @param fieldModifier The modifiers for the field to update. More information on how to atomically update a solr
     *                      field using a field modifier can be found here: https://yonik.com/solr/atomic-updates/
     */
    void atomicUpdate(Context context, String uniqueIndexId, String field, Map<String,Object> fieldModifier)
            throws SolrServerException, IOException;
    /**
     * Iterates over all documents in the Lucene index and verifies they are in
     * the database, if not, they are removed. Registers a heartbeat after each batch.
     *
     * @param handler       The handler to register the heartbeat with
     * @throws IOException            IO exception
     * @throws SQLException           sql exception
     * @throws SearchServiceException occurs when something went wrong with querying the solr server
     */
    void cleanIndex(DSpaceRunnableHandler handler)
        throws IOException, SQLException, SearchServiceException;

    /**
     * Iterates over all Items, Collections and Communities and updates them in
     * the index. Registers a heartbeat after every object is indexed.
     *
     * @param context The DSpace context
     * @param force   Whether the reindexing should be forced
     * @param type    The type of object to index
     * @param handler The runnable handler used to register heartbeats
     */
    void updateIndex(Context context, boolean force, String type, DSpaceRunnableHandler handler);
}
