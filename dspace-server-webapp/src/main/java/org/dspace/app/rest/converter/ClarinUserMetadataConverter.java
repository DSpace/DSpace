package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.springframework.stereotype.Component;

@Component
public class ClarinUserMetadataConverter implements DSpaceConverter<ClarinUserMetadata, ClarinUserMetadataRest> {
    @Override
    public ClarinUserMetadataRest convert(ClarinUserMetadata modelObject, Projection projection) {
        ClarinUserMetadataRest clarinUserMetadataRest = new ClarinUserMetadataRest();
        clarinUserMetadataRest.setId(modelObject.getID());
        clarinUserMetadataRest.setProjection(projection);
        clarinUserMetadataRest.setMetadataKey(modelObject.getMetadataKey());
        clarinUserMetadataRest.setMetadataValue(modelObject.getMetadataValue());
        return clarinUserMetadataRest;
    }

    @Override
    public Class<ClarinUserMetadata> getModelClass() {
        return ClarinUserMetadata.class;
    }
}
