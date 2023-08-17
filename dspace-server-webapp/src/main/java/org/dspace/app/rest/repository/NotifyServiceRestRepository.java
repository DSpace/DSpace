/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.NotifyServiceRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage NotifyService Rest object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */

@Component(NotifyServiceRest.CATEGORY + "." + NotifyServiceRest.NAME)
public class NotifyServiceRestRepository extends DSpaceRestRepository<NotifyServiceRest, Integer> {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    ResourcePatch<NotifyServiceEntity> resourcePatch;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public NotifyServiceRest findOne(Context context, Integer id) {
        try {
            NotifyServiceEntity notifyServiceEntity = notifyService.find(context, id);
            if (notifyServiceEntity == null) {
                throw new ResourceNotFoundException("The notifyService for ID: " + id + " could not be found");
            }
            return converter.toRest(notifyServiceEntity, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<NotifyServiceRest> findAll(Context context, Pageable pageable) {
        try {
            return converter.toRestPage(notifyService.findAll(context), pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    protected NotifyServiceRest createAndReturn(Context context) throws AuthorizeException, SQLException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        NotifyServiceRest notifyServiceRest;
        try {
            ServletInputStream input = req.getInputStream();
            notifyServiceRest = mapper.readValue(input, NotifyServiceRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        NotifyServiceEntity notifyServiceEntity = notifyService.create(context);
        notifyServiceEntity.setName(notifyServiceRest.getName());
        notifyServiceEntity.setDescription(notifyServiceRest.getDescription());
        notifyServiceEntity.setUrl(notifyServiceRest.getUrl());
        notifyServiceEntity.setLdnUrl(notifyServiceRest.getLdnUrl());
        notifyService.update(context, notifyServiceEntity);

        return converter.toRest(notifyServiceEntity, utils.obtainProjection());
    }
    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
                         Patch patch) throws AuthorizeException, SQLException {
        NotifyServiceEntity notifyServiceEntity = notifyService.find(context, id);
        if (notifyServiceEntity == null) {
            throw new ResourceNotFoundException(
                NotifyServiceRest.CATEGORY + "." + NotifyServiceRest.NAME + " with id: " + id + " not found");
        }
        resourcePatch.patch(context, notifyServiceEntity, patch.getOperations());
        notifyService.update(context, notifyServiceEntity);
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            NotifyServiceEntity notifyServiceEntity = notifyService.find(context, id);
            if (notifyServiceEntity == null) {
                throw new ResourceNotFoundException(NotifyServiceRest.CATEGORY + "." +
                    NotifyServiceRest.NAME + " with id: " + id + " not found");
            }
            notifyService.delete(context, notifyServiceEntity);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "byLdnUrl")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public NotifyServiceRest findByLdnUrl(@Parameter(value = "ldnUrl", required = true) String ldnUrl) {
        try {
            NotifyServiceEntity notifyServiceEntity = notifyService.findByLdnUrl(obtainContext(), ldnUrl);
            if (notifyServiceEntity == null) {
                return null;
            }
            return converter.toRest(notifyServiceEntity, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<NotifyServiceRest> getDomainClass() {
        return NotifyServiceRest.class;
    }

}
