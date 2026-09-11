/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.dao;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.curate.CurationQueueLock;

/**
 * Database Access Object interface for CurationQueueLock.
 * Manages access to data for curation queue locks.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public interface CurationQueueLockDAO extends GenericDAO<CurationQueueLock> {

    /**
     * Find an existing lock for the specified queue.
     *
     * @param context DSpace context
     * @param queueName Queue name
     * @return The lock if it exists, otherwise null
     * @throws SQLException If database error
     */
    CurationQueueLock findByQueueName(Context context, String queueName) throws SQLException;

    /**
     * Check if a lock exists for the specified queue.
     *
     * @param context DSpace context
     * @param queueName Queue name
     * @return true if a lock exists, false otherwise
     * @throws SQLException If database error
     */
    boolean isQueueLocked(Context context, String queueName) throws SQLException;

    /**
     * Verify if the specified lock corresponds to the provided ticket.
     *
     * @param context DSpace context
     * @param queueName Queue name
     * @param ticket Ticket to verify
     * @return true if the lock is valid for the ticket, false otherwise
     * @throws SQLException If database error
     */
    boolean validateLock(Context context, String queueName, long ticket) throws SQLException;

    /**
     * Release (delete) the lock associated with this queue and ticket.
     *
     * @param context DSpace context
     * @param queueName Queue name
     * @param ticket Ticket associated with the lock
     * @return true if the lock was successfully released, false otherwise
     * @throws SQLException If database error
     */
    boolean releaseLock(Context context, String queueName, long ticket) throws SQLException;
}
