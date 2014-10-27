/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemmarking;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This is an item marking Strategy class that tries to mark an item
 * based on the collection the items belong to
 * 
 * @author Kostas Stamatis
 * 
 */
public class ItemMarkingCollectionStrategy implements ItemMarkingExtractor {

	Map<String, ItemMarkingInfo> mapping = new HashMap<String, ItemMarkingInfo>();
	
	public ItemMarkingCollectionStrategy() {
	}

	@Override
	public ItemMarkingInfo getItemMarkingInfo(Context context, Item item)
			throws SQLException {
		
		if (mapping!=null){
			for (Collection collection : item.getCollections()){
				if (mapping.containsKey(collection.getHandle())){
					return mapping.get(collection.getHandle());
				}
			}
		}
		
		return null;
	}

	public void setMapping(Map<String, ItemMarkingInfo> mapping) {
		this.mapping = mapping;
	}
}
