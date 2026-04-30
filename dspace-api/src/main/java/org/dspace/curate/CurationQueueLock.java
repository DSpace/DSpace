/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Represents an exclusive lock on a curation queue.
 * It is used to prevent multiple processes from working on the same queue simultaneously.
 * <p>
 * The lock is associated with a queue name and includes a unique token (the ticket),
 * when it was acquired and by which process (or worker).
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
@Entity
@Table(name = "curation_queue_lock", indexes = {
    @Index(name = "idx_cql_queue_name", columnList = "queue_name", unique = true)
})
public class CurationQueueLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "queue_name", nullable = false, unique = true, length = 128)
    private String queueName;

    @Column(name = "ticket", nullable = false)
    private long ticket;

    @Column(name = "owner_id", length = 256)
    private String ownerId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lock_date", nullable = false)
    private Date lockDate;

    /**
     * Default constructor required by JPA.
     */
    protected CurationQueueLock() {
        // default
    }

    /**
     * Main constructor for creating a new lock.
     *
     * @param queueName Name of the queue to lock
     * @param ticket Unique token for this lock
     * @param ownerId Identifier of the process/worker owning the lock
     */
    public CurationQueueLock(String queueName, long ticket, String ownerId) {
        this.queueName = queueName;
        this.ticket = ticket;
        this.ownerId = ownerId;
        this.lockDate = new Date();
    }

    public Integer getId() {
        return id;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public long getTicket() {
        return ticket;
    }

    public void setTicket(long ticket) {
        this.ticket = ticket;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Date getLockDate() {
        return lockDate;
    }

    public void setLockDate(Date lockDate) {
        this.lockDate = lockDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CurationQueueLock)) {
            return false;
        }
        CurationQueueLock that = (CurationQueueLock) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
