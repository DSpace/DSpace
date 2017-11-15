package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

public class ItemMetadataValueAddPatchOperation extends AddPatchOperation<MetadataValueRest> {

	@Autowired
	ItemService itemService;
	
	@Override
	void add(Context context, Request currentRequest, WorkspaceItem source, String path, Object value) throws Exception {
		String[] split = path.split("/");
		addValue(context, source, split[0], evaluateObject((LateObjectEvaluator)value));
	}

	@Override
	protected Class<MetadataValueRest[]> getClassForEvaluation() {
		return MetadataValueRest[].class;
	}

	private void addValue(Context context, WorkspaceItem source, String target, MetadataValueRest[] list) throws SQLException {
		String[] metadata = Utils.tokenize(target);
		for(MetadataValueRest ll : list) {
			itemService.addMetadata(context, source.getItem(), metadata[0], metadata[1], metadata[2], ll.getLanguage(), ll.getValue(), ll.getAuthority(), ll.getConfidence());
		}
	}
}
