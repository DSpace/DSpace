/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "remove" PATCH operation to remove bitstream alternative content metadata tag.
 *
 * @author Oscar Chacón (oscar at escire.net)
 */
public class AlternativeContentBitstreamRemovePatchOperation extends MetadataValueRemovePatchOperation<Bitstream> {

    private static final String ALTERNATIVE_CONTENT_METADATA_NAME = "dc.type";

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Override
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {
        //"path": "/sections/upload/files/0/alternativecontent"
        //"abspath": "/files/0/alternativecontent"
        String absolutePath = getAbsolutePath(path);
        String[] split = absolutePath.split("/");

        Item item = source.getItem();
        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);

        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {
                    deleteValue(context, b, ALTERNATIVE_CONTENT_METADATA_NAME, -1);
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
