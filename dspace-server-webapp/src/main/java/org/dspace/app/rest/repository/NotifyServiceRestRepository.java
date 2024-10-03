/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static java.lang.String.format;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.NotifyServiceInboundPatternRest;
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

@Component(NotifyServiceRest.CATEGORY + "." + NotifyServiceRest.PLURAL_NAME)
public class NotifyServiceRestRepository extends DSpaceRestRepository<NotifyServiceRest, Integer> {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private NotifyServiceInboundPatternService inboundPatternService;

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
    @PreAuthorize("hasAuthority('ADMIN')")
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

        if (notifyServiceRest.getScore() != null) {
            if (notifyServiceRest.getScore().compareTo(java.math.BigDecimal.ZERO) == -1 ||
                notifyServiceRest.getScore().compareTo(java.math.BigDecimal.ONE) == 1) {
                throw new UnprocessableEntityException(format("Score out of range [0, 1] %s",
                    notifyServiceRest.getScore().setScale(4).toPlainString()));
            }
        }

        if (notifyService.findByLdnUrl(context,notifyServiceRest.getLdnUrl()) != null) {
            throw new UnprocessableEntityException(format("LDN url already in use %s",
                notifyServiceRest.getLdnUrl()));
        }

        NotifyServiceEntity notifyServiceEntity = notifyService.create(context, notifyServiceRest.getName());
        notifyServiceEntity.setDescription(notifyServiceRest.getDescription());
        notifyServiceEntity.setUrl(notifyServiceRest.getUrl());
        notifyServiceEntity.setLdnUrl(notifyServiceRest.getLdnUrl());
        notifyServiceEntity.setEnabled(notifyServiceRest.isEnabled());
        notifyServiceEntity.setLowerIp(notifyServiceRest.getLowerIp());
        notifyServiceEntity.setUpperIp(notifyServiceRest.getUpperIp());

        if (notifyServiceRest.getNotifyServiceInboundPatterns() != null) {
            appendNotifyServiceInboundPatterns(context, notifyServiceEntity,
                notifyServiceRest.getNotifyServiceInboundPatterns());
        }

        notifyServiceEntity.setScore(notifyServiceRest.getScore());

        notifyService.update(context, notifyServiceEntity);

        return converter.toRest(notifyServiceEntity, utils.obtainProjection());
    }

    private void appendNotifyServiceInboundPatterns(Context context, NotifyServiceEntity notifyServiceEntity,
        List<NotifyServiceInboundPatternRest> inboundPatternRests) throws SQLException {

        List<NotifyServiceInboundPattern> inboundPatterns = new ArrayList<>();

        for (NotifyServiceInboundPatternRest inboundPatternRest : inboundPatternRests) {
            NotifyServiceInboundPattern inboundPattern = inboundPatternService.create(context, notifyServiceEntity);
            inboundPattern.setPattern(inboundPatternRest.getPattern());
            inboundPattern.setConstraint(inboundPatternRest.getConstraint());
            inboundPattern.setAutomatic(inboundPatternRest.isAutomatic());

            inboundPatterns.add(inboundPattern);
        }

        notifyServiceEntity.setInboundPatterns(inboundPatterns);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
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
    @PreAuthorize("hasAuthority('ADMIN')")
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

    @SearchRestMethod(name = "byInboundPattern")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<NotifyServiceRest> findManualServicesByInboundPattern(
        @Parameter(value = "pattern", required = true) String pattern,
        Pageable pageable) {
        try {
            List<NotifyServiceEntity> notifyServiceEntities =
                notifyService.findManualServicesByInboundPattern(obtainContext(), pattern)
                .stream()
                .filter(NotifyServiceEntity::isEnabled)
                .collect(Collectors.toList());

            return converter.toRestPage(notifyServiceEntities, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<NotifyServiceRest> getDomainClass() {
        return NotifyServiceRest.class;
    }
}
