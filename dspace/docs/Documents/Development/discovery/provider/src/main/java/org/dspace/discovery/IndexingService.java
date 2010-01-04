package org.dspace.discovery;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: mdiggory
 * Date: Oct 19, 2009
 * Time: 12:51:53 PM
 */
public interface IndexingService {

    void indexContent(Context context, DSpaceObject dso)
            throws SQLException;

    void indexContent(Context context, DSpaceObject dso,
                      boolean force) throws SQLException;

    void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException;

    void unIndexContent(Context context, String handle)
            throws SQLException, IOException;

    void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException;

    void createIndex(Context context) throws SQLException, IOException;

    void updateIndex(Context context);

    void updateIndex(Context context, boolean force);

    void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException;
}
