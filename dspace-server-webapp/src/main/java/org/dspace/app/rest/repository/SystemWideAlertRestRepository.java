/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.alerts.AllowSessionsEnum;
import org.dspace.alerts.SystemWideAlert;
import org.dspace.alerts.service.SystemWideAlertService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SystemWideAlertRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * The repository for the SystemWideAlert workload
 */
@Component(SystemWideAlertRest.CATEGORY + "." + SystemWideAlertRest.NAME)
public class SystemWideAlertRestRepository extends DSpaceRestRepository<SystemWideAlertRest, Integer> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private SystemWideAlertService systemWideAlertService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected SystemWideAlertRest createAndReturn(Context context) throws SQLException, AuthorizeException {
        SystemWideAlert systemWideAlert = createSystemWideAlert(context);
        return converter.toRest(systemWideAlert, utils.obtainProjection());
    }


    @Override
    @PreAuthorize("permitAll()")
    public SystemWideAlertRest findOne(Context context, Integer id) {
        try {
            SystemWideAlert systemWideAlert = systemWideAlertService.find(context, id);
            if (systemWideAlert == null) {
                throw new ResourceNotFoundException(
                        "systemWideAlert with id " + systemWideAlert.getID() + " was not found");
            }
            return converter.toRest(systemWideAlert, utils.obtainProjection());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<SystemWideAlertRest> findAll(Context context, Pageable pageable) {
        try {
            List<SystemWideAlert> systemWideAlerts = systemWideAlertService.findAll(context, pageable.getPageSize(),
                                                                                    Math.toIntExact(
                                                                                            pageable.getOffset()));
            return converter.toRestPage(systemWideAlerts, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected SystemWideAlertRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                      Integer id, JsonNode jsonNode) throws SQLException, AuthorizeException {

        SystemWideAlertRest systemWideAlertRest;
        try {
            systemWideAlertRest = new ObjectMapper().readValue(jsonNode.toString(), SystemWideAlertRest.class);
        } catch (JsonProcessingException e) {
            throw new UnprocessableEntityException("Cannot parse JSON in request body", e);
        }

        if (systemWideAlertRest == null || isBlank(systemWideAlertRest.getMessage())) {
            throw new UnprocessableEntityException("system alert message cannot be blank");
        }

        SystemWideAlert systemWideAlert = systemWideAlertService.find(context, id);
        if (systemWideAlert == null) {
            throw new ResourceNotFoundException("system wide alert with id: " + id + " not found");
        }

        systemWideAlert.setMessage(systemWideAlertRest.getMessage());
        systemWideAlert.setAllowSessions(systemWideAlertRest.getAllowSessions());
        systemWideAlert.setCountdownTo(systemWideAlertRest.getCountdownTo());
        systemWideAlert.setActive(systemWideAlertRest.isActive());

        systemWideAlertService.update(context, systemWideAlert);
        context.commit();

        return converter.toRest(systemWideAlert, utils.obtainProjection());
    }


    /**
     * Helper method to create a system-wide alert and deny creation when one already exists
     * @param context   The database context
     * @return the created system-wide alert
     * @throws SQLException
     */
    private SystemWideAlert createSystemWideAlert(Context context)
            throws SQLException {
        List<SystemWideAlert> all = systemWideAlertService.findAll(context);
        if (!all.isEmpty()) {
            throw new DSpaceBadRequestException("A system wide alert already exists, no new value can be created. " +
                                                        "Try updating the existing one.");
        }


        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        SystemWideAlertRest systemWideAlertRest;
        try {
            ServletInputStream input = req.getInputStream();
            systemWideAlertRest = mapper.readValue(input, SystemWideAlertRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body.", e1);
        }

        SystemWideAlert systemWideAlert;

        try {
            systemWideAlert = systemWideAlertService.create(context, systemWideAlertRest.getMessage(),
                                                            AllowSessionsEnum.fromInt(
                                                                    systemWideAlertRest.getAllowSessions()),
                                                            systemWideAlertRest.getCountdownTo(),
                                                            systemWideAlertRest.isActive());
            systemWideAlertService.update(context, systemWideAlert);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return systemWideAlert;
    }


    @Override
    public Class<SystemWideAlertRest> getDomainClass() {
        return SystemWideAlertRest.class;
    }
}
