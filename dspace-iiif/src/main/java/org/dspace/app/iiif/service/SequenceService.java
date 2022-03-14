/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.CanvasItemsGenerator;
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
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
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
     * Returns a sequence generator that has been configured with canvases. (@abollini will update.)
     *
     * @param item the DSpace item
     * @return a sequence generator
     */
    public CanvasItemsGenerator getSequence(Item item) {

        sequenceGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/sequence/s0");
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


}
