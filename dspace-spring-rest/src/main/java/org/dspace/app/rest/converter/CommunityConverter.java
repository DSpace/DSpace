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

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
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
	@Autowired
	private BitstreamConverter bitstreamConverter;
	
	@Autowired
	private CollectionConverter collectionConverter;
	
	@Override
	public org.dspace.content.Community toModel(org.dspace.app.rest.model.CommunityRest obj) {
		return (org.dspace.content.Community) super.toModel(obj);
	}

	@Override
	public CommunityRest fromModel(org.dspace.content.Community obj) {
		CommunityRest com = (CommunityRest) super.fromModel(obj);
		Bitstream logo = obj.getLogo();
		if (logo != null) {
			com.setLogo(bitstreamConverter.convert(logo));
		}
		List<Collection> collections = obj.getCollections();
		if (collections != null) {
			List<CollectionRest> collectionsRest = new ArrayList<CollectionRest>();
			for (Collection col : collections) {
				CollectionRest colrest = collectionConverter.fromModel(col);
				collectionsRest.add(colrest);
			}
			com.setCollections(collectionsRest);
		}
		return com;
	}

	@Override
	protected CommunityRest newInstance() {
		return new CommunityRest();
	}
}
