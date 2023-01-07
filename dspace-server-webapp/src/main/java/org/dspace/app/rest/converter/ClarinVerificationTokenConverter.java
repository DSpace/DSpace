/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinVerificationTokenRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinVerificationToken;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the ClarinVerificationToken in the DSpace API data model and the
 * REST data model
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class ClarinVerificationTokenConverter implements DSpaceConverter<ClarinVerificationToken,
        ClarinVerificationTokenRest> {

    @Override
    public ClarinVerificationTokenRest convert(ClarinVerificationToken modelObject, Projection projection) {
        ClarinVerificationTokenRest clarinVerificationTokenRest = new ClarinVerificationTokenRest();
        clarinVerificationTokenRest.setId(modelObject.getID());
        clarinVerificationTokenRest.setToken(modelObject.getToken());
        clarinVerificationTokenRest.setShibHeaders(modelObject.getShibHeaders());
        clarinVerificationTokenRest.setEmail(modelObject.getEmail());
        clarinVerificationTokenRest.setePersonNetID(modelObject.getePersonNetID());
        clarinVerificationTokenRest.setProjection(projection);
        return clarinVerificationTokenRest;
    }

    @Override
    public Class<ClarinVerificationToken> getModelClass() {
        return ClarinVerificationToken.class;
    }
}
