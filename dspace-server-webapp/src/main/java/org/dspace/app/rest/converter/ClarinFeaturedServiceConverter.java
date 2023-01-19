/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.dspace.app.rest.model.ClarinFeaturedServiceLinkRest;
import org.dspace.app.rest.model.ClarinFeaturedServiceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinFeaturedService;
import org.dspace.content.clarin.ClarinFeaturedServiceLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Convert the FeaturedService objects to the REST objects.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class ClarinFeaturedServiceConverter implements DSpaceConverter<ClarinFeaturedService,
        ClarinFeaturedServiceRest> {

    @Autowired
    private ConverterService converter;

    @Override
    public ClarinFeaturedServiceRest convert(ClarinFeaturedService modelObject, Projection projection) {
        ClarinFeaturedServiceRest clarinFeaturedServiceRest = new ClarinFeaturedServiceRest();
        clarinFeaturedServiceRest.setProjection(projection);
        clarinFeaturedServiceRest.setName(modelObject.getName());
        clarinFeaturedServiceRest.setUrl(modelObject.getUrl());
        clarinFeaturedServiceRest.setDescription(modelObject.getDescription());
        setFeaturedServicesLinks(clarinFeaturedServiceRest, modelObject.getFeaturedServiceLinks(), projection);
        return clarinFeaturedServiceRest;
    }

    public void setFeaturedServicesLinks(ClarinFeaturedServiceRest clarinFeaturedServiceRest,
                                         List<ClarinFeaturedServiceLink> clarinFeaturedServiceLinks,
                                         Projection projection) {
        DSpaceConverter<ClarinFeaturedServiceLink, ClarinFeaturedServiceLinkRest> featuredServiceLinkConverter =
                converter.getConverter(ClarinFeaturedServiceLink.class);

        for (ClarinFeaturedServiceLink clarinFeaturedServiceLink : clarinFeaturedServiceLinks) {
            clarinFeaturedServiceRest.getFeaturedServiceLinks().add(
                featuredServiceLinkConverter.convert(clarinFeaturedServiceLink, projection)
            );
        }
    }

    @Override
    public Class<ClarinFeaturedService> getModelClass() {
        return ClarinFeaturedService.class;
    }
}
