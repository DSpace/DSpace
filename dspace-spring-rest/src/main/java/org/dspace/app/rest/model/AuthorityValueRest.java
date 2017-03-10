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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The AuthorityValue REST Resource. It represents a single entry in an
 * authority list used or not by a metadata
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorityValueRest extends BaseObjectRest<String> {
	public static final String NAME = "authorityvalue";

	private AuthorityListRest authorityList;

	private String preferredLabel;

	private List<String> variants;

	public AuthorityListRest getAuthorityList() {
		return authorityList;
	}

	public void setAuthorityList(AuthorityListRest authorityList) {
		this.authorityList = authorityList;
	}

	public String getPreferredLabel() {
		return preferredLabel;
	}

	public void setPreferredLabel(String preferredLabel) {
		this.preferredLabel = preferredLabel;
	}

	public List<String> getVariants() {
		return variants;
	}

	public void setVariants(List<String> variants) {
		this.variants = variants;
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