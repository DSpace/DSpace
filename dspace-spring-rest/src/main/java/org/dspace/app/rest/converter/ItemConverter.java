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
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Item in the DSpace API data model and the
 * REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class ItemConverter extends DSpaceObjectConverter<org.dspace.content.Item, org.dspace.app.rest.model.ItemRest> {
	@Autowired(required = true)
	private CollectionConverter collectionConverter;
	@Autowired(required = true)
	private BitstreamConverter bitstreamConverter;

	private static final Logger log = Logger.getLogger(ItemConverter.class);
	
	@Override
	public ItemRest fromModel(org.dspace.content.Item obj) {
		ItemRest item = super.fromModel(obj);
		item.setInArchive(obj.isArchived());
		item.setDiscoverable(obj.isDiscoverable());
		item.setWithdrawn(obj.isWithdrawn());
		item.setLastModified(obj.getLastModified());
		try {
			Collection c = obj.getOwningCollection();
			if (c != null) {
				item.setOwningCollection(collectionConverter.fromModel(c));
			}
		} catch (Exception e) {
			log.error("Error setting owning collection for item"+item.getHandle(), e);
		}
		try {
			Collection c = obj.getTemplateItemOf();
			if (c != null) {
				item.setTemplateItemOf(collectionConverter.fromModel(c));
			}
		} catch (Exception e) {
			log.error("Error setting template item of for item "+item.getHandle(), e);
		}
		List<BitstreamRest> bitstreams = new ArrayList<BitstreamRest>();
		for (Bundle bun : obj.getBundles()) {
			for (Bitstream bit : bun.getBitstreams()) {
				BitstreamRest bitrest = bitstreamConverter.fromModel(bit);
				bitstreams.add(bitrest);
			}
		}
		item.setBitstreams(bitstreams);
		return item;
	}

	@Override
	public org.dspace.content.Item toModel(ItemRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ItemRest newInstance() {
		return new ItemRest();
	}

}
