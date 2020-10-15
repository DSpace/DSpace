/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service.dto;

import org.springframework.stereotype.Service;

@Service
public class NBTopic {
	private String id;
	private String type = "openaireBrokerTopic";
	private String name;
	private String lastEvent;
	private String totalSuggestion;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getLastEvent() {
		return lastEvent;
	}

	public void setLastEvent(String lastEvent) {
		this.lastEvent = lastEvent;
	}

	public String getTotalSuggestion() {
		return totalSuggestion;
	}

	public void setTotalSuggestion(String totalSuggestion) {
		this.totalSuggestion = totalSuggestion;
	}
}
