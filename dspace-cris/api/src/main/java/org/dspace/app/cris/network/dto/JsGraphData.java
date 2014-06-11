/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network.dto;

public class JsGraphData {
	
	private String relation = "";
	
	private String type  = "";

	private String color  = "";
	
	private String lineWidth = "";

	
	private int count;
		
	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getRelation() {
		return relation;
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

	public String getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(String lineWidth) {
		this.lineWidth = lineWidth;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public int getCount()
	{
		return count;
	}
	
}
