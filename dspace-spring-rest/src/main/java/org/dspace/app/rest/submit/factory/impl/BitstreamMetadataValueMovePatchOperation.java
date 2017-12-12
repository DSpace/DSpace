/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * Submission "move" PATCH operation.
 *
 * See {@link ItemMetadataValueMovePatchOperation}
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class BitstreamMetadataValueMovePatchOperation extends MetadataValueMovePatchOperation<Bitstream> {

	@Autowired
	BitstreamService bitstreamService;

	@Autowired
	ItemService itemService;

	@Override
	void move(Context context, Request currentRequest, WorkspaceItem source, String path, Object from)
			throws Exception {
		String[] splitTo = path.split("/");
		Item item = source.getItem();
		List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
		for (Bundle bb : bundle) {
			int idx = 0;
			for (Bitstream b : bb.getBitstreams()) {
				if (idx == Integer.parseInt(splitTo[0])) {

					String evalFrom = evaluateString((LateObjectEvaluator) from);
					String[] splitFrom = evalFrom.split("/");
					String metadata = splitFrom[0];

					if (splitTo.length > 1) {
						String stringTo = splitTo[1];
						if (splitFrom.length > 1) {
							String stringFrom = splitFrom[1];

							int intTo = Integer.parseInt(stringTo);
							int intFrom = Integer.parseInt(stringFrom);
							moveValue(context, b, metadata, intFrom, intTo);
						}
					}
				}
			}
		}
	}

	@Override
	protected BitstreamService getDSpaceObjectService() {
		return bitstreamService;
	}

}
