/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.springframework.beans.factory.annotation.Autowired;
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

	private static final Logger log = Logger.getLogger(CommunityConverter.class);
	@Autowired(required = true)
	private CollectionConverter collectionConverter;

	@Override
	public org.dspace.content.Community toModel(org.dspace.app.rest.model.CommunityRest obj) {
		return (org.dspace.content.Community) super.toModel(obj);
	}

	@Override
	public CommunityRest fromModel(org.dspace.content.Community obj) {
		CommunityRest communityRest = super.fromModel(obj);
		try {
			for (Community c: obj.getParentCommunities()) {
				communityRest.setParentCommunity(fromModel(c));
			}
		} catch (Exception e) {
			log.error("Error setting parent community for community "+communityRest.getHandle(), e);
		}

		/*
		try {
			List<CommunityRest> commrestlist = new ArrayList<>();
			for (Community c: obj.getSubcommunities()) {
				commrestlist.add(fromModel(c));
			}
			communityRest.setSubcommunities(commrestlist);
		} catch (Exception e) {
			log.error("Error setting subcommunities for community "+communityRest.getHandle(), e);
		}
		try {
			List<CollectionRest> collrestlist = new ArrayList<>();
			for (Collection c: obj.getCollections()) {
				collrestlist.add(collectionConverter.fromModel(c));
			}
			communityRest.setCollections(collrestlist);
		} catch (Exception e) {
			log.error("Error setting collections for community "+communityRest.getHandle(), e);
		}
		*/
		return communityRest;
	}

	@Override
	protected CommunityRest newInstance() {
		return new CommunityRest();
	}
}
