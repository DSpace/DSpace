/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;
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
 * Submission "replace" operation to replace metadata in the Bitstream
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamMetadataValueReplacePatchOperation extends MetadataValueReplacePatchOperation<Bitstream> {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Override
    void replace(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {
        //"path": "/sections/upload/files/0/metadata/dc.title/2"
        //"abspath": "/files/0/metadata/dc.title/2"
        String[] split = getAbsolutePath(path).split("/");
        Item item = source.getItem();
        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {
                    replace(context, b, split, value);
                }
                idx++;
            }
        }
    }

    private void replace(Context context, Bitstream b, String[] split, Object value)
        throws SQLException, IllegalArgumentException, IllegalAccessException {
        String mdString = split[3];
        List<MetadataValue> metadataByMetadataString = bitstreamService.getMetadataByMetadataString(b, mdString);
        Assert.notEmpty(metadataByMetadataString);

        int index = Integer.parseInt(split[4]);
        // if split size is one so we have a call to initialize or replace
        if (split.length == 5) {
            MetadataValueRest obj = evaluateSingleObject((LateObjectEvaluator) value);
            replaceValue(context, b, mdString, metadataByMetadataString, obj, index);
        } else {
            //"path": "/sections/upload/files/0/metadata/dc.title/2/language"
            if (split.length > 5) {
                setDeclaredField(context, b, value, mdString, split[5], metadataByMetadataString, index);
            }
        }
    }

    @Override
    protected BitstreamService getDSpaceObjectService() {
        return bitstreamService;
    }
}
