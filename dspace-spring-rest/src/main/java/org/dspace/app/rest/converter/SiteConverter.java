/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SiteRest;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the community in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class SiteConverter
		extends DSpaceObjectConverter<org.dspace.content.Site, org.dspace.app.rest.model.SiteRest> {
	@Override
	public org.dspace.content.Site toModel(org.dspace.app.rest.model.SiteRest obj) {
		return (org.dspace.content.Site) super.toModel(obj);
	}

	@Override
	public SiteRest fromModel(org.dspace.content.Site obj) {
		return (SiteRest) super.fromModel(obj);
	}

	@Override
	protected SiteRest newInstance() {
		return new SiteRest();
	}
}
