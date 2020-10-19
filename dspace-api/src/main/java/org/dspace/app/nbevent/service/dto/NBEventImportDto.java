/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service.dto;

import org.apache.solr.client.solrj.beans.Field;

public class NBEventImportDto {

    @Field("original_id")
    private String originalId;

    @Field("title")
    private String title;

    @Field("topic")
    private String topic;

    @Field("trust")
    private long trust;

    @Field("message")
    private MessageDto message;

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getTrust() {
        return trust;
    }

    public void setTrust(long trust) {
        this.trust = trust;
    }

    public MessageDto getMessage() {
        return message;
    }

    public void setMessage(MessageDto message) {
        this.message = message;
    }

    public String getHashString() {
        return "originalId=" + originalId + ", title=" + title + ", topic=" + topic + ", trust=" + trust + ", message="
                + message;
    }

}
