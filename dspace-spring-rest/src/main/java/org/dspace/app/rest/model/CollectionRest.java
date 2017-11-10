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

/**
 * The Collection REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@LinksRest(links = {
		@LinkRest(name = LicenseRest.NAME, linkClass = LicenseRest.class, method = "getLicenseCollection", optional = true),
		@LinkRest(name = DefaultAccessConditionRest.NAME, linkClass = DefaultAccessConditionRest.class, method = "getDefaultBitstreamPoliciesForCollection", optional = true)
})
public class CollectionRest extends DSpaceObjectRest {
	public static final String NAME = "collection";
	public static final String CATEGORY = RestModel.CORE;

	@JsonIgnore
	private LicenseRest license;
	@JsonIgnore
	private List<DefaultAccessConditionRest> defaultBitstreamsPolicies;

	@JsonIgnore
	private BitstreamRest logo;

	public BitstreamRest getLogo() {
		return logo;
	}

	public void setLogo(BitstreamRest logo) {
		this.logo = logo;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public String getType() {
		return NAME;
	}

	public LicenseRest getLicense() {
		return license;
	}

	public void setLicense(LicenseRest license) {
		this.license = license;
	}

	public List<DefaultAccessConditionRest> getDefaultBitstreamsPolicies() {
		return defaultBitstreamsPolicies;
	}

	public void setDefaultBitstreamsPolicies(List<DefaultAccessConditionRest> defaultBitstreamsPolicies) {
		this.defaultBitstreamsPolicies = defaultBitstreamsPolicies;
	}


}
