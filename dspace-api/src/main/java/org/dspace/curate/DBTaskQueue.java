/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.curate.dao.CurationQueueEntryDAO;
import org.dspace.curate.dao.CurationQueueLockDAO;
import org.dspace.curate.dao.impl.CurationQueueEntryDAOImpl;
import org.dspace.curate.dao.impl.CurationQueueLockDAOImpl;
import org.dspace.utils.DSpace;
import org.hibernate.exception.ConstraintViolationException;

/**
 * DBTaskQueue provides a TaskQueue implementation backed by a relational database
 * (Hibernate/JPA) instead of the original flat-file mechanism. Each enqueued task
 * is persisted as a row in the {@code curation_task_queue} table (see entity {@link CurationQueueEntry}).
 *
 * Concurrency / locking model:
 *  - Writers simply insert new rows into curation_task_queue table
 *  - A single reader claims exclusive access to a queue by inserting a row in the curation_queue_lock table
 *    with a unique ticket value. The uniqueness constraint on the queue_name ensures only one process
 *    can claim a queue at a time.
 *  - After claiming, the reader holds an in-memory reference to the ticket used to acquire the lock.
 *    This ticket must be presented to release the lock.
 *  - On release: the lock row is deleted, allowing other processes to claim the queue.
 *  - Only the entries that were retrieved during dequeue will be removed during release,
 *    not any new entries that were added while processing.
 *
 * NOTE: This implementation assumes the caller supplies a reasonably unique ticket (e.g. System.currentTimeMillis()).
 *
 * Format parity: The persistent data mirrors the pipe-separated value used by {@link TaskQueueEntry}:
 *   epersonId|submitTime|tasks(comma separated)|objectId
 *
 * This class purposefully manages its own short-lived {@link Context} instances because the TaskQueue
 * interface does not expose a Context parameter.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com) (DB refactor)
 */
public class DBTaskQueue implements TaskQueue {
    private static final Logger log = LogManager.getLogger(TaskQueue.class);

    // ticket of the currently held lock (or -1L if no lock is held)
    private long readTicket = -1L;

    // name of the currently locked queue (or null if no lock is held)
    private String lockedQueueName = null;

    // IDs of entries that were dequeued and need to be deleted on release
    private Set<Integer> dequeuedEntryIds = new HashSet<>();

    // Data access objects
    private final CurationQueueEntryDAO queueEntryDAO;
    private final CurationQueueLockDAO queueLockDAO;

    public DBTaskQueue() {
        queueEntryDAO = new DSpace().getSingletonService(CurationQueueEntryDAOImpl.class);
        queueLockDAO = new DSpace().getSingletonService(CurationQueueLockDAOImpl.class);
    }

    /**
     * Return distinct queue names existing in the DB.
     */
    @Override
    public String[] queueNames() {
        try (Context context = new Context()) {
            List<String> names = queueEntryDAO.findAllQueueNames(context);
            return names.toArray(new String[0]);
        } catch (SQLException e) {
            log.error("SQL error retrieving queue names: {}", e.getMessage(), e);
            return new String[0];
        } catch (Exception e) {
            log.error("Error retrieving queue names: {}", e.getMessage(), e);
            return new String[0];
        }
    }

    /**
     * Enqueue a single task entry by delegating to the set-based method.
     */
    @Override
    public synchronized void enqueue(String queueName, TaskQueueEntry entry) throws IOException {
        Set<TaskQueueEntry> set = new HashSet<>();
        set.add(entry);
        enqueue(queueName, set);
    }

    /**
     * Persist a set of task entries for the specified queue.
     * This operation can be performed even when the queue is being processed,
     * allowing new tasks to be added while others are in progress.
     */
    @Override
    public synchronized void enqueue(String queueName, Set<TaskQueueEntry> entrySet) throws IOException {
        if (entrySet == null || entrySet.isEmpty()) {
            return; // nothing to do
        }
        try (Context context = new Context()) {
            for (TaskQueueEntry entry : entrySet) {
                CurationQueueEntry entity = new CurationQueueEntry(queueName, entry);
                queueEntryDAO.create(context, entity);
            }
            context.commit();
        } catch (SQLException sqle) {
            log.error("SQL error while enqueuing tasks for queue '{}': {}", queueName, sqle.getMessage(), sqle);
            throw new IOException("Database error enqueuing tasks", sqle);
        } catch (ConstraintViolationException e) {
            log.error("Violating constraint {}", e.getConstraintName());
        } catch (Exception e) {
            log.error("Unexpected error while enqueuing tasks for queue '{}': {}", queueName, e.getMessage(), e);
            throw new IOException("Unexpected error enqueuing tasks", e);
        }
    }

    /**
     * Claim (dequeue) all entries for the queue by acquiring an exclusive lock via the curation_queue_lock table.
     * Returns the entries as immutable value objects. Subsequent dequeue calls with a different ticket are ignored
     * until release is invoked.
     * The IDs of the dequeued entries are stored internally so that only these specific entries
     * will be removed when release(remove=true) is called.
     */
    @Override
    public synchronized Set<TaskQueueEntry> dequeue(String queueName, long ticket) throws IOException {
        // Reset the stored entry IDs from any previous dequeue operation
        dequeuedEntryIds.clear();

        if (readTicket != -1L) {
            // already holding a lock
            return Collections.emptySet();
        }
        if (queueName == null) {
            return Collections.emptySet();
        }

        try (Context context = new Context()) {
            // First check if the queue is already locked by another process
            if (queueLockDAO.isQueueLocked(context, queueName)) {
                log.debug("Queue '{}' is already locked by another process", queueName);
                return Collections.emptySet();
            }

            // Get the number of items in the queue before acquiring the lock
            long entryCount = queueEntryDAO.countByQueueName(context, queueName);
            if (entryCount == 0) {
                // No entries to process
                log.debug("Queue '{}' has no entries to process", queueName);
                return Collections.emptySet();
            }

            // Create a new lock for this queue
            CurationQueueLock lock = new CurationQueueLock(queueName, ticket,
                    "process-" + Thread.currentThread().getId());
            queueLockDAO.create(context, lock);

            // If we get here, the lock was successfully created
            readTicket = ticket;
            lockedQueueName = queueName;

            // Fetch all entries for this queue
            List<CurationQueueEntry> entries = queueEntryDAO.findByQueueName(context, queueName);

            // Store the IDs of entries that we're returning for processing
            dequeuedEntryIds = entries.stream()
                .map(CurationQueueEntry::getId)
                .collect(Collectors.toSet());

            // Convert to task queue entries for return
            Set<TaskQueueEntry> result = new HashSet<>();
            for (CurationQueueEntry entry : entries) {
                result.add(entry.toTaskQueueEntry());
            }

            // Commit the transaction to ensure the lock is persisted
            context.commit();
            return result;
        } catch (SQLException sqle) {
            log.error("SQL error while dequeuing tasks for queue '{}': {}", queueName, sqle.getMessage(), sqle);
            // If there was an error, ensure we don't hold the lock in memory
            readTicket = -1L;
            lockedQueueName = null;
            dequeuedEntryIds.clear();
            throw new IOException("Database error dequeuing tasks", sqle);
        } catch (Exception e) {
            log.error("Unexpected error while dequeuing tasks for queue '{}': {}", queueName, e.getMessage(), e);
            // If there was an error, ensure we don't hold the lock in memory
            readTicket = -1L;
            lockedQueueName = null;
            dequeuedEntryIds.clear();
            throw new IOException("Unexpected error dequeuing tasks", e);
        }
    }

    /**
     * Release the lock on the queue. If remove=true, only the entries that were retrieved
     * during the dequeue operation will be deleted, not any new entries that might have
     * been added while processing.
     */
    @Override
    public synchronized void release(String queueName, long ticket, boolean remove) {
        if (ticket != readTicket || readTicket == -1L || lockedQueueName == null ||
                !lockedQueueName.equals(queueName)) {
            return; // nothing to release or ticket/queue mismatch
        }

        try (Context context = new Context()) {
            // First release the lock
            boolean lockReleased = queueLockDAO.releaseLock(context, queueName, ticket);
            if (!lockReleased) {
                log.warn("Failed to release lock for queue '{}' with ticket {}", queueName, ticket);
            }

            // If requested, delete only the entries that were dequeued, not all entries in the queue
            if (remove && !dequeuedEntryIds.isEmpty()) {
                int deleted = queueEntryDAO.deleteByIds(context, dequeuedEntryIds);
                log.debug("Deleted {} entries from queue '{}' that were processed", deleted, queueName);
            }

            context.commit();
        } catch (Exception e) {
            log.error("Error releasing queue '{}' (remove={}): {}", queueName, remove, e.getMessage(), e);
        } finally {
            // Even in case of error, release the lock in memory
            readTicket = -1L;
            lockedQueueName = null;
            dequeuedEntryIds.clear();
        }
    }
}
