package org.dspace.iiif.annotationlink.factory;

import org.dspace.iiif.annotationlink.service.AnnotationLinkService;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotationLinkServiceFactoryImpl extends AnnotationLinkServiceFactory {

    @Autowired
    AnnotationLinkService annotationLinkService;

    @Override
    public AnnotationLinkService getAnnotationLinkService() {
        System.out.println(annotationLinkService);
        return annotationLinkService;
    }
}
