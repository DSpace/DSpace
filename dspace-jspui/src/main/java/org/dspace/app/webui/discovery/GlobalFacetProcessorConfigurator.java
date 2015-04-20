/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalFacetProcessorConfigurator {
	
	private Map<String,	List<InnerGlobalFacetProcessorConfigurator>> groups;

	public GlobalFacetProcessorConfigurator() {
		groups = new HashMap<String, List<InnerGlobalFacetProcessorConfigurator>>();
	}

	public GlobalFacetProcessorConfigurator(Map<String, List<InnerGlobalFacetProcessorConfigurator>> groups) {
		this.groups = groups;
	}
	
	public Map<String, List<InnerGlobalFacetProcessorConfigurator>> getGroups() {
		return groups;
	}
	
	public void setGroups(Map<String, List<InnerGlobalFacetProcessorConfigurator>> groups) {
		this.groups = groups;
	}
	
	static class InnerGlobalFacetProcessorConfigurator {
		
		private String name;
		
		private List<String> secondLevelFacet;

		InnerGlobalFacetProcessorConfigurator() {
			// TODO Auto-generated constructor stub
		}

		InnerGlobalFacetProcessorConfigurator(String name) {
			this.name = name;
		}
		
		InnerGlobalFacetProcessorConfigurator(String name, List<String> secondLevelFacet) {
			this.name = name;
			this.secondLevelFacet = secondLevelFacet;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getSecondLevelFacet() {
			return secondLevelFacet;
		}

		public void setSecondLevelFacet(List<String> secondLevelFacet) {
			this.secondLevelFacet = secondLevelFacet;
		} 
		
	}

}
