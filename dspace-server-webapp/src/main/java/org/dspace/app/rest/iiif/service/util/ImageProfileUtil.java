/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service.util;

import org.dspace.app.rest.iiif.model.generator.ProfileGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageProfileUtil {

    @Autowired
    ProfileGenerator profile;

    /**
     * Utility method for obtaining the image service profile.
     * Calling from this utility provides a unique instance of the
     * autowired property. Necessary because a single canvas resource contains
     * both thumbnail and images.
     *
     * @return  image service profile
     */
    public ProfileGenerator getImageProfile() throws
            RuntimeException {
        profile.setIdentifier("http://iiif.io/api/image/2/level1.json");
        return profile;
    }
}
