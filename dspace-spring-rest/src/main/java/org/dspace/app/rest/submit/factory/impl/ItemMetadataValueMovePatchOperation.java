/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "move" PATCH operation.
 * 
 * It is possible to rearrange the metadata values using the move operation. For
 * instance to put the 3rd author as 1st author you need to run:
 * 
 * <code>
 * curl -X PATCH http://${dspace.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "move", "from": "
 * /sections/traditionalpageone/dc.contributor.author/2", "path": "
 * /sections/traditionalpageone/dc.contributor.author/0"}]'
 * </code>
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class ItemMetadataValueMovePatchOperation extends MovePatchOperation<MetadataEntryRest> {

	@Autowired
	ItemService itemService;

	@Override
	void move(Context context, Request currentRequest, WorkspaceItem source, String string, Object value)
			throws Exception {
		// TODO not yet implemented
	}

	@Override
	protected Class<MetadataEntryRest[]> getArrayClassForEvaluation() {
		return MetadataEntryRest[].class;
	}

	@Override
	protected Class<MetadataEntryRest> getClassForEvaluation() {		
		return MetadataEntryRest.class;
	}

}
