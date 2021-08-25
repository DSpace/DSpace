/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import java.util.ArrayList;
import java.util.List;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.Service;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade for the domain model's ImageContent.
 *
 * Presentation API version 2.1.1: The ImageContent entity is contained in the "resource"
 * field of annotations with motivation "sc:painting". Image resources, and only image resources,
 * are included in the images property of the canvas. This changes in API version 3.0.
 */
@Component
@RequestScope
public class ImageContentGenerator implements IIIFResource  {

    private String identifier;
    private String mimetype;
    private org.dspace.app.rest.iiif.model.generator.ImageServiceGenerator imageService;

    /**
     * Sets the mandatory identifier for image content.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the optional mimetype.
     * @param mimetype
     */
    public void setFormat(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Sets the image service that the client will use to retrieve images.
     * @param imageService
     */
    public void addService(org.dspace.app.rest.iiif.model.generator.ImageServiceGenerator imageService) {
        this.imageService = imageService;
    }

    @Override
    public Resource<ImageContent> getResource() {
        if (identifier == null) {
            throw new RuntimeException("The ImageContent resource requires an identifier.");
        }
        ImageContent imageContent = new ImageContent(identifier);
        if (mimetype != null) {
            imageContent.setFormat(mimetype);
        }
        // Supporting a single service for each image resource.
        List<Service> services = new ArrayList<>();
        if (imageService != null) {
            services.add(imageService.getService());
        }
        imageContent.setServices(services);

        return imageContent;
    }
}
