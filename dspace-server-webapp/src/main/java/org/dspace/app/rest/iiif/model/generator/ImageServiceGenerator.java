/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.Service;
import de.digitalcollections.iiif.model.image.ImageService;

/**
 * POJO facade for API version 2.1.1 image service property. Added to
 * each image resource.
 */
public class ImageServiceGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFService {

    private ImageService imageService;

    public ImageServiceGenerator(String identifier) {
        imageService = new ImageService(identifier);
    }

    /**
     * Sets the IIIF image profile.
     * @param profile
     */
    public ImageServiceGenerator setProfile(org.dspace.app.rest.iiif.model.generator.ProfileGenerator profile) {
        imageService.addProfile(profile.getValue());
        return this;
    }

    @Override
    public Service getService() {
        return imageService;
    }
}
