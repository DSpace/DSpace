package org.dspace.app.webui.cris.components;

import java.util.List;
import java.util.Map;

public class ExploreMapProcessors {
	private Map<String, List<ExploreProcessor>> processorsMap;
	
	public void setProcessorsMap(Map<String, List<ExploreProcessor>> processorsMap) {
		this.processorsMap = processorsMap;
	}
	
	public Map<String, List<ExploreProcessor>> getProcessorsMap() {
		return processorsMap;
	}
}
