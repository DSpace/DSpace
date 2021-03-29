/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.rest.iiif.model.generator.ImageServiceGenerator;
import org.dspace.app.rest.iiif.model.generator.ProfileGenerator;
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
    protected String BITSTREAM_PATH_PREFIX;
    /**
     * Possible values: "paged" or "individuals".  Typically paged is preferred
     * for documents. However, it can be overridden in configuration if necessary
     * for the viewer client.
     */
    protected static String DOCUMENT_VIEWING_HINT;

    // TODO: should these bundle settings be added to dspace configuration or hard-coded here?
    // The DSpace bundle used for IIIF entity types.
    protected static final String IIIF_BUNDLE = "IIIF";
    // The DSpace bundle for other content related to item.
    protected static final String OTHER_CONTENT_BUNDLE = "OtherContent";

    // Paths for IIIF Image API requests.
    protected static final String THUMBNAIL_PATH = "/full/90,/0/default.jpg";
    protected static final String IMAGE_PATH =     "/full/full/0/default.jpg";

    @Autowired
    IIIFUtils utils;

    @Autowired
    ThumbProfileUtil thumbUtil;

    @Autowired
    ImageProfileUtil imageUtil;

    @Autowired
    ImageContentGenerator imageContent;

    @Autowired
    ImageServiceGenerator imageService;

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
    }

    /**
     * Creates the manifest id from the provided uuid.
     * @param uuid the item id
     * @return the manifest identifier (url)
     */
    protected String getManifestId(UUID uuid) {
        return IIIF_ENDPOINT + uuid + "/manifest";
    }

    /**
     * Association of images with their respective canvases is done via annotations.
     * Only the annotations that associate images or parts of images are included in
     * the canvas in the images property. If a IIIF Image API service is available for
     * the image, then a link to the service’s base URI should be included.
     *
     * This method adds an image annotations to a canvas for both thumbnail and full size
     * images. The annotation references the IIIF image service.
     *
     * @param canvas the Canvas object.
     * @param mimeType the image mime type
     * @param bitstreamID the bitstream uuid
     */
    protected void addImage(CanvasGenerator canvas, String mimeType, UUID bitstreamID) throws
            RuntimeException {
        canvas.addThumbnail(getThumbnailAnnotation(bitstreamID, mimeType));
        // Add image content resource to canvas facade.
        canvas.addImage(getImageContent(bitstreamID, mimeType, imageUtil.getImageProfile(), IMAGE_PATH).getResource());
    }

    /**
     * A small image that depicts or pictorially represents the resource that
     * the property is attached to, such as the title page, a significant image
     * or rendering of a canvas with multiple content resources associated with it.
     * It is recommended that a IIIF Image API service be available for this image for
     * manipulations such as resizing.
     *
     * This method returns a thumbnail annotation that includes the IIIF image service.
     *
     * @param uuid the bitstream id
     * @return thumbnail Annotation
     */
    protected ImageContentGenerator getThumbnailAnnotation(UUID uuid, String mimetype) throws
            RuntimeException {
        return getImageContent(uuid, mimetype, thumbUtil.getThumbnailProfile(), THUMBNAIL_PATH);
    }

    /**
     * Association of images with their respective canvases is done via annotations. The Open Annotation model
     * allows any resource to be associated with any other resource, or parts thereof, and it is reused for
     * both commentary and painting resources on the canvas.
     * @param uuid bitstream uuid
     * @param mimetype bitstream mimetype
     * @param profile the service profile
     * @param path the path component of the identifier
     * @return
     */
    private ImageContentGenerator getImageContent(UUID uuid, String mimetype, ProfileGenerator profile, String path) {
        imageContent.setFormat(mimetype);
        imageContent.setIdentifier(IMAGE_SERVICE + uuid + path);
        imageContent.addService(getImageService(profile, uuid.toString()));
        return imageContent;
    }

    /**
     * A link to a service that makes more functionality available for the resource,
     * such as from an image to the base URI of an associated IIIF Image API service.
     *
     * @param profile service profile
     * @param uuid id of the image bitstream
     * @return object representing the Image Service
     */
    private ImageServiceGenerator getImageService(ProfileGenerator profile, String uuid) {
        imageService.setIdentifier(IMAGE_SERVICE + uuid);
        imageService.setProfile(profile);
        return imageService;
    }

}
