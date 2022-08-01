/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * The class for maintaining CMDI file in the right bundle.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class CMDIFileBundleMaintainer {

    private static final String hasCMDYesOptionValue = "yes";

    private static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private static ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private CMDIFileBundleMaintainer() {}

    /**
     * If the admin has selected the option to upload the CMDI file, move the CMDI file from the ORIGINAL bundle
     * to the METADATA bundle and vice versa.
     *
     * @param context of the HTTP request or of the Testing context
     * @param item of the InProgressSubmission object
     * @param mdv of the 'local.hasCMDI' metadata
     */
    public static void updateCMDIFileBundle(Context context, Item item, List<MetadataValue> mdv)
            throws SQLException, AuthorizeException, IOException {
        List<Bundle> bundleMETADATA = itemService.getBundles(item, Constants.METADATA_BUNDLE_NAME);
        List<Bundle> bundleORIGINAL = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);

        String targetBundle = "";
        List<Bundle> bundleToProcess = null;

        if (!bundleMETADATA.isEmpty() && mdv.isEmpty()) {
            targetBundle = Constants.CONTENT_BUNDLE_NAME;
            bundleToProcess = bundleMETADATA;
        } else if (!bundleORIGINAL.isEmpty() && !mdv.isEmpty() &&
                StringUtils.equals(hasCMDYesOptionValue, mdv.get(0).getValue())) {
            targetBundle = Constants.METADATA_BUNDLE_NAME;
            bundleToProcess = bundleORIGINAL;
        }

        for (Bundle bundle : CollectionUtils.emptyIfNull(bundleToProcess)) {
            for (Bitstream bitstream : bundle.getBitstreams()) {
                if (!bitstream.getName().toLowerCase().endsWith(".cmdi")) {
                    continue;
                }

                // change bundle only for cmdi file
                List<Bundle> targetBundles = itemService.getBundles(item, targetBundle);
                InputStream inputStream = bitstreamService.retrieve(context, bitstream);

                // Create a new Bitstream
                Bitstream source = null;

                if (targetBundles.size() < 1) {
                    source = itemService.createSingleBitstream(context, inputStream, item, targetBundle);
                } else {
                    // we have a bundle already, just add bitstream
                    source = bitstreamService.create(context, targetBundles.get(0), inputStream);
                }

                source.setName(context, bitstream.getName());
                source.setSource(context, bitstream.getSource());
                source.setFormat(context, bitstream.getFormat(context));

                // add the bitstream to the right bundle
                bitstreamService.update(context, source);
                itemService.update(context, item);

                // remove the bitstream from the bundle where it shouldn't be
                bitstreamService.delete(context, bitstream);
            }
        }
    }
}
