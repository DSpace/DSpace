/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.service.SystemConfigVariableService;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SystemConfigVariableService}.
 *
 * <p>Uses {@link Utils#getAllowedTemplateConfig()} to retrieve the allowed configuration
 * properties for templates without hardcoding configuration keys.</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class SystemConfigVariableServiceImpl implements SystemConfigVariableService {

    private static final Logger log = LogManager.getLogger(SystemConfigVariableServiceImpl.class);

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public List<SystemConfigVariable> getConfigVariables(Context context)
            throws SQLException, AuthorizeException {
        if (context == null || !authorizeService.isAdmin(context)) {
            logUnauthorizedAttempt(context, "retrieve all system config variables");
            throw new AuthorizeException("Only administrators are allowed to access system config variables.");
        }
        Map<String, String> allowedConfigs = Utils.getAllowedTemplateConfig();
        List<SystemConfigVariable> vars = new ArrayList<>();
        for (Map.Entry<String, String> entry : allowedConfigs.entrySet()) {
            vars.add(new SystemConfigVariable(
                entry.getKey(),
                entry.getValue(),
                String.format("${config.get('%s')}", entry.getKey())
            ));
        }
        vars.sort(Comparator.comparing(SystemConfigVariable::getKey));
        return vars;
    }

    @Override
    public SystemConfigVariable getConfigVariable(Context context, String key)
            throws SQLException, AuthorizeException {
        if (context == null || !authorizeService.isAdmin(context)) {
            logUnauthorizedAttempt(context, "retrieve system config variable '" + key + "'");
            throw new AuthorizeException("Only administrators are allowed to access system config variables.");
        }
        if (key == null) {
            return null;
        }
        Map<String, String> allowedConfigs = Utils.getAllowedTemplateConfig();
        String value = allowedConfigs.get(key);
        if (value != null) {
            return new SystemConfigVariable(
                key,
                value,
                String.format("${config.get('%s')}", key)
            );
        }
        return null;
    }

    /**
     * Logs a formatted warning indicating an unauthorized access attempt by a non-administrator.
     *
     * @param context the DSpace context
     * @param action  the action attempted
     */
    private void logUnauthorizedAttempt(Context context, String action) {
        EPerson currentUser = (context != null) ? context.getCurrentUser() : null;
        String userIdentification = (currentUser != null)
                ? String.format("User '%s' (ID: %s, Email: %s)",
                        currentUser.getName(), currentUser.getID(), currentUser.getEmail())
                : "Anonymous user";
        log.warn(LogHelper.getHeader(context, "unauthorized_access_attempt",
                String.format("Access denied: %s attempted to %s without administrator privileges.",
                        userIdentification, action)));
    }
}
