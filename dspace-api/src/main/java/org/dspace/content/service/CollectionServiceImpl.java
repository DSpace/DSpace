/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.service;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.List;

/**
 * User: Robin Taylor
 * Date: 31/05/12
 * Time: 14:40
 */

public class CollectionServiceImpl implements CollectionService {

    private List<CollectionDAO> collectionDAOs;
    private CollectionDAO collectionDAO;

    /** log4j category */
    private static Logger log = Logger.getLogger(CollectionServiceImpl.class);

    public void setCollectionDAOs(List<CollectionDAO> collectionDAOs) {
        this.collectionDAOs = collectionDAOs;
    }

    public void setCollectionDAO()
    {
        String dbName = new DSpace().getConfigurationService().getProperty("db.name") ;
        for (CollectionDAO collDAO : collectionDAOs)
        {
            if (collDAO.getDBName().equals(dbName))
            {
                collectionDAO = collDAO;
            }
        }
    }

    public Collection find(Context context, int id) throws SQLException {
        Collection collection = collectionDAO.find(context, id);

        // First check the cache
        Collection fromCache = (Collection) context.fromCache(Collection.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        // not null, return Collection
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_collection",
                    "collection_id=" + id));
        }

        // not null, return Collection
        log.debug(LogManager.getHeader(context, "find_collection", "collection_id=" + id));
        return collection;
    }


}
