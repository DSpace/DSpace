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

    void cleanIndex(boolean force) throws IOException,
        SQLException, SearchServiceException;

    void commit() throws SearchServiceException;

    void optimize() throws SearchServiceException;

    void buildSpellCheck() throws SearchServiceException, IOException;
}
