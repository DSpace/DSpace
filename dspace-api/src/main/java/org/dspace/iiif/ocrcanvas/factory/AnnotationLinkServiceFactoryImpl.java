/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.ocrcanvas.factory;

import org.dspace.iiif.ocrcanvas.service.AnnotationLinkService;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotationLinkServiceFactoryImpl extends AnnotationLinkServiceFactory {

    @Autowired
    AnnotationLinkService annotationLinkService;

    @Override
    public AnnotationLinkService getAnnotationLinkService() {
        return annotationLinkService;
    }
}
