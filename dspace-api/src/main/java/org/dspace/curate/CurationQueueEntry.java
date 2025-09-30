/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * CurationQueueEntry is the persistent database representation of a queued curation task entry.
 * It mirrors the value object {@link TaskQueueEntry} but adds persistence specific fields (id).
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
@Entity
@Table(name = "curation_task_queue", indexes = {
    @Index(name = "idx_ctq_queue", columnList = "queue_name")
})
public class CurationQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "queue_name", nullable = false)
    private String queueName;

    @Column(name = "eperson", nullable = false)
    private String epersonId;

    @Column(name = "submit_time", nullable = false)
    private long submitTime;

    @Column(name = "tasks", nullable = false)
    private String tasks;

    @Column(name = "object_id")
    private String objectId;

    /** Default constructor required by JPA. */
    protected CurationQueueEntry() {
        // default
    }

    /** Convenience constructor. */
    public CurationQueueEntry(String queueName, TaskQueueEntry vo) {
        this.queueName = queueName;
        this.epersonId = vo.getEpersonId();
        this.submitTime = vo.getSubmitTime();
        this.tasks = String.join(",", vo.getTaskNames());
        this.objectId = vo.getObjectId();
    }
    /**
     * Gets the unique identifier of this queue entry.
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Gets the queue name.
     * @return the queue name
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * Sets the queue name.
     * @param queueName the queue name to set
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * Gets the eperson identifier.
     * @return the eperson id
     */
    public String getEpersonId() {
        return epersonId;
    }

    /**
     * Sets the eperson identifier.
     * @param epersonId the eperson id to set
     */
    public void setEpersonId(String epersonId) {
        this.epersonId = epersonId;
    }

    /**
     * Gets the submit time.
     * @return the submit time
     */
    public long getSubmitTime() {
        return submitTime;
    }

    /**
     * Sets the submit time.
     * @param submitTime the submit time to set
     */
    public void setSubmitTime(long submitTime) {
        this.submitTime = submitTime;
    }

    /**
     * Gets the tasks as a comma separated string.
     * @return the tasks
     */
    public String getTasks() {
        return tasks;
    }

    /**
     * Sets the tasks as a comma separated string.
     * @param tasks the tasks to set
     */
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    /**
     * Gets the object identifier.
     * @return the object id
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Sets the object identifier.
     * @param objectId the object id to set
     */
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    /** Rebuild the immutable value object representation. */
    public TaskQueueEntry toTaskQueueEntry() {
        return new TaskQueueEntry(epersonId, String.valueOf(submitTime), tasks, objectId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CurationQueueEntry)) {
            return false;
        }
        CurationQueueEntry that = (CurationQueueEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
