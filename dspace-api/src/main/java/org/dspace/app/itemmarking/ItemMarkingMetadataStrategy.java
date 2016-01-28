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

import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

/**
 * This is an item marking Strategy class that tries to mark an item
 * based on the existence of a specific value within the values of a specific
 * metadata field
 * 
 * @author Kostas Stamatis
 * 
 */
public class ItemMarkingMetadataStrategy implements ItemMarkingExtractor {

	private String metadataField;
	Map<String, ItemMarkingInfo> mapping = new HashMap<String, ItemMarkingInfo>();
	
	public ItemMarkingMetadataStrategy() {
	}

	@Override
	public ItemMarkingInfo getItemMarkingInfo(Context context, Item item)
			throws SQLException {
		
		if (metadataField != null && mapping!=null)
		{
			Metadatum[] vals = item.getMetadataByMetadataString(metadataField);
			if (vals.length > 0)
			{
				for (Metadatum value : vals){
					String type = value.value;
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
