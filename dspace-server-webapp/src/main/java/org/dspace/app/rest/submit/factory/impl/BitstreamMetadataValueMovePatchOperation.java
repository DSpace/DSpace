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
 * Submission "move" PATCH operation.
 *
 * See {@link ItemMetadataValueMovePatchOperation}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamMetadataValueMovePatchOperation extends MetadataValueMovePatchOperation<Bitstream> {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    // this is wired in the pring-dspace-core-services.xml
    BitstreamMetadataValuePathUtils bitstreamMetadataValuePathUtils;

    @Override
    void move(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path, String from)
            throws Exception {
        //"path": "/sections/upload/files/0/metadata/dc.title/2"
        //"abspath": "/files/0/metadata/dc.title/2"
        String absolutePath = getAbsolutePath(path);
        String[] splitTo = absolutePath.split("/");
        bitstreamMetadataValuePathUtils.validate(absolutePath);
        Item item = source.getItem();
        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(splitTo[1])) {

                    String evalFrom = getAbsolutePath(from);
                    String[] splitFrom = evalFrom.split("/");
                    String metadata = splitFrom[3];

                    if (splitTo.length > 4) {
                        String stringTo = splitTo[4];
                        if (splitFrom.length > 4) {
                            String stringFrom = splitFrom[4];

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

    public void setBitstreamMetadataValuePathUtils(BitstreamMetadataValuePathUtils bitstreamMetadataValuePathUtils) {
        this.bitstreamMetadataValuePathUtils = bitstreamMetadataValuePathUtils;
    }
}
