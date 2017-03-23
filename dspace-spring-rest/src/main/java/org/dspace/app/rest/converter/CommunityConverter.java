/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CommunityRest;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the community in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class CommunityConverter
		extends DSpaceObjectConverter<org.dspace.content.Community, org.dspace.app.rest.model.CommunityRest> {
	@Override
	public org.dspace.content.Community toModel(org.dspace.app.rest.model.CommunityRest obj) {
		return (org.dspace.content.Community) super.toModel(obj);
	}

	@Override
	public CommunityRest fromModel(org.dspace.content.Community obj) {
		return (CommunityRest) super.fromModel(obj);
	}

	@Override
	protected CommunityRest newInstance() {
		return new CommunityRest();
	}
}
