/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network.dto;

public class JsGraphNodeData {
		
	
	private String profile = "";
	
	private String type  = "";

	private String color = "";
		
	private String modeStyle = "";
	
	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getProfile() {
		return profile;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public void setModeStyle(String modeStyle) {
		this.modeStyle = modeStyle;
	}

	public String getModeStyle() {
		return this.modeStyle;
	}
}
