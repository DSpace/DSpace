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
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.CanvasItemsGenerator;
import org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SequenceService  extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SequenceService.class);

    // TODO i18n
    private static final String PDF_DOWNLOAD_LABEL = "Download as PDF";

    @Autowired
    CanvasItemsGenerator sequenceGenerator;

    @Autowired
    ExternalLinksGenerator externalLinksGenerator;

    @Autowired
    CanvasService canvasService;

    ApplicationContext applicationContext;

    public SequenceService(ApplicationContext applicationContext, ConfigurationService configurationService) {
        setConfiguration(configurationService);
        this.applicationContext = applicationContext;
    }

    public CanvasItemsGenerator getSequence(Item item, List<Bitstream> bitstreams, Context context, Info info) {

        sequenceGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/sequence/s0");
        if (bitstreams.size() > 0) {
            addCanvases(context, item, bitstreams, info);
        }
        addRendering(item, context);

        return sequenceGenerator;
    }

    /**
     * This method adds a canvas to the sequence for each item in the list of DSpace bitstreams.
     * Bitstreams must be on image mime type.
     *
     * @param context the DSpace context
     * @param item the DSpace Item
     * @param bitstreams list of DSpace bitstreams
     */
    private void addCanvases(Context context, Item item,
                             List<Bitstream> bitstreams, Info info) {
        /*
         * The counter tracks the position of the bitstream in the list and is used to create the canvas identifier.
         * The order of bitstreams (and thus page order in documents) is determined by position in the DSpace
         * bundle.
         */
        int counter = 0;
        if (bitstreams == null || bitstreams.size() == 0) {
            throw new RuntimeException("No bitstreams found for " + item.getID()  +
                    ". Cannot add media content to the manifest.");
        }
        for (Bitstream bitstream : bitstreams) {
            UUID bitstreamId = bitstream.getID();
            String mimeType = utils.getBitstreamMimeType(bitstream, context);
            if (utils.checkImageMimeType(mimeType)) {
                String manifestId = item.getID().toString();
                CanvasGenerator canvasGenerator =
                        canvasService.getCanvas(manifestId, bitstreamId, mimeType, info, counter);
                sequenceGenerator.addCanvas(canvasGenerator);
                counter++;
            }
        }
    }

    /**
     * A link to an external resource intended for display or download by a human user.
     * This property can be used to link from a manifest, collection or other resource
     * to the preferred viewing environment for that resource, such as a viewer page on
     * the publisher’s web site. Other uses include a rendering of a manifest as a PDF
     * or EPUB.
     *
     * This method looks for a PDF rendering in the Item's ORIGINAL bundle and adds
     * it to the Sequence if found.
     *
     * @param item DSpace Item
     * @param context DSpace context
     */
    private void addRendering(Item item, Context context) {
        List<Bundle> bundles = item.getBundles("ORIGINAL");
        if (bundles.size() == 0) {
            return;
        }
        Bundle bundle = bundles.get(0);
        List<Bitstream> bitstreams = bundle.getBitstreams();
        for (Bitstream bitstream : bitstreams) {
            String mimeType = null;
            try {
                mimeType = bitstream.getFormat(context).getMIMEType();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // If the ORIGINAL bundle contains a PDF, assume that it represents the
            // item and add to rendering. Ignore other mime-types. Other options
            // might be using the primary bitstream or relying on a bitstream metadata
            // field, e.g. iiif.rendering
            if (mimeType != null && mimeType.contentEquals("application/pdf")) {
                String id = BITSTREAM_PATH_PREFIX + "/" + bitstream.getID() + "/content";
                sequenceGenerator.addRendering(
                        externalLinksGenerator
                                .setIdentifier(id)
                                .setLabel(PDF_DOWNLOAD_LABEL)
                                .setFormat(mimeType)
                );
            }
        }
    }
}
