/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.content.DSpaceObject;
import org.dspace.core.ReloadableEntity;

/**
 * Class representing ldnMessages stored in the DSpace system.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "ldn_messages")
public class LDNMessageEntity implements ReloadableEntity<String> {

    public static final Integer QUEUE_STATUS_QUEUED = 1;
    public static final Integer QUEUE_STATUS_PROCESSING = 2;
    public static final Integer QUEUE_STATUS_PROCESSED = 3;
    public static final Integer QUEUE_STATUS_FAILED = 4;

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "object", referencedColumnName = "uuid")
    private DSpaceObject object;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "type")
    private String type;

    @Column(name = "queue_status")
    private Integer queueStatus;

    @Column(name = "queue_attempts")
    private Integer queueAttempts = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "queue_last_start_time")
    private Date queueLastStartTime = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "queue_timeout")
    private Date queueTimeout = null;

    @ManyToOne
    @JoinColumn(name = "origin", referencedColumnName = "id")
    private NotifyServiceEntity origin;

    @ManyToOne
    @JoinColumn(name = "target", referencedColumnName = "id")
    private NotifyServiceEntity target;

    @ManyToOne
    @JoinColumn(name = "inReplyTo", referencedColumnName = "id")
    private LDNMessageEntity inReplyTo;

    @ManyToOne
    @JoinColumn(name = "context", referencedColumnName = "uuid")
    private DSpaceObject context;

    protected LDNMessageEntity() {

    }

    public LDNMessageEntity(String id) {
        this.id = id;
    }

    @Override
    public String getID() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DSpaceObject getObject() {
        return object;
    }

    public void setObject(DSpaceObject object) {
        this.object = object;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NotifyServiceEntity getOrigin() {
        return origin;
    }

    public void setOrigin(NotifyServiceEntity origin) {
        this.origin = origin;
    }

    public NotifyServiceEntity getTarget() {
        return target;
    }

    public void setTarget(NotifyServiceEntity target) {
        this.target = target;
    }

    public LDNMessageEntity getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(LDNMessageEntity inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public DSpaceObject getContext() {
        return context;
    }

    public void setContext(DSpaceObject context) {
        this.context = context;
    }

    public Integer getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(Integer queueStatus) {
        this.queueStatus = queueStatus;
    }

    public Integer getQueueAttempts() {
        return queueAttempts;
    }

    public void setQueueAttempts(Integer queueAttempts) {
        this.queueAttempts = queueAttempts;
    }

    public Date getQueueLastStartTime() {
        return queueLastStartTime;
    }

    public void setQueueLastStartTime(Date queueLastStartTime) {
        this.queueLastStartTime = queueLastStartTime;
    }

    public Date getQueueTimeout() {
        return queueTimeout;
    }

    public void setQueueTimeout(Date queueTimeout) {
        this.queueTimeout = queueTimeout;
    }

    @Override
    public String toString() {
        return "LDNMessage id:" + this.getID() + " typed:" + this.getType();
    }
}
