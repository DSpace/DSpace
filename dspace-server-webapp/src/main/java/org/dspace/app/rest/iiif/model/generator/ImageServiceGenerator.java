/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import java.util.ArrayList;

import de.digitalcollections.iiif.model.Profile;
import de.digitalcollections.iiif.model.Service;
import de.digitalcollections.iiif.model.image.ImageService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade or Presentation API version 2.1.1 image service property that is added to
 * each image resource.
 */
@Component
@RequestScope
public class ImageServiceGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFService {

    private String identifier;
    private Profile profile;

    /**
     * Sets the mandatory identifier for the image service.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the IIIF image profile.
     * @param profile
     */
    public void setProfile(org.dspace.app.rest.iiif.model.generator.ProfileGenerator profile) {
        this.profile = profile.getValue();

    }

    @Override
    public Service getService() {
        ImageService imageService = new ImageService(identifier);
        if (profile != null) {
            ArrayList<Profile> profiles = new ArrayList<>();
            profiles.add(profile);
            imageService.setProfiles(profiles);
        }
        return imageService;
    }
}
