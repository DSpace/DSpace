/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

public class BitstreamFormatRest {

	private Integer id;

	private String shortDescription;

	private String description;

	private String mimetype;

	private int supportLevel;

	private boolean internal;
	
	private List<String> extensions;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public int getSupportLevel() {
		return supportLevel;
	}

	public void setSupportLevel(int supportLevel) {
		this.supportLevel = supportLevel;
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}

	public List<String> getExtensions() {
		return extensions;
	}
	
	public void setExtensions(List<String> extensions) {
		this.extensions = extensions;
	}
}