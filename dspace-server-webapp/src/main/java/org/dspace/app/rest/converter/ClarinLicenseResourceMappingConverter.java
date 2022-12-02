package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinLicenseResourceMappingRest;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinUserMetadata;
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
