/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.content.Community;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Collection in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class CollectionConverter
		extends DSpaceObjectConverter<org.dspace.content.Collection, org.dspace.app.rest.model.CollectionRest> {
	private static final Logger log = Logger.getLogger(CommunityConverter.class);
	@Autowired(required = true)
	private CommunityConverter communityConverter;

	@Override
	public org.dspace.content.Collection toModel(org.dspace.app.rest.model.CollectionRest obj) {
		return (org.dspace.content.Collection) super.toModel(obj);
	}

	@Override
	public CollectionRest fromModel(org.dspace.content.Collection obj, String projection) {
		CollectionRest collectionRest = super.fromModel(obj, projection);
		try {
			for (Community c: obj.getCommunities()) {
				collectionRest.setParentCommunity(communityConverter.fromModel(c));
			}
		} catch (Exception e) {
			log.error("Error setting parent community for community "+collectionRest.getHandle(), e);
		}
		return collectionRest;
	}

	@Override
	protected CollectionRest newInstance() {
		return new CollectionRest();
	}
}
