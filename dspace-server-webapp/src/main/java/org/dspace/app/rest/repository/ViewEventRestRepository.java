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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.EventService;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(ViewEventRest.CATEGORY + "." + ViewEventRest.NAME)
public class ViewEventRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private EventService eventService;

    private List<String> typeList = Arrays.asList(Constants.typeText);

    public ViewEventRest createViewEvent() throws AuthorizeException, SQLException {

        Context context = obtainContext();
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        ViewEventRest viewEventRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            viewEventRest = mapper.readValue(input, ViewEventRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }
        if (viewEventRest.getTargetId() == null || StringUtils.isBlank(viewEventRest.getTargetType()) ||
            !typeList.contains(viewEventRest.getTargetType().toUpperCase())) {
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
}
