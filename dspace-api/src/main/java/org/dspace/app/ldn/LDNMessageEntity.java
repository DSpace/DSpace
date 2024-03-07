/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.lang.reflect.Field;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ReloadableEntity;

/**
 * Class representing ldnMessages stored in the DSpace system and, when locally resolvable,
 * some information are stored as dedicated attributes.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "ldn_message")
public class LDNMessageEntity implements ReloadableEntity<String> {

    /**
     * LDN messages interact with a fictitious queue. Scheduled tasks manage the queue.
     */

    /*
     * Notification Type constants
     */
    public static final String TYPE_INCOMING = "Incoming";
    public static final String TYPE_OUTGOING = "Outgoing";

    /**
     * Message must not be processed.
     */
    public static final Integer QUEUE_STATUS_UNTRUSTED_IP = 0;

    /**
    * Message queued, it has to be elaborated.
    */
    public static final Integer QUEUE_STATUS_QUEUED = 1;

    /**
     * Message has been taken from the queue and it's elaboration is in progress.
     */
    public static final Integer QUEUE_STATUS_PROCESSING = 2;

    /**
     * Message has been correctly elaborated.
     */
    public static final Integer QUEUE_STATUS_PROCESSED = 3;

    /**
     * Message has not been correctly elaborated - despite more than "ldn.processor.max.attempts" retryies
     */
    public static final Integer QUEUE_STATUS_FAILED = 4;

    /**
     * Message must not be processed
     */
    public static final Integer QUEUE_STATUS_UNTRUSTED = 5;

    /**
     * Message is not processed since action is not mapped
     */
    public static final Integer QUEUE_STATUS_UNMAPPED_ACTION = 6;

    /**
     * Message queued for retry, it has to be elaborated.
     */
    public static final Integer QUEUE_STATUS_QUEUED_FOR_RETRY = 7;

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "object", referencedColumnName = "uuid")
    private DSpaceObject object;

    @Column(name = "message", columnDefinition = "text")
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

    @Column(name = "activity_stream_type")
    private String activityStreamType;

    @Column(name = "coar_notify_type")
    private String coarNotifyType;

    @Column(name = "source_ip")
    private String sourceIp;

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

    /**
     * 
     * @return the DSpace item related to this message
     */
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

    public String getActivityStreamType() {
        return activityStreamType;
    }

    public void setActivityStreamType(String activityStreamType) {
        this.activityStreamType = activityStreamType;
    }

    public String getCoarNotifyType() {
        return coarNotifyType;
    }

    public void setCoarNotifyType(String coarNotifyType) {
        this.coarNotifyType = coarNotifyType;
    }

    /**
     * 
     * @return The originator of the activity, typically the service responsible for sending the notification
     */
    public NotifyServiceEntity getOrigin() {
        return origin;
    }

    public void setOrigin(NotifyServiceEntity origin) {
        this.origin = origin;
    }

    /**
     * 
     * @return The intended destination of the activity, typically the service which consumes the notification
     */
    public NotifyServiceEntity getTarget() {
        return target;
    }

    public void setTarget(NotifyServiceEntity target) {
        this.target = target;
    }

    /**
     * 
     * @return This property is used when the notification is a direct response to a previous notification;
     * contains an {@link org.dspace.app.ldn.LDNMessageEntity#inReplyTo id}
     */
    public LDNMessageEntity getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(LDNMessageEntity inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    /**
     * 
     * @return This identifies another resource which is relevant to understanding the notification
     */
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

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    @Override
    public String toString() {
        return "LDNMessage id:" + this.getID() + " typed:" + this.getType();
    }

    public static String getNotificationType(LDNMessageEntity ldnMessage) {
        if (ldnMessage.getInReplyTo() != null || ldnMessage.getOrigin() != null) {
            return TYPE_INCOMING;
        }
        return TYPE_OUTGOING;
    }

    public static String getServiceNameForNotifyServ(NotifyServiceEntity serviceEntity) {
        if (serviceEntity != null) {
            return serviceEntity.getName();
        }
        return "self";
    }

    public static String getQueueStatus(LDNMessageEntity ldnMessage) {
        Class<LDNMessageEntity> cl = LDNMessageEntity.class;
        try {
            for (Field f : cl.getDeclaredFields()) {
                String fieldName = f.getName();
                if (fieldName.startsWith("QUEUE_") && (f.get(null) == ldnMessage.getQueueStatus())) {
                    return fieldName;
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
