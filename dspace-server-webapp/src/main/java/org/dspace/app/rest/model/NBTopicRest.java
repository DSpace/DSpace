/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

public class NBTopicRest extends BaseObjectRest<String> {

	private static final long serialVersionUID = -7455358581579629244L;

	public static final String NAME_PLURAL = "nbtopics";
	public static final String NAME = "nbtopic";
	public static final String CATEGORY = RestAddressableModel.INTEGRATION;
		
	private String id;
	private String type = "openaireBrokerTopic";
	private String name;
	private String lastEvent;
	private String totalSuggestions;

	
	@Override
	public String getType() {
		return NAME;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastEvent() {
		return lastEvent;
	}

	public void setLastEvent(String lastEvent) {
		this.lastEvent = lastEvent;
	}

	public String getTotalSuggestions() {
		return totalSuggestions;
	}

	public void setTotalSuggestions(String totalSuggestions) {
		this.totalSuggestions = totalSuggestions;
	}

	public void setType(String type) {
		this.type = type;
	}

}
