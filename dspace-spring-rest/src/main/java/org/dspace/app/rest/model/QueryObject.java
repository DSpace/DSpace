package org.dspace.app.rest.model;

import java.util.Map;

public class QueryObject {
	
	private String query;
		 
	private Map<String, String> map;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}
}
