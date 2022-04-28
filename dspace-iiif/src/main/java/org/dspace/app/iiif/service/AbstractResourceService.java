/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.util.UUID;

import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.app.iiif.service.utils.ImageProfileUtil;
import org.dspace.app.iiif.service.utils.ThumbProfileUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for services.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class AbstractResourceService {
    /**
     * These values are defined in dspace configuration.
     */
    protected String IIIF_ENDPOINT;
    protected String IMAGE_SERVICE;
    protected String SEARCH_URL;
    protected String CLIENT_URL;
    protected String IIIF_LOGO_IMAGE;
    protected String BITSTREAM_PATH_PREFIX;
    protected int DEFAULT_CANVAS_WIDTH;
    protected int DEFAULT_CANVAS_HEIGHT;
    /**
     * Possible values: "paged" or "individuals". The property
     * value is set in dspace configuration.
     */
    protected static String DOCUMENT_VIEWING_HINT;

    // Paths for IIIF Image API requests.
    protected static final String THUMBNAIL_PATH = "/full/90,/0/default.jpg";
    protected static final String IMAGE_PATH =     "/full/full/0/default.jpg";
    // Default canvas dimensions.
    protected static Integer defaultCanvasWidthFallback = 2200;
    protected static Integer defaultCanvasHeightFallback = 1600;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ThumbProfileUtil thumbUtil;

    @Autowired
    ImageProfileUtil imageUtil;

    ConfigurationService configurationService;


    /**
     * Set constants using DSpace configuration definitions.
     * @param configurationService the DSpace configuration service
     */
    protected void setConfiguration(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        IIIF_ENDPOINT = configurationService.getProperty("dspace.server.url") + "/iiif/";
        IMAGE_SERVICE = configurationService.getProperty("iiif.image.server");
        SEARCH_URL = configurationService.getProperty("iiif.search.url");
        BITSTREAM_PATH_PREFIX = configurationService.getProperty("dspace.server.url") + "/api/core/bitstreams";
        DOCUMENT_VIEWING_HINT = configurationService.getProperty("iiif.document.viewing.hint");
        CLIENT_URL = configurationService.getProperty("dspace.ui.url");
        IIIF_LOGO_IMAGE = configurationService.getProperty("iiif.logo.image");
    }

    protected void setDefaultCanvasDimensions() {
        DEFAULT_CANVAS_WIDTH = this.configurationService.getIntProperty("iiif.canvas.default-width",
            defaultCanvasWidthFallback);
        DEFAULT_CANVAS_HEIGHT = this.configurationService.getIntProperty("iiif.canvas.default-height",
            defaultCanvasHeightFallback);
    }

    /**
     * Creates the manifest id from the provided uuid.
     * @param uuid the item id
     * @return the manifest identifier (url)
     */
    protected String getManifestId(UUID uuid) {
        return IIIF_ENDPOINT + uuid + "/manifest";
    }


}
