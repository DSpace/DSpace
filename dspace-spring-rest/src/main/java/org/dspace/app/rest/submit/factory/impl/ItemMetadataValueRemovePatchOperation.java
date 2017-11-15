package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

public class ItemMetadataValueRemovePatchOperation extends RemovePatchOperation<MetadataValueRest> {

	@Autowired
	ItemService itemService;
	
	@Override
	void remove(Context context, Request currentRequest, WorkspaceItem source, String path, Object value) throws Exception {
		String[] split = path.split("/");
		deleteValue(context, source, split[0], split[1]);
	}

	@Override
	protected Class<MetadataValueRest[]> getClassForEvaluation() {
		return MetadataValueRest[].class;
	}
	
	private void deleteValue(Context context, WorkspaceItem source, String target, String index) throws SQLException {
		String[] metadata = Utils.tokenize(target);
		List<MetadataValue> mm = itemService.getMetadata(source.getItem(),  metadata[0], metadata[1], metadata[2], Item.ANY);
		itemService.clearMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], Item.ANY);
		int idx = 0;
		for(MetadataValue m : mm) {
			Integer toDelete = Integer.parseInt(index);
			if(idx != toDelete) {
				itemService.addMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], m.getLanguage(), m.getValue(), m.getAuthority(), m.getConfidence());	
			}
			idx++;
		}
	}

}
