/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the Bitstream object.
 * The implementation of this class is responsible for all database calls for the Bitstream object and is autowired
 * by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BitstreamDAO extends DSpaceObjectLegacySupportDAO<Bitstream> {

    /**
     * Returns a paginated iterator over all bitstreams.
     *
     * @param context the DSpace context
     * @param limit the maximum number of results to return
     * @param offset the index of the first result to return
     * @return an iterator over the requested bitstreams
     * @throws SQLException if a database error occurs
     */
    Iterator<Bitstream> findAll(Context context, int limit, int offset) throws SQLException;

    /**
     * Returns the bitstreams that have been marked as deleted.
     *
     * @param context the DSpace context
     * @param limit the maximum number of results to return
     * @param offset the index of the first result to return
     * @return the list of deleted bitstreams
     * @throws SQLException if a database error occurs
     */
    List<Bitstream> findDeletedBitstreams(Context context, int limit, int offset) throws SQLException;

    /**
     * Finds bitstreams sharing the same internal identifier as the given bitstream.
     *
     * @param context the DSpace context
     * @param bitstream the bitstream whose internal identifier is matched
     * @return the list of bitstreams with a duplicate internal identifier
     * @throws SQLException if a database error occurs
     */
    List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Finds bitstreams that have no recent checksum result.
     *
     * @param context the DSpace context
     * @return the list of bitstreams without a recent checksum
     * @throws SQLException if a database error occurs
     */
    List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException;

    /**
     * Returns an iterator over the bitstreams belonging to the given community.
     *
     * @param context the DSpace context
     * @param community the community
     * @return an iterator over the community bitstreams
     * @throws SQLException if a database error occurs
     */
    Iterator<Bitstream> findByCommunity(Context context, Community community) throws SQLException;

    /**
     * Returns an iterator over the bitstreams belonging to the given collection.
     *
     * @param context the DSpace context
     * @param collection the collection
     * @return an iterator over the collection bitstreams
     * @throws SQLException if a database error occurs
     */
    Iterator<Bitstream> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * Returns an iterator over the bitstreams belonging to the given item.
     *
     * @param context the DSpace context
     * @param item the item
     * @return an iterator over the item bitstreams
     * @throws SQLException if a database error occurs
     */
    Iterator<Bitstream> findByItem(Context context, Item item) throws SQLException;

    /**
     * Returns an iterator over the bitstreams stored in the given assetstore.
     *
     * @param context the DSpace context
     * @param storeNumber the assetstore number
     * @return an iterator over the matching bitstreams
     * @throws SQLException if a database error occurs
     */
    Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException;

    /**
     * Counts the bitstreams stored in the given assetstore.
     *
     * @param context the DSpace context
     * @param storeNumber the assetstore number
     * @return the number of bitstreams in the assetstore
     * @throws SQLException if a database error occurs
     */
    Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException;

    int countRows(Context context) throws SQLException;

    int countDeleted(Context context) throws SQLException;

    int countWithNoPolicy(Context context) throws SQLException;

    List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException;

    /**
     * Returns an iterator over the showable bitstreams of the given item and bundle.
     *
     * @param context the DSpace context
     * @param itemId the identifier of the item
     * @param bundleName the name of the bundle
     * @return an iterator over the showable bitstreams
     * @throws SQLException if a database error occurs
     */
    Iterator<Bitstream> findShowableByItem(Context context, UUID itemId, String bundleName) throws SQLException;
}
