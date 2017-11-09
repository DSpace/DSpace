/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The License text REST resource.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class LicenseRest implements RestModel {
	
	public static final String NAME = "license";
	public static final String CATEGORY = RestModel.CORE;

	private boolean custom = false;
	private String text; 
	
	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public String getType() {
		return NAME;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
