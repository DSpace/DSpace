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
public class SubmissionPanelRest extends BaseObjectRest<String> {
	
	public static final String NAME = "panel";	
	private String header;
	private boolean mandatory;
	private String panelType;
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
		return "panel";
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

	@Override
	public String getCategory() {
		return SubmissionDefinitionRest.CATEGORY;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	public String getPanelType() {
		return panelType;
	}

	public void setPanelType(String panelType) {
		this.panelType = panelType;
	}

}
