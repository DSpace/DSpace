/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.UUID;

import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.app.rest.iiif.service.util.ImageProfileUtil;
import org.dspace.app.rest.iiif.service.util.ThumbProfileUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for IIIF responses.
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
    protected static final Integer DEFAULT_CANVAS_WIDTH_FALLBACK = 1200;
    protected static final Integer DEFAULT_CANVAS_HEIGHT_FALLBACK = 1600;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ThumbProfileUtil thumbUtil;

    @Autowired
    ImageProfileUtil imageUtil;


    /**
     * Set constants using DSpace configuration definitions.
     * @param configurationService the DSpace configuration service
     */
    protected void setConfiguration(ConfigurationService configurationService) {
        IIIF_ENDPOINT = configurationService.getProperty("iiif.url");
        IMAGE_SERVICE = configurationService.getProperty("iiif.image.server");
        SEARCH_URL = configurationService.getProperty("iiif.solr.search.url");
        BITSTREAM_PATH_PREFIX = configurationService.getProperty("iiif.bitstream.url");
        DOCUMENT_VIEWING_HINT = configurationService.getProperty("iiif.document.viewing.hint");
        CLIENT_URL = configurationService.getProperty("dspace.ui.url");
        IIIF_LOGO_IMAGE = configurationService.getProperty("iiif.logo.image");
        DEFAULT_CANVAS_WIDTH = configurationService.getIntProperty("iiif.canvas.default-width",
                DEFAULT_CANVAS_WIDTH_FALLBACK);
        DEFAULT_CANVAS_HEIGHT = configurationService.getIntProperty("iiif.canvas.default-heigth",
                DEFAULT_CANVAS_HEIGHT_FALLBACK);
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
