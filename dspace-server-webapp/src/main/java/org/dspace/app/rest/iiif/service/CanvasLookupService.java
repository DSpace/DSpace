/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.ArrayList;
import java.util.UUID;

import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Canvases may be dereferenced separately from the manifest via their URIs.
 */
@Component
public class CanvasLookupService extends AbstractResourceService {

    @Autowired
    IIIFUtils utils;

    @Autowired
    CanvasService canvasService;

    public CanvasLookupService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    public String generateCanvas(Context context, Item item, String canvasId) {
        int canvasPosition = utils.getCanvasId(canvasId);
        Bitstream bitstream = utils.getBitstreamForCanvas(item, IIIF_BUNDLE, canvasPosition);
        if (bitstream == null) {
            throw new ResourceNotFoundException();
        }
        Info info =
                utils.validateInfoForSingleCanvas(utils.getInfo(context, item, IIIF_BUNDLE), canvasPosition);
        ArrayList<Bitstream> bitstreams = new ArrayList<>();
        bitstreams.add(bitstream);
        UUID bitstreamID = bitstream.getID();
        String mimeType = utils.getBitstreamMimeType(bitstream, context);
        CanvasGenerator canvas = canvasService.getCanvas(item.getID().toString(), info, canvasPosition);
        if (mimeType.contains("image/")) {
            addImage(canvas, mimeType, bitstreamID);
        }
        return utils.asJson(canvas.getResource());
    }

}
