/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.eperson.EPerson;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Item object.
 * The implementation of this class is responsible for all database calls for the Item object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ItemDAO extends DSpaceObjectLegacySupportDAO<Item> {
    public Iterator<Item> findAll(Session session, boolean archived) throws SQLException;

    public Iterator<Item> findAll(Session session, boolean archived, int limit, int offset) throws SQLException;

    @Deprecated
    public Iterator<Item> findAll(Session session, boolean archived, boolean withdrawn) throws SQLException;

    /**
     * Find all items that are:
     * - NOT in the workspace
     * - NOT in the workflow
     * - NOT a template item for e.g. a collection
     *
     * This implies that the result also contains older versions of items and withdrawn items.
     * @param session the current request's database context.
     * @return iterator over all regular items.
     * @throws SQLException if database error.
     */
    public Iterator<Item> findAllRegularItems(Session session) throws SQLException;

    /**
     * Find all Items modified since a Date.
     *
     * @param session the current request's database context.
     * @param since   Earliest interesting last-modified date.
     * @return iterator over items
     * @throws SQLException if database error
     */
    public Iterator<Item> findByLastModifiedSince(Session session, Date since)
        throws SQLException;

    public Iterator<Item> findBySubmitter(Session session, EPerson eperson) throws SQLException;

    /**
     * Find all the items by a given submitter. The order is
     * indeterminate. All items are included.
     *
     * @param session the current request's database context.
     * @param eperson the submitter
     * @param retrieveAllItems flag to determine if only archive should be returned
     * @return an iterator over the items submitted by eperson
     * @throws SQLException if database error
     */
    public Iterator<Item> findBySubmitter(Session session, EPerson eperson, boolean retrieveAllItems)
        throws SQLException;

    public Iterator<Item> findBySubmitter(Session session, EPerson eperson, MetadataField metadataField, int limit)
        throws SQLException;

    public Iterator<Item> findByMetadataField(Session session, MetadataField metadataField, String value,
                                              boolean inArchive) throws SQLException;

    public Iterator<Item> findByMetadataQuery(Session session, List<List<MetadataField>> listFieldList,
                                              List<String> query_op, List<String> query_val, List<UUID> collectionUuids,
                                              String regexClause, int offset, int limit) throws SQLException;

    public Iterator<Item> findByAuthorityValue(Session session, MetadataField metadataField, String authority,
                                               boolean inArchive) throws SQLException;

    public Iterator<Item> findArchivedByCollection(Session session, Collection collection, Integer limit,
                                                   Integer offset) throws SQLException;

    /**
     * Returns all the Items in an iterator that are archived and for which the given Collection is part of the Item's
     * Collections but it is not the owning collection
     * @param session       The current request's database context.
     * @param collection    The collection to check on
     * @param limit         The limit for the query
     * @param offset        The offset for the query
     * @return              An iterator containing the items for which the constraints hold true
     * @throws SQLException If something goes wrong
     */
    public Iterator<Item> findArchivedByCollectionExcludingOwning(Session session, Collection collection, Integer limit,
                                                                  Integer offset) throws SQLException;

    /**
     * Counts all the items that are archived and for which the given Collection is part of the Item's Collections
     * but it is not the owning Collection
     * @param session       The current request's database context.
     * @param collection    The collection to check on
     * @return              The total amount of items that fit the constraints
     * @throws SQLException If something goes wrong
     */
    public int countArchivedByCollectionExcludingOwning(Session session, Collection collection) throws SQLException;

    public Iterator<Item> findAllByCollection(Session session, Collection collection) throws SQLException;

    public Iterator<Item> findAllByCollection(Session session, Collection collection, Integer limit, Integer offset)
        throws SQLException;

    /**
     * Count number of items in a given collection
     *
     * @param session          the current request's database context.
     * @param collection       the collection
     * @param includeArchived  whether to include archived items in count
     * @param includeWithdrawn whether to include withdrawn items in count
     * @return item count
     * @throws SQLException if database error
     */
    public int countItems(Session session, Collection collection, boolean includeArchived, boolean includeWithdrawn)
        throws SQLException;

    /**
     * Count number of unique items across several collections at once.
     * This method can be used with
     * {@link org.dspace.content.service.CommunityService#getAllCollections(Context, Community)}
     * to determine the unique number of items in a Community.
     *
     * @param session          the current request's database context.
     * @param collections      the list of collections
     * @param includeArchived  whether to include archived items in count
     * @param includeWithdrawn whether to include withdrawn items in count
     * @return item count
     * @throws SQLException if database error
     */
    public int countItems(Session session, List<Collection> collections, boolean includeArchived,
                          boolean includeWithdrawn) throws SQLException;

    /**
     * Get all Items installed or withdrawn, discoverable, and modified since a Date.
     *
     * @param session      the current request's database context.
     * @param archived     whether to find archived
     * @param withdrawn    whether to find withdrawn
     * @param discoverable whether to find discoverable
     * @param lastModified earliest interesting last-modified date.
     * @return iterator over items
     * @throws SQLException if database error
     */
    public Iterator<Item> findAll(Session session, boolean archived,
                                  boolean withdrawn, boolean discoverable, Date lastModified)
        throws SQLException;

    /**
     * Count total number of items (rows in item table).
     *
     * @param session the current request's database context.
     * @return total count
     * @throws SQLException if database error
     */
    int countRows(Session session) throws SQLException;

    /**
     * Count number of items based on specific status flags
     *
     * @param session          the current request's database context.
     * @param includeArchived  whether to include archived items in count
     * @param includeWithdrawn whether to include withdrawn items in count
     * @return count of items
     * @throws SQLException if database error
     */
    int countItems(Session session, boolean includeArchived, boolean includeWithdrawn) throws SQLException;

    /**
     * Count number of items from the specified submitter based on specific status flags
     *
     * @param session          the current request's database context.
     * @param submitter        the submitter
     * @param includeArchived  whether to include archived items in count
     * @param includeWithdrawn whether to include withdrawn items in count
     * @return count of items
     * @throws SQLException if database error
     */
    public int countItems(Session session, EPerson submitter, boolean includeArchived, boolean includeWithdrawn)
        throws SQLException;
}
