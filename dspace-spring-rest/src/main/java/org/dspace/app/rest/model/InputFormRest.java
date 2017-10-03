/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Input Form REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class InputFormRest extends BaseObjectRest<String> {
	public static final String NAME = "input-form";
	public static final String CATEGORY = RestModel.CONFIGURATION;

	private String name;
	
	@JsonProperty(value="isDefault")
	private boolean defaultConf;
	
	private List<InputFormPageRest> pages;

	@Override
	public String getId() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setPages(List<InputFormPageRest> pages) {
		this.pages = pages;
	}
	
	public List<InputFormPageRest> getPages() {
		return pages;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
	
	public void setDefaultConf(boolean isDefault) {
		this.defaultConf = isDefault;
	}
	
	public boolean isDefaultConf() {
		return defaultConf;
	}
	
	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}
}