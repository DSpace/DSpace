/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinLicenseResourceUserAllowanceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.springframework.stereotype.Component;

@Component
public class ClarinLicenseResourceUserAllowanceConverter implements
        DSpaceConverter<ClarinLicenseResourceUserAllowance, ClarinLicenseResourceUserAllowanceRest> {
    @Override
    public ClarinLicenseResourceUserAllowanceRest convert(ClarinLicenseResourceUserAllowance modelObject,
                                                          Projection projection) {
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
