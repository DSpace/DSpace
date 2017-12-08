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
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "move" PATCH operation.
 *
 * See {@link ItemMetadataValueMovePatchOperation}
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class BitstreamMetadataValueMovePatchOperation extends MovePatchOperation<MetadataEntryRest> {

	@Autowired
	BitstreamService bitstreamService;
	
	
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
