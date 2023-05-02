/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.ocrcanvas.factory;

import org.dspace.iiif.ocrcanvas.service.AnnotationLinkService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract  class AnnotationLinkServiceFactory {

    public static AnnotationLinkServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("annotationLinkServiceFactory",
                                        AnnotationLinkServiceFactory.class);
    }

    public abstract AnnotationLinkService getAnnotationLinkService();
}
