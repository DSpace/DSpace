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
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "remove" operation for deletion of the Bitstream
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamRemovePatchOperation extends RemovePatchOperation<String> {

    @Autowired
    ItemService itemService;

    @Autowired
    BundleService bundleService;

    @Override
    void remove(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {

        String absPath = getAbsolutePath(path);
        Item item = source.getItem();

        if (absPath.isEmpty()) {
            // manage delete all files

            List<Bundle> bbb = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
            for (Bundle bb : bbb) {
                for (Bitstream b : bb.getBitstreams()) {
                    deleteBitstream(context, item, b);
                }
            }

        } else {
            String[] split = absPath.split("/");
            int index = Integer.parseInt(split[1]);

            List<Bundle> bbb = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
            Bitstream bitstream = null;
            external:
                for (Bundle bb : bbb) {
                    int idx = 0;
                    for (Bitstream b : bb.getBitstreams()) {
                        if (idx == index) {
                            bitstream = b;
                            break external;
                        }
                        idx++;
                    }
                }

            deleteBitstream(context, item, bitstream);
        }
    }

    private void deleteBitstream(Context context, Item item, Bitstream bitstream) throws Exception {
        // remove bitstream from bundle..
        // delete bundle if it's now empty
        List<Bundle> bundles = bitstream.getBundles();
        Bundle bundle = bundles.get(0);
        bundleService.removeBitstream(context, bundle, bitstream);

        List<Bitstream> bitstreams = bundle.getBitstreams();

        // remove bundle if it's now empty
        if (bitstreams.size() < 1) {
            itemService.removeBundle(context, item, bundle);
        }

    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }
}
