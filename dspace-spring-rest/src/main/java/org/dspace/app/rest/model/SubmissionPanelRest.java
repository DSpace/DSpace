/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import java.io.Serializable;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The SubmissionPanel REST Resource. It is not addressable directly, only used
 * as inline object in the SubmissionDefinition resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@JsonInclude(value=Include.NON_NULL)
public class SubmissionPanelRest implements RestModel {
	public static final String NAME = "panel";	
	private String header;
	private String id;
	private boolean mandatory;
	private String type;
	private ScopeEnum scope;
	private SubmissionVisibilityRest visibility;

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ScopeEnum getScope() {
		return scope;
	}

	public void setScope(ScopeEnum scope) {
		this.scope = scope;
	}

	public SubmissionVisibilityRest getVisibility() {
		return visibility;
	}

	public void setVisibility(SubmissionVisibilityRest visibility) {
		if (visibility != null && (visibility.getMain() != null || visibility.getOther() != null)) {
			this.visibility = visibility;
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getCategory() {
		return SubmissionDefinitionRest.CATEGORY;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}

}
