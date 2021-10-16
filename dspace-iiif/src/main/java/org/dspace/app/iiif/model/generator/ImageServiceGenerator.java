/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import de.digitalcollections.iiif.model.Service;
import de.digitalcollections.iiif.model.image.ImageService;

/**
 * This service generator wraps the image service property model. An image service
 * annotation is added to each canvas annotation.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class ImageServiceGenerator implements IIIFService {

    private ImageService imageService;

    public ImageServiceGenerator(String identifier) {
        imageService = new ImageService(identifier);
    }

    /**
     * Sets the IIIF image profile.
     * @param profile a profile generator
     */
    public ImageServiceGenerator setProfile(ProfileGenerator profile) {
        imageService.addProfile(profile.generateValue());
        return this;
    }

    @Override
    public Service generateService() {
        return imageService;
    }
}
