/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * The BitstreamFormat REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class BitstreamFormatRest extends BaseObjectRest<Integer> {
	public static final String NAME = "bitstreamformat";
	
	public static final String CATEGORY = RestModel.CORE;
	
	private String shortDescription;

	private String description;

	private String mimetype;

	private int supportLevel;

	private boolean internal;

	private List<String> extensions;

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

	@JsonIgnore
	@Override
	public String getCategory() {
		return CATEGORY;
	}
	
	@Override
	public String getType() {
		return NAME;
	}

	@Override
	@JsonIgnore
	public Class getController() {
		return RestResourceController.class;
	}
}