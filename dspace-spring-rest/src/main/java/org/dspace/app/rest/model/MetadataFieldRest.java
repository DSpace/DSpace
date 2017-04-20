/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The MetadataField REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataFieldRest extends BaseObjectRest<Integer> {
	public static final String NAME = "metadatafield";

	private String element;

	private String qualifier;

	private String scopeNote;

	public String getElement() {
		return element;
	}

	public void setElement(String element) {
		this.element = element;
	}

	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	public String getScopeNote() {
		return scopeNote;
	}

	public void setScopeNote(String scopeNote) {
		this.scopeNote = scopeNote;
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