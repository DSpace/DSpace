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

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController will take care to manipulate the related item eventually
 * associated with a qa event
 * "/api/integration/qualityassuranceevents/{qaeventid}/related"
 */
@RestController
@RequestMapping("/api/" + QAEventRest.CATEGORY + "/" + QAEventRest.PLURAL_NAME
    + REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG + "/" + QAEventRest.RELATED)
public class QAEventRelatedRestController {

    @Autowired
    protected Utils utils;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private QAEventService qaEventService;

    /**
     * This method associate an item to a qa event
     * 
     * @param qaeventId       The qa event id
     * @param relatedItemUUID The uuid of the related item to associate with the qa event
     * @return The related item
     * @throws SQLException       If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RepresentationModel<?>> addRelatedItem(@PathVariable(name = "id") String qaeventId,
        @RequestParam(name = "item") UUID relatedItemUUID) throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainCurrentRequestContext();
        QAEvent qaevent = qaEventService.findEventByEventId(qaeventId);
        if (qaevent == null) {
            throw new ResourceNotFoundException("No such qa event: " + qaeventId);
        }

        if (!qaEventService.isRelatedItemSupported(qaevent)) {
            throw new UnprocessableEntityException("The given event does not supports a related item");
        }

        if (qaevent.getRelated() != null) {
            throw new UnprocessableEntityException("The given event already has a related item");
        }

        Item relatedItem = itemService.find(context, relatedItemUUID);
        if (relatedItem == null) {
            throw new UnprocessableEntityException("The proposed related item was not found");
        }

        qaevent.setRelated(relatedItemUUID.toString());
        qaEventService.store(context, qaevent);

        ItemRest relatedItemRest = converterService.toRest(relatedItem, utils.obtainProjection());
        ItemResource itemResource = converterService.toResource(relatedItemRest);

        context.complete();

        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), itemResource);
    }

    /**
     * This method remove the association to a related item from a qa event
     * 
     * @param qaeventId       The qa event id
     * @return The related item
     * @throws SQLException       If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RepresentationModel<?>> removeRelatedItem(@PathVariable(name = "id") String qaeventId)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainCurrentRequestContext();
        QAEvent qaevent = qaEventService.findEventByEventId(qaeventId);

        if (qaevent == null) {
            throw new ResourceNotFoundException("No such qa event: " + qaeventId);
        }

        if (!qaEventService.isRelatedItemSupported(qaevent)) {
            throw new UnprocessableEntityException("The given event does not supports a related item");
        }

        if (qaevent.getRelated() != null) {
            qaevent.setRelated(null);
            qaEventService.store(context, qaevent);
            context.complete();
        }

        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }
}
