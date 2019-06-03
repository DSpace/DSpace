/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Submission "add" PATCH operation at metadata Bitstream level.
 *
 * See {@link ItemMetadataValueAddPatchOperation}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamMetadataValueAddPatchOperation extends MetadataValueAddPatchOperation<Bitstream> {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Override
    void add(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
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
                        List<MetadataValueRest> list = evaluateArrayObject((LateObjectEvaluator) value);
                        replaceValue(context, b, split[3], list);

                    } else {
                        // call with "-" or "index-based" we should receive only single
                        // object member
                        MetadataValueRest object = evaluateSingleObject((LateObjectEvaluator) value);
                        // check if is not empty
                        List<MetadataValue> metadataByMetadataString =
                            bitstreamService.getMetadataByMetadataString(b,split[3]);
                        Assert.notEmpty(metadataByMetadataString);
                        if (split.length > 4) {
                            String controlChar = split[4];
                            switch (controlChar) {
                                case "-":
                                    addValue(context, b, split[3], object, -1);
                                    break;
                                default:
                                    // index based

                                    int index = Integer.parseInt(controlChar);
                                    if (index > metadataByMetadataString.size()) {
                                        throw new IllegalArgumentException(
                                            "The specified index MUST NOT be greater than the number of elements in " +
                                                "the array");
                                    }
                                    addValue(context, b, split[3], object, index);

                                    break;
                            }
                        }
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
