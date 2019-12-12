/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
public class ItemCorrectionProvider extends AbstractVersionProvider {

    Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemCorrectionProvider.class);

    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;

    @Autowired(required = true)
    protected XmlWorkflowItemService workflowItemService;

    public WorkspaceItem createNewItemAndAddItInWorkspace(Context context, Collection collection, Item nativeItem)
            throws AuthorizeException, IOException, SQLException {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item itemNew = workspaceItem.getItem();
        // copy metadata from native item to corrected item
        copyMetadata(context, itemNew, nativeItem);
        context.turnOffAuthorisationSystem();
        // copy bundles and bitstreams from native item
        createBundlesAndAddBitstreams(context, itemNew, nativeItem);
        context.restoreAuthSystemState();
        workspaceItem.setItem(itemNew);
        workspaceItemService.update(context, workspaceItem);

        log.info("Created new correction item " + workspaceItem.getItem().getID().toString()
                + "from item " + nativeItem.getID().toString());

        return workspaceItem;

    }

    public XmlWorkflowItem updateNativeItemWithCorrection(Context context, XmlWorkflowItem workflowItem,
            Item correctionItem, Item nativeItem) throws AuthorizeException, IOException, SQLException {

        // clear all metadata entries from native item
        itemService.clearMetadata(context, nativeItem, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        // copy metadata from corrected item to native item
        copyMetadata(context, nativeItem, correctionItem);
        context.turnOffAuthorisationSystem();
        // copy bundles and bitstreams of native item
        updateBundlesAndBitstreams(context, correctionItem, nativeItem);
        log.info("Updated new item " + nativeItem.getID().toString()
                + " with correction item " + correctionItem.getID().toString());

        workflowItem.setItem(nativeItem);
        workflowItemService.update(context, workflowItem);
        itemService.delete(context, correctionItem);
        log.info("Deleted correction item " + correctionItem.getID().toString());

        context.restoreAuthSystemState();




        return workflowItem;
    }

    protected void updateBundlesAndBitstreams(Context c, Item itemNew, Item nativeItem)
            throws SQLException, AuthorizeException, IOException {

        // Update only default bundle
        Bundle nativeDefaultBundle = nativeItem.getBundles(Constants.DEFAULT_BUNDLE_NAME).get(0);
        Bundle correctedDefaultBundle = itemNew.getBundles(Constants.DEFAULT_BUNDLE_NAME).get(0);;

        List<Bitstream> nativeBitstreams = nativeDefaultBundle.getBitstreams();
        for (Bitstream bitstreamCorrected : correctedDefaultBundle.getBitstreams()) {
            // check if new bitstream exists in native bundle
            Bitstream nativeBitstream = findBitstreamByChecksum(nativeDefaultBundle, bitstreamCorrected.getChecksum());
            if (nativeBitstream != null) {
                // update native bitstream metadata
                nativeBitstream.getMetadata();
                bitstreamService.clearMetadata(c, nativeBitstream, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                List<MetadataValue> metadataValues = bitstreamService
                        .getMetadata(bitstreamCorrected, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

                for (MetadataValue metadataValue : metadataValues) {
                    bitstreamService.addMetadata(c, nativeBitstream, metadataValue.getMetadataField(),
                        metadataValue.getLanguage(), metadataValue.getValue(), metadataValue.getAuthority(),
                        metadataValue.getConfidence());
                }
                bitstreamService.update(c, nativeBitstream);
            } else {
                // Add new bitstram to native bundle
                // Metadata and additional information like internal identifier,
                // file size, checksum, and checksum algorithm are set by the bitstreamStorageService.clone(...)
                // and respectively bitstreamService.clone(...) method.
                Bitstream bitstreamNew =  bitstreamStorageService.clone(c, bitstreamCorrected);

                bundleService.addBitstream(c, nativeDefaultBundle, bitstreamNew);

                // NOTE: bundle.addBitstream() causes Bundle policies to be inherited by default.
                // So, we need to REMOVE any inherited TYPE_CUSTOM policies before copying over the correct ones.
                authorizeService.removeAllPoliciesByDSOAndType(c, bitstreamNew, ResourcePolicy.TYPE_CUSTOM);

                // Now, we need to copy the TYPE_CUSTOM resource policies from old bitstream
                // to the new bitstream, like we did above for bundles
                List<ResourcePolicy> bitstreamPolicies =
                    authorizeService.findPoliciesByDSOAndType(c, nativeBitstream, ResourcePolicy.TYPE_CUSTOM);
                authorizeService.addPolicies(c, bitstreamPolicies, bitstreamNew);

                if (correctedDefaultBundle.getPrimaryBitstream() != null && correctedDefaultBundle.getPrimaryBitstream()
                                                                              .equals(nativeBitstream)) {
                    nativeDefaultBundle.setPrimaryBitstreamID(bitstreamNew);
                }

                bitstreamService.update(c, bitstreamNew);
            }

        }
        bundleService.update(c, nativeDefaultBundle);
    }

    protected Bitstream findBitstreamByChecksum(Bundle bundle, String bitstreamChecksum) {
        List<Bitstream> bitstreams = bundle.getBitstreams();
        for (Bitstream bitstream : bitstreams) {
            if (bitstream.getChecksum().equals(bitstreamChecksum)) {
                return bitstream;
            }
        }

        return null;
    }
}
