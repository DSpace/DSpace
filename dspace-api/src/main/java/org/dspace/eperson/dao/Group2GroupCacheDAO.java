/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.Group;
import org.dspace.eperson.Group2GroupCache;

/**
 * Database Access Object interface class for the Group2GroupCache object.
 * The implementation of this class is responsible for all database calls for the Group2GroupCache object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface Group2GroupCacheDAO extends GenericDAO<Group2GroupCache> {

    /**
     * Returns the current cache table as a set of UUID pairs.
     * @param context The relevant DSpace Context.
     * @return Set of UUID pairs, where the first element is the parent UUID and the second one is the child UUID.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    Set<Pair<UUID, UUID>> getCache(Context context) throws SQLException;

    /**
     * Returns all cache entities that are children of a given parent Group entity.
     * @param context The relevant DSpace Context.
     * @param group Parent group to perform the search.
     * @return List of cached groups that are children of the parent group.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<Group2GroupCache> findByParent(Context context, Group group) throws SQLException;

    /**
     * Returns all cache entities that are parents of at least one group from a children groups list.
     * @param context The relevant DSpace Context.
     * @param groups Children groups to perform the search.
     * @return List of cached groups that are parents of at least one group from the children groups list.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<Group2GroupCache> findByChildren(Context context, Iterable<Group> groups) throws SQLException;

    /**
     * Returns the cache entity given specific parent and child groups.
     * @param context The relevant DSpace Context.
     * @param parent Parent group.
     * @param child Child gruoup.
     * @return Cached group.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    Group2GroupCache findByParentAndChild(Context context, Group parent, Group child) throws SQLException;

    /**
     * Returns the cache entity given specific parent and child groups.
     * @param context The relevant DSpace Context.
     * @param parent Parent group.
     * @param child Child gruoup.
     * @return Cached group.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    Group2GroupCache find(Context context, Group parent, Group child) throws SQLException;

    /**
     * Completely deletes the current cache table.
     * @param context The relevant DSpace Context.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    void deleteAll(Context context) throws SQLException;

    /**
     * Deletes a specific cache row given parent and child groups UUIDs.
     * @param context The relevant DSpace Context.
     * @param parent Parent group UUID.
     * @param child Child group UUID.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    void deleteFromCache(Context context, UUID parent, UUID child) throws SQLException;

    /**
     * Adds a single row to the cache table given parent and child groups UUIDs.
     * @param context The relevant DSpace Context.
     * @param parent Parent group UUID.
     * @param child Child group UUID.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    void addToCache(Context context, UUID parent, UUID child) throws SQLException;
}
