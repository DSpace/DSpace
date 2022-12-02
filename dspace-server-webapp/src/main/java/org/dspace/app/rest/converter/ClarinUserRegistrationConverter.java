/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.springframework.stereotype.Component;

@Component
public class ClarinUserRegistrationConverter implements DSpaceConverter<ClarinUserRegistration, ClarinUserRegistrationRest> {

    @Override
    public ClarinUserRegistrationRest convert(ClarinUserRegistration modelObject, Projection projection) {
        ClarinUserRegistrationRest clarinUserRegistrationRest = new ClarinUserRegistrationRest();
        clarinUserRegistrationRest.setProjection(projection);
        clarinUserRegistrationRest.setId(modelObject.getID());
        clarinUserRegistrationRest.setePersonID(modelObject.getPersonID());
        clarinUserRegistrationRest.setEmail(modelObject.getEmail());
        clarinUserRegistrationRest.setOrganization(modelObject.getOrganization());
        clarinUserRegistrationRest.setConfirmation(modelObject.isConfirmation());
        return clarinUserRegistrationRest;
    }

    @Override
    public Class<ClarinUserRegistration> getModelClass() {
        return ClarinUserRegistration.class;
    }
}
