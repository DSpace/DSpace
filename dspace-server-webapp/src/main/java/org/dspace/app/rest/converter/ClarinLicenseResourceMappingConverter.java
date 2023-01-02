/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinLicenseResourceMappingRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.springframework.stereotype.Component;

@Component
public class ClarinLicenseResourceMappingConverter implements
        DSpaceConverter<ClarinLicenseResourceMapping, ClarinLicenseResourceMappingRest> {
    @Override
    public ClarinLicenseResourceMappingRest convert(ClarinLicenseResourceMapping modelObject, Projection projection) {
        ClarinLicenseResourceMappingRest clarinLicenseResourceMappingRest = new ClarinLicenseResourceMappingRest();
        clarinLicenseResourceMappingRest.setProjection(projection);
        clarinLicenseResourceMappingRest.setId(modelObject.getID());
        clarinLicenseResourceMappingRest.setBitstreamID(modelObject.getBitstream().getID());
        return clarinLicenseResourceMappingRest;
    }

    @Override
    public Class<ClarinLicenseResourceMapping> getModelClass() {
        return ClarinLicenseResourceMapping.class;
    }
}
