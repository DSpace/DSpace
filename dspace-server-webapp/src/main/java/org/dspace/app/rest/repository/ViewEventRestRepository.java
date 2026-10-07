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
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.app.rest.model.wrapper.SavedHttpServletRequestWrapper;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.EventService;
import org.dspace.services.model.Event;
import org.dspace.usage.UsageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(ViewEventRest.CATEGORY + "." + ViewEventRest.PLURAL_NAME)
public class ViewEventRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private EventService eventService;

    @Autowired
    private ObjectMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(ViewEventRestRepository.class);

    private final List<String> typeList = Arrays.asList(Constants.typeText);

    public ViewEventRest createViewEvent() throws SQLException {

        Context context = obtainContext();
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ViewEventRest viewEventRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            viewEventRest = mapper.readValue(input, ViewEventRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }
        final UUID targetId = viewEventRest.getTargetId();
        if (targetId == null || StringUtils.isBlank(viewEventRest.getTargetType()) ||
            !typeList.contains(viewEventRest.getTargetType().toUpperCase())) {
            throw new DSpaceBadRequestException("The given ViewEvent was invalid, one or more properties are either" +
                                                    " wrong or missing");
        }
        DSpaceObjectService<?> dSpaceObjectService =
            ContentServiceFactory.getInstance().getDSpaceObjectService(
                Constants.getTypeID(
                    viewEventRest.getTargetType()
                                 .toUpperCase(Locale.getDefault())
                )
            );

        if (!dSpaceObjectService.exists(context, targetId)) {
            throw new UnprocessableEntityException(
                "The given targetId does not resolve to a DSpaceObject: " + targetId);
        }
        final String referrer = viewEventRest.getReferrer();
        SavedHttpServletRequestWrapper requestWrapper = new SavedHttpServletRequestWrapper(req);
        eventService.scheduleAsyncEventConsumer(
            (consumer) -> eventConsumerScheduler(consumer, requestWrapper, dSpaceObjectService, targetId, referrer)
        );
        return viewEventRest;
    }

    protected void eventConsumerScheduler(
        Consumer<Event> eventConsumer,
        HttpServletRequestWrapper requestWrapper,
        DSpaceObjectService<?> dSpaceObjectService,
        UUID targetId,
        String referrer
    ) {
        try (Context ctx = new Context()) {
            eventConsumer.accept(
                UsageEvent.createUsageEvent(
                    ctx,
                    requestWrapper,
                    dSpaceObjectService,
                    targetId,
                    referrer
                )
            );
            ctx.complete();
        } catch (SQLException e) {
            log.error("Cannot persist the ViewEvent!", e);
        }
    }
}
