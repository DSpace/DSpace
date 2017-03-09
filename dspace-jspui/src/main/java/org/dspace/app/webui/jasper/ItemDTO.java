package org.dspace.app.webui.jasper;

import java.util.Map;

public class ItemDTO {
	
	private Map<String, String> metadata;
	
	public Map<String, String> getMetadata() {
		return metadata;
	}

	public ItemDTO(Map<String, String> metadata) {
		this.metadata = metadata;
	}

}
