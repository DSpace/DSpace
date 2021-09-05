/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.UUID;

import org.dspace.app.rest.iiif.model.generator.ImageContentGenerator;
import org.dspace.app.rest.iiif.model.generator.ImageServiceGenerator;
import org.dspace.app.rest.iiif.model.generator.ProfileGenerator;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ImageContentService extends AbstractResourceService {


    public ImageContentService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
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
    protected ImageContentGenerator getImageContent(UUID uuid, String mimetype, ProfileGenerator profile, String path) {
        return new ImageContentGenerator(IMAGE_SERVICE + uuid + path)
                .setFormat(mimetype)
                .addService(getImageService(profile, uuid.toString()));
    }

    protected ImageContentGenerator getImageContent(String identifier) {
        return new ImageContentGenerator(identifier);
    }

    /**
     * A link to a service that makes more functionality available for the resource,
     * like the Image API service.
     *
     * @param profile service profile
     * @param uuid id of the image bitstream
     * @return object representing the Image Service
     */
    private ImageServiceGenerator getImageService(ProfileGenerator profile, String uuid) {
        return new ImageServiceGenerator(IMAGE_SERVICE + uuid).setProfile(profile);
    }

}
