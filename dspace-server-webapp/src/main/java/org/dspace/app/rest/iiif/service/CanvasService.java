/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CanvasService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CanvasService.class);

    @Autowired
    ImageContentService imageContentService;

    @Autowired
    IIIFUtils utils;

    /**
     * Constructor.
     * 
     * @param configurationService the DSpace configuration service.
     */
    public CanvasService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Creates a single Canvas object. If canvas parameters are provided by the Info
     * object they are used. If canvas parameters are unavailable, default values
     * are used instead.
     *
     * Note that info.json is going to be replaced with metadata in the bitstream
     * DSO.
     *
     * @param manifestId  manifest id
     * @param bitstreamId uuid of the bitstream
     * @param mimeType    the mimetype of the bitstream
     * @param info        parameters for this canvas
     * @param count       the canvas position in the sequence.
     * @return canvas object
     */
    protected CanvasGenerator getCanvas(String manifestId, Bitstream bitstream, Bundle bundle, Item item, int count,
            String mimeType) {
        int pagePosition = count + 1;

        String canvasNaming = utils.getCanvasNaming(item, "Page");
        String label = utils.getIIIFLabel(bitstream, canvasNaming + " " + pagePosition);
        int canvasWidth = utils.getCanvasWidth(bitstream, bundle, item, DEFAULT_CANVAS_WIDTH);
        int canvasHeight = utils.getCanvasHeight(bitstream, bundle, item, DEFAULT_CANVAS_HEIGHT);
        UUID bitstreamId = bitstream.getID();

        ImageContentGenerator image = imageContentService.getImageContent(bitstreamId, mimeType,
                imageUtil.getImageProfile(), IMAGE_PATH);

        ImageContentGenerator thumb = imageContentService.getImageContent(bitstreamId, mimeType,
                thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);

        return new CanvasGenerator().setIdentifier(IIIF_ENDPOINT + manifestId + "/canvas/c" + count)
                .addImage(image.getResource()).addThumbnail(thumb.getResource()).setHeight(canvasHeight)
                .setWidth(canvasWidth).setLabel(label);
    }

    /**
     * Ranges expect the Canvas object to have only an identifier.
     * 
     * @param identifier  the DSpace item identifier
     * @param startCanvas the position of the canvas in list
     * @return
     */
    protected CanvasGenerator getRangeCanvasReference(String identifier, String startCanvas) {
        return new CanvasGenerator().setIdentifier(IIIF_ENDPOINT + identifier + startCanvas);
    }

}
