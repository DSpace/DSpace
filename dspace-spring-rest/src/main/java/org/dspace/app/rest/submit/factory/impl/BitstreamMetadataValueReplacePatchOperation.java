package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

public class BitstreamMetadataValueReplacePatchOperation extends ReplacePatchOperation<MetadataValueRest> {

	@Autowired
	BitstreamService bitstreamService;
	
	@Autowired
	ItemService itemService;
	
	@Override
	void replace(Context context, Request currentRequest, WorkspaceItem source, String path, Object value) throws Exception {
		String[] split = path.split("/");
		Item item = source.getItem();
		List<Bundle> bundle = itemService.getBundles(item, "ORIGINAL");;
		for(Bundle bb : bundle) {
			int idx = 0;
			for(Bitstream b : bb.getBitstreams()) {
				if(idx==Integer.parseInt(split[0])) {
					replaceValue(context, b, split[2], evaluateObject((LateObjectEvaluator)value));
				}
				idx++;
			}
		}
	}

	@Override
	protected Class<MetadataValueRest[]> getClassForEvaluation() {
		return MetadataValueRest[].class;
	}

	private void replaceValue(Context context, Bitstream source, String target, MetadataValueRest[] list) throws SQLException {		
		String[] metadata = Utils.tokenize(target);
		bitstreamService.clearMetadata(context, source, metadata[0], metadata[1], metadata[2], Item.ANY);
		for(MetadataValueRest ll : list) {
			bitstreamService.addMetadata(context, source, metadata[0], metadata[1], metadata[2], ll.getLanguage(), ll.getValue(), ll.getAuthority(), ll.getConfidence());
		}		
	}
}
