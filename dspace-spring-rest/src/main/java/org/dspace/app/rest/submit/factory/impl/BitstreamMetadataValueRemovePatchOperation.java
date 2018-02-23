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

/**
 * Submission "remove" PATCH operation at metadata Bitstream level.
 *
 * See {@link ItemMetadataValueRemovePatchOperation}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamMetadataValueRemovePatchOperation extends MetadataValueRemovePatchOperation<Bitstream> {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Override
    void remove(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
        throws Exception {
        //"path": "/sections/upload/files/0/metadata/dc.title/2"
        //"abspath": "/files/0/metadata/dc.title/2"
        String[] split = getAbsolutePath(path).split("/");
        Item item = source.getItem();
        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        ;
        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {

                    if (split.length == 4) {
                        deleteValue(context, b, split[3], -1);
                    } else {
                        Integer toDelete = Integer.parseInt(split[4]);
                        deleteValue(context, b, split[3], toDelete);
                    }
                }
                idx++;
            }
        }

    }

    @Override
    protected BitstreamService getDSpaceObjectService() {
        return bitstreamService;
    }

}
