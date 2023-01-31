/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.dspace.alerts.AllowSessionsEnum;
import org.dspace.alerts.SystemWideAlert;
import org.dspace.alerts.service.SystemWideAlertService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.scripts.Process;

public class SystemWideAlertBuilder extends AbstractBuilder<SystemWideAlert, SystemWideAlertService> {

    private SystemWideAlert systemWideAlert;

    protected SystemWideAlertBuilder(Context context) {
        super(context);
    }

    public static SystemWideAlertBuilder createSystemWideAlert(Context context, String message)
            throws SQLException, AuthorizeException {
        SystemWideAlertBuilder systemWideAlertBuilder = new SystemWideAlertBuilder(context);
        return systemWideAlertBuilder.create(context, message, AllowSessionsEnum.ALLOW_ALL_SESSIONS, null, false);
    }

    private SystemWideAlertBuilder create(Context context, String message, AllowSessionsEnum allowSessionsType,
                                          Date countdownTo, boolean active)
            throws SQLException, AuthorizeException {
        this.context = context;
        this.systemWideAlert = systemWideAlertService.create(context, message, allowSessionsType, countdownTo, active);
        return this;
    }

    public SystemWideAlertBuilder withAllowSessions(AllowSessionsEnum allowSessionsType) {
        systemWideAlert.setAllowSessions(allowSessionsType);
        return this;
    }

    public SystemWideAlertBuilder withCountdownDate(Date countdownTo) {
        systemWideAlert.setCountdownTo(countdownTo);
        return this;
    }

    public SystemWideAlertBuilder isActive(boolean isActive) {
        systemWideAlert.setActive(isActive);
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            systemWideAlert = c.reloadEntity(systemWideAlert);
            if (systemWideAlert != null) {
                delete(c, systemWideAlert);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public SystemWideAlert build() {
        try {
            systemWideAlertService.update(context, systemWideAlert);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            return null;
        }
        return systemWideAlert;
    }


    @Override
    protected SystemWideAlertService getService() {
        return systemWideAlertService;
    }

    public void delete(Context c, SystemWideAlert alert) throws Exception {
        if (alert != null) {
            getService().delete(c, alert);
        }
    }

    public static void deleteProcess(Integer integer) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Process process = processService.find(c, integer);
            if (process != null) {
                try {
                    processService.delete(c, process);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }
}
