/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import org.dspace.app.iiif.model.generator.AnnotationListGenerator;
import org.dspace.content.Bitstream;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service is used to create an image annotation lists. Annotation Lists are separate resources
 * that should be dereferenced when encountered. They are collections of annotations, where each
 * annotation targets the Canvas or part thereof. The annotation list must have an http(s) URI given
 * in @id, and the JSON representation must be returned when that URI is dereferenced.
 *
 * @author Michael Spalti  (mspalti at willamette.edu)
 */
@RequestScope
@Component
public class ImageAnnotationService extends AbstractResourceService {

    public ImageAnnotationService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Returns the annotation list generator for the provided bitstream.
     * @param bitstream DSpace bitstream
     * @return the list generator
     */
    public AnnotationListGenerator getImageAnnotations(Bitstream bitstream) {
        AnnotationListGenerator list =  new AnnotationListGenerator();
        list.setIdentifier(IIIF_ENDPOINT + bitstream.getID() + "/annotation/list");
        return list;
    }

}
