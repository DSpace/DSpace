package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinLicenseResourceUserAllowanceRest;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.springframework.stereotype.Component;

@Component
public class ClarinLicenseResourceUserAllowanceConverter implements
        DSpaceConverter<ClarinLicenseResourceUserAllowance, ClarinLicenseResourceUserAllowanceRest> {
    @Override
    public ClarinLicenseResourceUserAllowanceRest convert(ClarinLicenseResourceUserAllowance modelObject, Projection projection) {
        ClarinLicenseResourceUserAllowanceRest clarinLicenseResourceUserAllowanceRest =
                new ClarinLicenseResourceUserAllowanceRest();
        clarinLicenseResourceUserAllowanceRest.setProjection(projection);
        clarinLicenseResourceUserAllowanceRest.setId(modelObject.getID());
        clarinLicenseResourceUserAllowanceRest.setToken(modelObject.getToken());
        return clarinLicenseResourceUserAllowanceRest;
    }

    @Override
    public Class<ClarinLicenseResourceUserAllowance> getModelClass() {
        return ClarinLicenseResourceUserAllowance.class;
    }
}
