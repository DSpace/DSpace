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
	
	/*
    public static final String ORIGINAL_ID = "original_id";
    public static final String TITLE = "title";
    public static final String TOPIC = "topic";
    public static final String TRUST = "trust";
    public static final String MESSAGE = "message";
    public static final String EVENT_ID = "event_id";
    public static final String RESOURCE_UUID = "resource_uuid";
    public static final String LAST_UPDATE = "last_update";

	 */
	
    @Field("original_id")
	private String originalId;
	
    @Field("title")
	private String title;
	
    @Field("topic")
	private String topic;
	
    @Field("trust")
	private String trust;
	
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

	public String getTrust() {
		return trust;
	}

	public void setTrust(String trust) {
		this.trust = trust;
	}


	public MessageDto getMessage() {
		return message;
	}

	public void setMessage(MessageDto message) {
		this.message = message;
	}

	public String getHashString() {
		return "originalId=" + originalId + ", title=" + title + ", topic=" + topic + ", trust=" + trust
				+ ", message=" + message;
	}
	
	

}
