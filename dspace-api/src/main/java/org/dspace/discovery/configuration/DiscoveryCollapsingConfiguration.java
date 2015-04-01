/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;



public class DiscoveryCollapsingConfiguration {
    
	private String groupIndexFieldName;

	private int groupLimit;
	
    public String getGroupIndexFieldName() {
		return groupIndexFieldName;
	}
	public void setGroupIndexFieldName(String groupField) {
		this.groupIndexFieldName = groupField;
	}
	public int getGroupLimit() {
		return groupLimit;
	}
	public void setGroupLimit(int groupLimit) {
		this.groupLimit = groupLimit;
	}
    public String getGroupField() {
		return getGroupIndexFieldName() + "_group";
	}
	public String getGlobalFacet() {
		return getGroupIndexFieldName() + "_filter";
	}
    
}