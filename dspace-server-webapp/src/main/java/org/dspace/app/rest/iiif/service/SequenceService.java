/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.CanvasItemsGenerator;
import org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating a {@code Sequence}. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 */
@RequestScope
@Component
public class SequenceService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SequenceService.class);

    /*
     * The counter tracks the position of the bitstream in the list and is used to create the canvas identifier.
     * The order of bitstreams (and thus page order in documents) is determined by position in the DSpace
     * bundle.
     */
    int counter = 0;

    @Autowired
    CanvasItemsGenerator sequenceGenerator;

    @Autowired
    CanvasService canvasService;

    public SequenceService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Returns a sequence generator that has been configured with canvases and an optional
     * rendering link. (@abollini will update.)
     *
     * @param item the DSpace item
     * @param context the DSpace context
     * @return a sequence generator
     */
    public CanvasItemsGenerator getSequence(Item item, Context context) {

        sequenceGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/sequence/s0");
        addRendering(item, context);

        return sequenceGenerator;
    }

    /**
     * This method adds a canvas to the sequence for each item in the list of DSpace bitstreams.
     * Bitstreams must be on image mime type. (@abollini will update.)
     *
     * @param context the DSpace context
     * @param item the DSpace Item
     * @param bnd a DSpace bundle
     * @param bitstream a DSpace bitstream
     */
    public CanvasGenerator addCanvas(Context context, Item item, Bundle bnd, Bitstream bitstream) {
        String mimeType = utils.getBitstreamMimeType(bitstream, context);
        String manifestId = item.getID().toString();
        CanvasGenerator canvasGenerator =
                canvasService.getCanvas(context, manifestId, bitstream, bnd, item, counter, mimeType);
        sequenceGenerator.addCanvas(canvasGenerator);
        counter++;
        return canvasGenerator;
    }

    /**
     * This method looks for a PDF rendering in the Item's ORIGINAL bundle and adds
     * it to the Sequence if found.
     *
     * @param item DSpace Item
     * @param context DSpace context
     */
    private void addRendering(Item item, Context context) {
        List<Bundle> bundles = utils.getIIIFBundles(item);
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bitstream : bitstreams) {
                String mimeType = null;
                try {
                    mimeType = bitstream.getFormat(context).getMIMEType();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // If the  bundle contains a PDF, assume that it represents the
                // item and add to rendering. Ignore other mime-types. Other options
                // might be using the primary bitstream or relying on a bitstream metadata
                // field, e.g. iiif.rendering
                if (mimeType != null && mimeType.contentEquals("application/pdf")) {
                    String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                    sequenceGenerator.addRendering(
                            new ExternalLinksGenerator(id)
                                .setLabel(utils.getIIIFLabel(bitstream, bitstream.getName()))
                                .setFormat(mimeType)
                    );
                }
            }
        }
    }

}
