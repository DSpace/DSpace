package org.dspace.app.rest.submit;

import java.util.Map;

import org.dspace.app.rest.submit.factory.impl.PatchOperation;

public class PatchConfigurationService {

	private Map<String, Map<String, PatchOperation>> map;

	public Map<String, Map<String, PatchOperation>> getMap() {
		return map;
	}

	public void setMap(Map<String, Map<String, PatchOperation>> map) {
		this.map = map;
	}
	
}
