/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.utils.BitstreamMetadataValuePathUtils;
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

    // this is wired in the pring-dspace-core-services.xml
    BitstreamMetadataValuePathUtils bitstreamMetadataValuePathUtils;

    @Override
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {
        //"path": "/sections/upload/files/0/metadata/dc.title/2"
        //"abspath": "/files/0/metadata/dc.title/2"
        String absolutePath = getAbsolutePath(path);
        String[] split = absolutePath.split("/");
        bitstreamMetadataValuePathUtils.validate(absolutePath);
        Item item = source.getItem();
        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);

        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {

                    if (split.length == 4) {
                        deleteValue(context, b, split[3], -1);
                    } else {
                        int toDelete = Integer.parseInt(split[4]);
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

    public void setBitstreamMetadataValuePathUtils(BitstreamMetadataValuePathUtils bitstreamMetadataValuePathUtils) {
        this.bitstreamMetadataValuePathUtils = bitstreamMetadataValuePathUtils;
    }
}
