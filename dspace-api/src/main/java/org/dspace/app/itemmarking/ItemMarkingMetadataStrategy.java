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
import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an item marking Strategy class that tries to mark an item
 * based on the existence of a specific value within the values of a specific
 * metadata field
 * 
 * @author Kostas Stamatis
 * 
 */
public class ItemMarkingMetadataStrategy implements ItemMarkingExtractor {

    @Autowired(required = true)
    protected ItemService itemService;

	private String metadataField;
	Map<String, ItemMarkingInfo> mapping = new HashMap<String, ItemMarkingInfo>();
	
	public ItemMarkingMetadataStrategy() {
	}

	@Override
	public ItemMarkingInfo getItemMarkingInfo(Context context, Item item)
			throws SQLException {
		
		if (metadataField != null && mapping!=null)
		{
			List<MetadataValue> vals = itemService.getMetadataByMetadataString(item, metadataField);
			if (vals.size() > 0)
			{
				for (MetadataValue value : vals){
					String type = value.getValue();
					if (mapping.containsKey(type)){
						return mapping.get(type);
					}
				}
			}
		}
		return null;
	}

	public void setMetadataField(String metadataField) {
		this.metadataField = metadataField;
	}

	public void setMapping(Map<String, ItemMarkingInfo> mapping) {
		this.mapping = mapping;
	}
}
