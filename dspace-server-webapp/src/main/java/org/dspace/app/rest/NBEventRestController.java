/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController will take care to manipulate the related item eventually associated with a nb event
 * "/api/integration/nbevents/{nbeventid}/related"
 */
@RestController
@RequestMapping("/api/integration/nbevents" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG + "/related")
public class NBEventRestController {
    @Autowired
    protected Utils utils;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private NBEventService nbEventService;

    /**
     * This method associate an item to a nb event
     * 
     * @param nbeventId       The nb event id
     * @param response        The current response
     * @param request         The current request
     * @param relatedItemUUID The uuid of the related item to associate with the nb
     *                        event
     * @return The related item
     * @throws SQLException       If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RepresentationModel<?>> postRelatedItem(@PathVariable(name = "id") String nbeventId,
            HttpServletResponse response, HttpServletRequest request,
            @RequestParam(required = true, name = "item") UUID relatedItemUUID)
        throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        NBEvent nbevent = nbEventService.findEventByEventId(context, nbeventId);
        if (nbevent == null) {
            throw new ResourceNotFoundException("No such nb event: " + nbeventId);
        }
        if (nbevent.getRelated() != null) {
            throw new UnprocessableEntityException("The nb event with ID: " + nbeventId + " already has " +
                                                       "a related item");
        } else if (!StringUtils.endsWith(nbevent.getTopic(), "/PROJECT")) {
            return ControllerUtils.toEmptyResponse(HttpStatus.BAD_REQUEST);
        }

        Item relatedItem = itemService.find(context, relatedItemUUID);
        if (relatedItem != null) {
            nbevent.setRelated(relatedItemUUID.toString());
            nbEventService.store(context, nbevent);
        } else {
            throw new UnprocessableEntityException("The proposed related item was not found");
        }
        ItemRest relatedItemRest = converterService.toRest(relatedItem, utils.obtainProjection());
        ItemResource itemResource = converterService.toResource(relatedItemRest);
        context.complete();
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), itemResource);
    }

    /**
     * This method remove the association to a related item from a nb event
     * 
     * @param nbeventId       The nb event id
     * @param response        The current response
     * @param request         The current request
     * @return The related item
     * @throws SQLException       If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RepresentationModel<?>> deleteAdminGroup(@PathVariable(name = "id") String nbeventId,
            HttpServletResponse response, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        Context context = ContextUtil.obtainContext(request);
        NBEvent nbevent = nbEventService.findEventByEventId(context, nbeventId);
        if (nbevent == null) {
            throw new ResourceNotFoundException("No such nb event: " + nbeventId);
        }
        if (nbevent.getRelated() != null) {
            nbevent.setRelated(null);
            nbEventService.store(context, nbevent);
            context.complete();
        }

        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }
}
