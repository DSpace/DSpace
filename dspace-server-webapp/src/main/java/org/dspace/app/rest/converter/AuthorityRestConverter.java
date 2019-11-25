/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.ChoiceAuthority;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the ChoiceAuthority in the DSpace API data
 * model and the REST data model
 *
 * TODO please do not use this convert but use the wrapper
 * {@link AuthorityUtils#convertAuthority(ChoiceAuthority, String, String)}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class AuthorityRestConverter implements DSpaceConverter<ChoiceAuthority, AuthorityRest> {

    @Override
    public AuthorityRest convert(ChoiceAuthority step, Projection projection) {
        AuthorityRest authorityRest = new AuthorityRest();
        authorityRest.setProjection(projection);
        authorityRest.setHierarchical(step.isHierarchical());
        authorityRest.setScrollable(step.isScrollable());
        authorityRest.setIdentifier(step.hasIdentifier());
        authorityRest.setPreloadLevel(step.getPreloadLevel());
        return authorityRest;
    }

    @Override
    public Class<ChoiceAuthority> getModelClass() {
        return ChoiceAuthority.class;
    }
}
