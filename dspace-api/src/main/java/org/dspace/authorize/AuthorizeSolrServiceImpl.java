/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.AuthorizeSolrService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AuthorizeSolrService uses Solr to check if a given context's user has ADMIN rights to any DSO of a given type.
 */
public class AuthorizeSolrServiceImpl implements AuthorizeSolrService {

    private static Logger log = LogManager.getLogger(AuthorizeSolrServiceImpl.class);

    @Autowired
    private SearchService searchService;
    @Autowired
    private AuthorizeService authorizeService;

    protected AuthorizeSolrServiceImpl() {

    }

    /**
     * Checks that the context's current user is a community admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a community admin in the site
     *                  false when this is not the case, or an exception occurred
     */
    @Override
    public boolean isCommunityAdmin(Context context) throws SQLException {
        return performCheck(context, "search.resourcetype:Community");
    }

    /**
     * Checks that the context's current user is a collection admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a collection admin in the site
     *                  false when this is not the case, or an exception occurred
     */
    @Override
    public boolean isCollectionAdmin(Context context) throws SQLException {
        return performCheck(context, "search.resourcetype:Collection");
    }

    /**
     * Checks that the context's current user is a community or collection admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a community or collection admin in the site
     *                  false when this is not the case, or an exception occurred
     */
    @Override
    public boolean isComColAdmin(Context context) throws SQLException {
        return performCheck(context,
            "(search.resourcetype:Community OR search.resourcetype:Collection)");
    }

    /**
     *  Finds communities for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @param offset    the offset for pagination
     * @param limit     the amount of dso's to return
     * @return          a list of communities for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public List<Community> findAdminAuthorizedCommunity(Context context, String query, int offset, int limit)
        throws SearchServiceException, SQLException {
        List<Community> communities = new ArrayList<>();
        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:Community",
            offset, limit);
        for (IndexableObject solrCollections : discoverResult.getIndexableObjects()) {
            Community community = ((IndexableCommunity) solrCollections).getIndexedObject();
            communities.add(community);
        }
        return communities;
    }

    /**
     *  Finds the amount of communities for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @return          the number of communities for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public int countAdminAuthorizedCommunity(Context context, String query)
        throws SearchServiceException, SQLException {
        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:Community",
            null, null);
        return (int)discoverResult.getTotalSearchResults();
    }

    /**
     *  Finds collections for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @param offset    the offset for pagination
     * @param limit     the amount of dso's to return
     * @return          a list of collections for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public List<Collection> findAdminAuthorizedCollection(Context context, String query, int offset, int limit)
        throws SearchServiceException, SQLException {
        List<Collection> collections = new ArrayList<>();
        if (context.getCurrentUser() == null) {
            return collections;
        }

        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:Collection",
            offset, limit);
        for (IndexableObject solrCollections : discoverResult.getIndexableObjects()) {
            Collection collection = ((IndexableCollection) solrCollections).getIndexedObject();
            collections.add(collection);
        }
        return collections;
    }

    /**
     *  Finds the amount of collections for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @return          the number of collections for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public int countAdminAuthorizedCollection(Context context, String query)
        throws SearchServiceException, SQLException {
        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:Collection",
            null, null);
        return (int)discoverResult.getTotalSearchResults();
    }

    private boolean performCheck(Context context, String query) throws SQLException {
        if (context.getCurrentUser() == null) {
            return false;
        }

        try {
            DiscoverResult discoverResult = getDiscoverResult(context, query, null, null);
            if (discoverResult.getTotalSearchResults() > 0) {
                return true;
            }
        } catch (SearchServiceException e) {
            log.error("Failed getting getting community/collection admin status for "
                + context.getCurrentUser().getEmail() + " The search error is: " + e.getMessage()
                + " The search resourceType filter was: " + query);
        }
        return false;
    }

    private DiscoverResult getDiscoverResult(Context context, String query, Integer offset, Integer limit)
        throws SearchServiceException, SQLException {
        StringBuilder groupQuery = new StringBuilder();
        List<Group> groups = context.getCurrentUser().getGroups();
        addGroupToQuery(groupQuery, groups);

        DiscoverQuery discoverQuery = new DiscoverQuery();
        if (!authorizeService.isAdmin(context)) {
            query = query + " AND (" +
                "admin:e" + context.getCurrentUser().getID() + groupQuery.toString() + ")";
        }
        discoverQuery.setQuery(query);
        if (offset != null) {
            discoverQuery.setStart(offset);
        }
        if (limit != null) {
            discoverQuery.setMaxResults(limit);
        }


        return searchService.search(context, discoverQuery);
    }

    private void addGroupToQuery(StringBuilder groupQuery, List<Group> groups) {
        if (groups == null) {
            return;
        }

        for (Group group: groups) {
            groupQuery.append(" OR admin:g");
            groupQuery.append(group.getID());

            addGroupToQuery(groupQuery, group.getParentGroups());
        }
    }

    private String formatCustomQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return "";
        } else {
            return query + " AND ";
        }
    }
}
