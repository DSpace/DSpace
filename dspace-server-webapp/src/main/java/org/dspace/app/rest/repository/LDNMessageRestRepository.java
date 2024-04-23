/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.exception.MethodNotAllowedException;
import org.dspace.app.rest.model.LDNMessageEntityRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage LDNMessageEntry Rest object
 *
 * @author Stefano Maffei(stefano.maffei at 4science.com)
 */

@Component(LDNMessageEntityRest.CATEGORY + "." + LDNMessageEntityRest.NAME_PLURALS)
public class LDNMessageRestRepository extends DSpaceRestRepository<LDNMessageEntityRest, String> {

    @Autowired
    private LDNMessageService ldnMessageService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public LDNMessageEntityRest findOne(Context context, String id) {
        try {
            LDNMessageEntity ldnMessageEntity = ldnMessageService.find(context, id);
            if (ldnMessageEntity == null) {
                throw new ResourceNotFoundException("The LDNMessageEntity for ID: " + id + " could not be found");
            }
            return converter.toRest(ldnMessageEntity, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<LDNMessageEntityRest> findAll(Context context, Pageable pageable) {
        try {
            return converter.toRestPage(ldnMessageService.findAll(context), pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected LDNMessageEntityRest createAndReturn(Context context) throws AuthorizeException, SQLException {
        throw new MethodNotAllowedException("Creation of LDN Message is not supported via Endpoint");
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, String id,
        Patch patch) throws AuthorizeException, SQLException {
        throw new MethodNotAllowedException("Patch of LDN Message is not supported via Endpoint");
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, String id) throws AuthorizeException {
        throw new MethodNotAllowedException("Deletion of LDN Message is not supported via Endpoint");
    }

    @Override
    public Class<LDNMessageEntityRest> getDomainClass() {
        return LDNMessageEntityRest.class;
    }

}
