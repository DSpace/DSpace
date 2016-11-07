package org.dspace.app.webui.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.webui.viewer.JSPViewer;
import org.dspace.utils.DSpace;

public class ViewerConfigurationService {
	
	private Map<String,JSPViewer> mapViewers = new HashMap<String,JSPViewer>();

	public Map<String, JSPViewer> getMapViewers() {
		List<JSPViewer> viewers = new DSpace().getServiceManager().getServicesByType(JSPViewer.class);
		Map<String, JSPViewer> mapViewers = new HashMap<String, JSPViewer>();
		for (JSPViewer jspviewer : viewers) {
			mapViewers.put(jspviewer.getViewJSP(), jspviewer);
		}
		return mapViewers;
	}

}
