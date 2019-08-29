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
import java.util.Locale;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.ViewEventResource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.EventService;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(ViewEventRest.CATEGORY + "." + ViewEventRest.NAME)
public class ViewEventRestRepository extends DSpaceRestRepository<ViewEventRest, UUID> {

    @Autowired
    private EventService eventService;

    public ViewEventRest findOne(Context context, UUID uuid) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    public Page<ViewEventRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }


    @Override
    protected ViewEventRest createAndReturn(Context context)
        throws AuthorizeException, SQLException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        ViewEventRest viewEventRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            viewEventRest = mapper.readValue(input, ViewEventRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }
        if (viewEventRest.getTargetId() == null || StringUtils.isBlank(viewEventRest.getTargetType())) {
            throw new DSpaceBadRequestException("The given ViewEvent was invalid, one or more properties are either" +
                                                    "wrong or missing");
        }
        DSpaceObjectService dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(
            Constants.getTypeID(viewEventRest.getTargetType().toUpperCase(Locale.getDefault())));

        DSpaceObject dSpaceObject = dSpaceObjectService.find(context, viewEventRest.getTargetId());
        if (dSpaceObject == null) {
            throw new UnprocessableEntityException(
                "The given targetId does not resolve to a DSpaceObject: " + viewEventRest.getTargetId());
        }
        UsageEvent usageEvent = new UsageEvent(UsageEvent.Action.VIEW, req, context, dSpaceObject);
        eventService.fireEvent(usageEvent);
        return viewEventRest;
    }

    public Class<ViewEventRest> getDomainClass() {
        return ViewEventRest.class;
    }

    public DSpaceResource<ViewEventRest> wrapResource(ViewEventRest model, String... rels) {
        return new ViewEventResource(model, utils, rels);
    }
}
