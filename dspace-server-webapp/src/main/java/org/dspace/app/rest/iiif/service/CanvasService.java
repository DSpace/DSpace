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
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating {@code Canvases}. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 */
@RequestScope
@Component
public class CanvasService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CanvasService.class);

    // Default canvas dimensions.
    protected static final Integer DEFAULT_CANVAS_WIDTH = 1200;
    protected static final Integer DEFAULT_CANVAS_HEIGHT = 1600;

    @Autowired
    ImageContentService imageContentService;

    /**
     * Constructor.
     * @param configurationService the DSpace configuration service.
     */
    public CanvasService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Creates a single Canvas object. If canvas parameters are provided by the
     * Info object they are used. If canvas parameters are unavailable, default values
     * are used instead.
     *
     * Note that info.json is going to be replaced with metadata in the bitstream DSO.
     *
     * @param manifestId manifest id
     * @param bitstreamId uuid of the bitstream
     * @param mimeType the mimetype of the bitstream
     * @param info parameters for this canvas
     * @param count the canvas position in the sequence.
     * @return canvas object
     */
    protected CanvasGenerator getCanvas(String manifestId, UUID bitstreamId, String mimeType, Info info, int count) {
        int pagePosition = count + 1;

        // Defaults settings. Used if no info.json is provided.
        String label = "Page " + pagePosition;
        int canvasWidth = DEFAULT_CANVAS_WIDTH;
        int canvasHeight = DEFAULT_CANVAS_HEIGHT;

        // Override with settings from info.json, if available.
        if (info != null && info.getGlobalDefaults() != null && info.getCanvases() != null) {
            // The info.json file can request global defaults for canvas
            // height, width and labels. Use global settings if activated in info.json.
            if (info.getGlobalDefaults().isActivated()) {
                // Create unique label by appending position to the default label.
                label = info.getGlobalDefaults().getLabel() + " " + pagePosition;
                canvasWidth = info.getGlobalDefaults().getWidth();
                canvasHeight = info.getGlobalDefaults().getHeight();
            } else if (info.getCanvases().get(count) != null) {
                if (info.getCanvases().get(count).getLabel().length() > 0) {
                    // Labels assumed unique and are not incremented
                    // when info.json provides individual canvas metadata.
                    label = info.getCanvases().get(count).getLabel();
                }
                canvasWidth = info.getCanvases().get(count).getWidth();
                canvasHeight = info.getCanvases().get(count).getHeight();
            }
        } else {
            log.info("Correctly formatted info.json was not found for item.  Using application defaults.");
        }

        ImageContentGenerator image = imageContentService
                .getImageContent(bitstreamId, mimeType, imageUtil.getImageProfile(), IMAGE_PATH);

        ImageContentGenerator thumb = imageContentService
                .getImageContent(bitstreamId, mimeType, thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);

        return new CanvasGenerator(IIIF_ENDPOINT + manifestId + "/canvas/c" + count)
                .addImage(image.generate())
                .addThumbnail(thumb.generate())
                .setHeight(canvasHeight)
                .setWidth(canvasWidth)
                .setLabel(label);
    }


    /**
     * Ranges expect the Canvas object to have only an identifier.
     * @param identifier the DSpace item identifier
     * @param startCanvas the position of the canvas in list
     * @return
     */
    protected CanvasGenerator getRangeCanvasReference(String identifier, String startCanvas) {
        return new CanvasGenerator(IIIF_ENDPOINT + identifier + startCanvas);
    }

}
