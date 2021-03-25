/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import org.apache.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CanvasService extends AbstractResourceService {

    private static final Logger log = Logger.getLogger(CanvasService.class);

    // Default canvas dimensions.
    protected static final Integer DEFAULT_CANVAS_WIDTH = 1200;
    protected static final Integer DEFAULT_CANVAS_HEIGHT = 1600;

    @Autowired
    CanvasGenerator canvas;

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
     * @param id manifest id
     * @param info parameters for this canvas
     * @param count the canvas position in the sequence.
     * @return canvas object
     */
    protected CanvasGenerator getCanvas(String id, Info info, int count) {
        // Defaults settings.
        int canvasWidth = DEFAULT_CANVAS_WIDTH;
        int canvasHeight = DEFAULT_CANVAS_HEIGHT;
        int pagePosition = count + 1;
        String label = "Page " + pagePosition;
        // Override with settings from info.json, if available.
        if (info != null && info.getGlobalDefaults() != null && info.getCanvases() != null) {
            // Use global settings if activated.
            if (info.getGlobalDefaults().isActivated()) {
                // Create unique label by appending position to the default label.
                label = info.getGlobalDefaults().getLabel() + " " + pagePosition;
                canvasWidth = info.getGlobalDefaults().getWidth();
                canvasHeight = info.getGlobalDefaults().getHeight();
            } else if (info.getCanvases().get(count) != null) {
                if (info.getCanvases().get(count).getLabel().length() > 0) {
                    // Individually defined canvas labels assumed unique, and are not incremented.
                    label = info.getCanvases().get(count).getLabel();
                }
                canvasWidth = info.getCanvases().get(count).getWidth();
                canvasHeight = info.getCanvases().get(count).getHeight();
            }
        } else {
            log.info("Correctly formatted info.json was not found for item.  Using application defaults.");
        }
        canvas.setIdentifier(IIIF_ENDPOINT + id + "/canvas/c" + count);
        canvas.setLabel(label);
        canvas.setHeight(canvasHeight);
        canvas.setWidth(canvasWidth);
        return canvas;
    }

    /**
     * Ranges expect the Canvas object to have only an identifier. This method assures that the
     * injected canvas facade is empty before setting the identifier.
     * @param identifier the DSpace item identifier
     * @param startCanvas the position of the canvas in list
     * @return
     */
    protected CanvasGenerator getRangeCanvasReference(String identifier, String startCanvas) {
        canvas.setIdentifier(IIIF_ENDPOINT + identifier + startCanvas);
        return canvas;
    }

}
