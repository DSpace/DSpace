/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.alerts.SystemWideAlert;
import org.dspace.app.rest.model.SystemWideAlertRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link SystemWideAlert} to an object of {@link SystemWideAlertRest}
 */
@Component
public class SystemWideAlertConverter implements DSpaceConverter<SystemWideAlert, SystemWideAlertRest> {


    @Override
    public SystemWideAlertRest convert(SystemWideAlert systemWideAlert, Projection projection) {
        SystemWideAlertRest systemWideAlertRest = new SystemWideAlertRest();
        systemWideAlertRest.setProjection(projection);
        systemWideAlertRest.setId(systemWideAlert.getID());
        systemWideAlertRest.setAlertId(systemWideAlert.getID());
        systemWideAlertRest.setMessage(systemWideAlert.getMessage());
        systemWideAlertRest.setAllowSessions(systemWideAlert.getAllowSessions().getValue());
        systemWideAlertRest.setCountdownTo(systemWideAlert.getCountdownTo());
        systemWideAlertRest.setActive(systemWideAlert.isActive());
        return systemWideAlertRest;
    }

    @Override
    public Class<SystemWideAlert> getModelClass() {
        return SystemWideAlert.class;
    }
}
