package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

public class BitstreamRemovePatchOperation extends RemovePatchOperation<String>{

	@Autowired
	ItemService itemService;
	
	@Autowired
	BundleService bundleService;
	
	@Override
	void remove(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
			throws Exception {
		
		Item item = source.getItem();
		List<Bundle> bbb = itemService.getBundles(item, "ORIGINAL");
		Bitstream bitstream = null;
		external : for(Bundle bb : bbb) {
			int idx = 0;
			for(Bitstream b : bb.getBitstreams()) {
				if(idx==Integer.parseInt(path)) {
					bitstream = b;
					break external;
				}
				idx++;
			}
		}
		
        // remove bitstream from bundle..
        // delete bundle if it's now empty
        List<Bundle> bundles = bitstream.getBundles();
        Bundle bundle = bundles.get(0);
		bundleService.removeBitstream(context, bundle, bitstream);
		
        List<Bitstream> bitstreams = bundle.getBitstreams();

        // remove bundle if it's now empty
        if (bitstreams.size() < 1)
        {
            itemService.removeBundle(context, item, bundle);
        }

	}

	@Override
	protected Class<String[]> getClassForEvaluation() {
		return String[].class;
	}

}
