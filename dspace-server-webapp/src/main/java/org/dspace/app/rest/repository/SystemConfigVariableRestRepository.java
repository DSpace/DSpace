/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.converter.SystemConfigVariableConverter;
import org.dspace.app.rest.model.SystemConfigVariableRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.SystemConfigVariable;
import org.dspace.core.service.SystemConfigVariableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * REST repository for retrieving general system configuration variables allowed in templates.
 *
 * <p>Exposes:
 * <ul>
 *   <li>{@code GET /api/system/systemconfigvariables} - paginated list of all allowed configs</li>
 *   <li>{@code GET /api/system/systemconfigvariables/{key}} - retrieve a single config by key</li>
 * </ul>
 * </p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@Component(SystemConfigVariableRest.CATEGORY + "." + SystemConfigVariableRest.PLURAL_NAME)
public class SystemConfigVariableRestRepository extends DSpaceRestRepository<SystemConfigVariableRest, String> {

    @Autowired
    private SystemConfigVariableService systemConfigVariableService;

    @Autowired
    private SystemConfigVariableConverter systemConfigVariableConverter;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SystemConfigVariableRest> findAll(Context context, Pageable pageable) {
        try {
            List<SystemConfigVariable> vars = systemConfigVariableService.getConfigVariables(context);
            return converter.toRestPage(vars, pageable, utils.obtainProjection());
        } catch (AuthorizeException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public SystemConfigVariableRest findOne(Context context, String key) {
        try {
            SystemConfigVariable var = systemConfigVariableService.getConfigVariable(context, key);
            return var != null ? systemConfigVariableConverter.convert(var, utils.obtainProjection()) : null;
        } catch (AuthorizeException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<SystemConfigVariableRest> getDomainClass() {
        return SystemConfigVariableRest.class;
    }
}
