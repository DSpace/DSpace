/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service.utils;

import org.dspace.app.iiif.model.generator.ProfileGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageProfileUtil {

    @Autowired
    ProfileGenerator profile;

    /**
     * Utility method for obtaining the image service profile.
     *
     * @return  image service profile
     */
    public ProfileGenerator getImageProfile() throws
            RuntimeException {
        profile.setIdentifier("http://iiif.io/api/image/2/level1.json");
        return profile;
    }
}
