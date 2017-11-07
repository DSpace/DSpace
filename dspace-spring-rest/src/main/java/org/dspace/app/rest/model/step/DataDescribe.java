/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.model.MetadataValueRest;

public class DataDescribe implements SectionData {

	private Map<String, List<MetadataValueRest>> metadata = new HashMap<>();

	public Map<String, List<MetadataValueRest>> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, List<MetadataValueRest>> metadata) {
		this.metadata = metadata;
	}

}
