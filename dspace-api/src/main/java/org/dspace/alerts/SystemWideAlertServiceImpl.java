/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.alerts.dao.SystemWideAlertDAO;
import org.dspace.alerts.service.SystemWideAlertService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link SystemWideAlertService} class
 */
public class SystemWideAlertServiceImpl implements SystemWideAlertService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SystemWideAlertService.class);


    @Autowired
    private SystemWideAlertDAO systemWideAlertDAO;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public SystemWideAlert create(final Context context, final String message,
                                  final AllowSessionsEnum allowSessionsType,
                                  final Date countdownTo, final boolean active) throws SQLException,
            AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators can create a system-wide alert");
        }
        SystemWideAlert systemWideAlert = new SystemWideAlert();
        systemWideAlert.setMessage(message);
        systemWideAlert.setAllowSessions(allowSessionsType);
        systemWideAlert.setCountdownTo(countdownTo);
        systemWideAlert.setActive(active);

        SystemWideAlert createdAlert = systemWideAlertDAO.create(context, systemWideAlert);
        log.info(LogHelper.getHeader(context, "system_wide_alert_create",
                                     "System Wide Alert has been created with message: '" + message + "' and ID "
                                             + createdAlert.getID() + " and allowSessionsType " + allowSessionsType +
                                             " and active set to " + active));


        return createdAlert;
    }

    @Override
    public SystemWideAlert find(final Context context, final int alertId) throws SQLException {
        return systemWideAlertDAO.findByID(context, SystemWideAlert.class, alertId);
    }

    @Override
    public List<SystemWideAlert> findAll(final Context context) throws SQLException {
        return systemWideAlertDAO.findAll(context, SystemWideAlert.class);
    }

    @Override
    public List<SystemWideAlert> findAll(final Context context, final int limit, final int offset) throws SQLException {
        return systemWideAlertDAO.findAll(context, limit, offset);
    }

    @Override
    public List<SystemWideAlert> findAllActive(final Context context, final int limit, final int offset)
            throws SQLException {
        return systemWideAlertDAO.findAllActive(context, limit, offset);
    }

    @Override
    public void delete(final Context context, final SystemWideAlert systemWideAlert)
            throws SQLException, IOException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators can create a system-wide alert");
        }
        systemWideAlertDAO.delete(context, systemWideAlert);
        log.info(LogHelper.getHeader(context, "system_wide_alert_create",
                                     "System Wide Alert with ID " + systemWideAlert.getID() + " has been deleted"));

    }

    @Override
    public void update(final Context context, final SystemWideAlert systemWideAlert)
            throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators can create a system-wide alert");
        }
        systemWideAlertDAO.save(context, systemWideAlert);

    }

    @Override
    public boolean canNonAdminUserLogin(Context context) throws SQLException {
        List<SystemWideAlert> active = findAllActive(context, 1, 0);
        if (active == null || active.isEmpty()) {
            return true;
        }
        return active.get(0).getAllowSessions() == AllowSessionsEnum.ALLOW_ALL_SESSIONS;
    }

    @Override
    public boolean canUserMaintainSession(Context context, EPerson ePerson) throws SQLException {
        if (authorizeService.isAdmin(context, ePerson)) {
            return true;
        }
        List<SystemWideAlert> active = findAllActive(context, 1, 0);
        if (active == null || active.isEmpty()) {
            return true;
        }
        return active.get(0).getAllowSessions() != AllowSessionsEnum.ALLOW_ADMIN_SESSIONS_ONLY;
    }
}
