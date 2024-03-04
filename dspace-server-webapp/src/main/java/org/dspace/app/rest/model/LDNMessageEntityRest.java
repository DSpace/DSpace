/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import org.dspace.app.rest.RestResourceController;

/**
 * The LDN Message REST resource.
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
@SuppressWarnings("serial")
public class LDNMessageEntityRest extends BaseObjectRest<String> {

    public static final String NAME = "message";
    public static final String NAME_PLURALS = "messages";
    public static final String CATEGORY = RestAddressableModel.LDN;

    private String notificationId;

    private Integer queueStatus;

    private String queueStatusLabel;

    private UUID context;

    private UUID object;

    private Integer target;

    private Integer origin;

    private String inReplyTo;

    private String activityStreamType;

    private String coarNotifyType;

    private Integer queueAttempts;

    private Date queueLastStartTime;

    private Date queueTimeout;

    private String notificationType;

    private String message;

    public LDNMessageEntityRest() {
        super();
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURALS;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(Integer queueStatus) {
        this.queueStatus = queueStatus;
    }

    public String getQueueStatusLabel() {
        return queueStatusLabel;
    }

    public void setQueueStatusLabel(String queueStatusLabel) {
        this.queueStatusLabel = queueStatusLabel;
    }

    public UUID getContext() {
        return context;
    }

    public void setContext(UUID context) {
        this.context = context;
    }

    public UUID getObject() {
        return object;
    }

    public void setObject(UUID object) {
        this.object = object;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public Integer getOrigin() {
        return origin;
    }

    public void setOrigin(Integer source) {
        this.origin = source;
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

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
