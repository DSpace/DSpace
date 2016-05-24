package org.dspace.app.cris.integration.authority;

import java.util.Map;

public class ItemMetadataImportFillerConfiguration {
	private Map<String, MappingDetails> mapping;
	
	private Boolean updateEnabled;
	
	public Map<String, MappingDetails> getMapping() {
		return mapping;
	}

	public void setMapping(Map<String, MappingDetails> mapping) {
		this.mapping = mapping;
	}

	public void setUpdateEnabled(Boolean updateEnabled) {
		this.updateEnabled = updateEnabled;
	}
	
	public Boolean getUpdateEnabled() {
		return updateEnabled;
	}
	
	public static class MappingDetails{
		private String shortName;
		//private String converter;
		private boolean useAll;
		
		private Integer visibility;
		
		public String getShortName() {
			return shortName;
		}
		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
		public boolean isUseAll() {
			return useAll;
		}
		public void setUseAll(boolean useAll) {
			this.useAll = useAll;
		}
		public Integer getVisibility() {
			return visibility;
		}
		public void setVisibility(Integer visibility){
			this.visibility = visibility;
		}
	}

}
