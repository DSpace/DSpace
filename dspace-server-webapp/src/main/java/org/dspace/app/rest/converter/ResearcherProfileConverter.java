/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming an model that represent a
 * ResearcherProfile to the REST representation of an ResearcherProfile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class ResearcherProfileConverter implements DSpaceConverter<ResearcherProfile, ResearcherProfileRest> {

    @Override
    public ResearcherProfileRest convert(ResearcherProfile profile, Projection projection) {
        ResearcherProfileRest researcherProfileRest = new ResearcherProfileRest();
        researcherProfileRest.setVisible(profile.isVisible());
        researcherProfileRest.setId(profile.getId());
        researcherProfileRest.setProjection(projection);
        return researcherProfileRest;
    }

    @Override
    public Class<ResearcherProfile> getModelClass() {
        return ResearcherProfile.class;
    }

}
