package org.dspace.iiif.annotationlink.factory;

import org.dspace.iiif.annotationlink.service.AnnotationLinkService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract  class AnnotationLinkServiceFactory {

    public static AnnotationLinkServiceFactory getInstance() {
        System.out.println(DSpaceServicesFactory.getInstance().getServiceManager());
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("annotationLinkServiceFactory",
                                        AnnotationLinkServiceFactory.class);
    }

    public abstract AnnotationLinkService getAnnotationLinkService();
}
