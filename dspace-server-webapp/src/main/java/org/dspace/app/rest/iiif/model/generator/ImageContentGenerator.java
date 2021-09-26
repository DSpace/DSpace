/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import javax.validation.constraints.NotNull;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;

/**
 * This service generator wraps the image content model.
 *
 * Presentation API version 2.1.1: The ImageContent entity is contained in the "resource"
 * field of annotations with motivation "sc:painting". Image resources, and only image resources,
 * are included in the image's property of the canvas. This changes in API version 3.0.
 */
public class ImageContentGenerator implements IIIFResource  {

    private org.dspace.app.rest.iiif.model.generator.ImageServiceGenerator imageService;
    private final ImageContent imageContent;

    public ImageContentGenerator(@NotNull String identifier) {
        imageContent = new ImageContent(identifier);
    }

    /**
     * Sets the optional mimetype.
     * @param mimetype
     */
    public ImageContentGenerator setFormat(String mimetype) {
        imageContent.setFormat(mimetype);
        return this;
    }

    /**
     * Adds the IIIF image service annotation.
     * @param imageService
     */
    public ImageContentGenerator addService(ImageServiceGenerator imageService) {
        this.imageContent.addService(imageService.generate());
        return this;
    }

    @Override
    public Resource<ImageContent> generate() {
        return imageContent;
    }
}
