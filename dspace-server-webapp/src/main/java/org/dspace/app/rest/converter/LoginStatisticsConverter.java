/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.LoginStatisticsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.LoginStatistics;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts
 * {@link LoginStatistics} to {@link LoginStatisticsRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class LoginStatisticsConverter implements DSpaceConverter<LoginStatistics, LoginStatisticsRest> {

    @Override
    public LoginStatisticsRest convert(LoginStatistics modelObject, Projection projection) {

        EPerson user = modelObject.getUser();

        LoginStatisticsRest rest = new LoginStatisticsRest();
        rest.setProjection(projection);
        rest.setId(user.getID().toString());
        rest.setName(user.getFullName());
        rest.setEmail(user.getEmail());
        rest.setCount(modelObject.getCount());

        return rest;
    }

    @Override
    public Class<LoginStatistics> getModelClass() {
        return LoginStatistics.class;
    }

}
