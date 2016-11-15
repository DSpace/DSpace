/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
		for (JSPViewer jspviewer : viewers) {
			mapViewers.put(jspviewer.getViewJSP(), jspviewer);
		}
		return mapViewers;
	}

}
