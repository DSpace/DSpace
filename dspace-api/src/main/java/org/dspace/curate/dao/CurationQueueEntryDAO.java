/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.curate.CurationQueueEntry;

/**
 * Database Access Object interface for CurationQueueEntry.
 * This interface is responsible for all database calls for CurationQueueEntry objects and is
 * autowired by Spring. It should only be accessed from a single service and should never be exposed
 * outside of the API.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public interface CurationQueueEntryDAO extends GenericDAO<CurationQueueEntry> {

    /**
     * Find all distinct queue names in the database.
     *
     * @param context The DSpace context
     * @return List of queue names
     * @throws SQLException If database error
     */
    List<String> findAllQueueNames(Context context) throws SQLException;

    /**
     * Find all entries for a specific queue.
     *
     * @param context The DSpace context
     * @param queueName The name of the queue
     * @return List of entries for the queue
     * @throws SQLException If database error
     */
    List<CurationQueueEntry> findByQueueName(Context context, String queueName) throws SQLException;

    /**
     * Count how many entries are in a queue.
     *
     * @param context The DSpace context
     * @param queueName The name of the queue
     * @return Number of entries in the queue
     * @throws SQLException If database error
     */
    long countByQueueName(Context context, String queueName) throws SQLException;

    /**
     * Delete all entries for a specific queue.
     *
     * @param context The DSpace context
     * @param queueName The name of the queue
     * @return Number of entries deleted
     * @throws SQLException If database error
     */
    int deleteByQueueName(Context context, String queueName) throws SQLException;

    /**
     * Delete entries by their IDs.
     * This allows selective deletion of specific entries from a queue.
     *
     * @param context The DSpace context
     * @param ids Set of entry IDs to delete
     * @return Number of entries deleted
     * @throws SQLException If database error
     */
    int deleteByIds(Context context, Set<Integer> ids) throws SQLException;
}
