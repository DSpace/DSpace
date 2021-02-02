/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * AuthorizeSolrService uses Solr to check if a given context's user has ADMIN rights to any DSO of a given type.
 */
public interface AuthorizeSolrService {

    /**
     * Checks that the context's current user is a community admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a community admin in the site
     *                  false when this is not the case, or an exception occurred
     */
    boolean isCommunityAdmin(Context context) throws SQLException;

    /**
     * Checks that the context's current user is a collection admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a collection admin in the site
     *                  false when this is not the case, or an exception occurred
     */
    boolean isCollectionAdmin(Context context) throws SQLException;

    /**
     * Checks that the context's current user is a community or collection admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a community or collection admin in the site
     *                  false when this is not the case, or an exception occurred
     */
    boolean isComColAdmin(Context context) throws SQLException;

    /**
     * Finds communities for which the current user is admin, AND which match the query.
     *
     * @param context   context with the current user
     * @param query     the query for which to filter the results more
     * @param offset    used for pagination of the results
     * @param limit     used for pagination of the results
     * @return          the number of matching communities
     * @throws SearchServiceException
     * @throws SQLException
     */
    List<Community> findAdminAuthorizedCommunity(Context context, String query, int offset, int limit)
        throws SearchServiceException, SQLException;

    /**
     * Counts communities for which the current user is admin, AND which match the query.
     *
     * @param context   context with the current user
     * @param query     the query for which to filter the results more
     * @return          the matching communities
     * @throws SearchServiceException
     * @throws SQLException
     */
    int countAdminAuthorizedCommunity(Context context, String query)
        throws SearchServiceException, SQLException;

    /**
     * Finds collections for which the current user is admin, AND which match the query.
     *
     * @param context   context with the current user
     * @param query     the query for which to filter the results more
     * @param offset    used for pagination of the results
     * @param limit     used for pagination of the results
     * @return          the matching collections
     * @throws SearchServiceException
     * @throws SQLException
     */
    List<Collection> findAdminAuthorizedCollection(Context context, String query, int offset, int limit)
        throws SearchServiceException, SQLException;

    /**
     * Counts collections for which the current user is admin, AND which match the query.
     *
     * @param context   context with the current user
     * @param query     the query for which to filter the results more
     * @return          the number of matching collections
     * @throws SearchServiceException
     * @throws SQLException
     */
    int countAdminAuthorizedCollection(Context context, String query)
        throws SearchServiceException, SQLException;
}
