/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinFeaturedServiceLinkRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinFeaturedServiceLink;
import org.springframework.stereotype.Component;

/**
 * Convert the FeaturedService objects to the REST objects.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class ClarinFeaturedServiceLinkConverter implements DSpaceConverter<ClarinFeaturedServiceLink,
        ClarinFeaturedServiceLinkRest> {

    @Override
    public ClarinFeaturedServiceLinkRest convert(ClarinFeaturedServiceLink modelObject, Projection projection) {
        ClarinFeaturedServiceLinkRest clarinFeaturedServiceLinkRest = new ClarinFeaturedServiceLinkRest();
        clarinFeaturedServiceLinkRest.setProjection(projection);
        clarinFeaturedServiceLinkRest.setKey(modelObject.getKey());
        clarinFeaturedServiceLinkRest.setValue(modelObject.getValue());
        return clarinFeaturedServiceLinkRest;
    }

    @Override
    public Class<ClarinFeaturedServiceLink> getModelClass() {
        return ClarinFeaturedServiceLink.class;
    }
}
