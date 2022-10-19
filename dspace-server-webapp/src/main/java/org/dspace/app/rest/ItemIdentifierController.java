/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.model.IdentifierRest;
import org.dspace.app.rest.model.IdentifiersRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.repository.ItemRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.logic.TrueFilter;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.factory.DSpaceServicesFactory;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to register a DOI for an item, if it has no DOI already, or a DOI in a state where it can be
 * advanced to queue for reservation or registration.
 *
 * @author Kim Shepherd
 */
@RestController
@RequestMapping("/api/" + ItemRest.CATEGORY + "/" + ItemRest.PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID
        + "/" + IdentifiersRest.NAME)
public class ItemIdentifierController {

    @Autowired
    ConverterService converter;

    @Autowired
    ItemService itemService;

    @Autowired
    ItemRestRepository itemRestRepository;

    @Autowired
    MetadataConverter metadataConverter;

    @Autowired
    IdentifierService identifierService;

    @Autowired
    DOIService doiService;

    @Autowired
    Utils utils;

    /**
     * Queue a new, pending or minted DOI for registration for a given item. Requires administrative privilege.
     * This request is sent from the Register DOI button (configurable) on the item status page.
     *
     * @return 302 FOUND if the DOI is already registered or reserved, 201 CREATED if queued for registration
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADMIN')")
    public ResponseEntity<RepresentationModel<?>> registerIdentifierForItem(@PathVariable UUID uuid,
                                                                            HttpServletRequest request,
                                                                            HttpServletResponse response)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        Item item = itemService.find(context, uuid);
        ItemRest itemRest;

        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }

        String identifier = null;
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        try {
            DOIIdentifierProvider doiIdentifierProvider = DSpaceServicesFactory.getInstance().getServiceManager()
                    .getServiceByName("org.dspace.identifier.DOIIdentifierProvider", DOIIdentifierProvider.class);
            if (doiIdentifierProvider != null) {
                DOI doi = doiService.findDOIByDSpaceObject(context, item);
                boolean exists = false;
                boolean pending = false;
                if (null != doi) {
                    exists = true;
                    Integer doiStatus = doiService.findDOIByDSpaceObject(context, item).getStatus();
                    // Check if this DOI has a status which makes it eligible for registration
                    if (null == doiStatus || DOIIdentifierProvider.MINTED.equals(doiStatus)
                            || DOIIdentifierProvider.PENDING.equals(doiStatus)) {
                        pending = true;
                    }
                }
                if (!exists || pending) {
                    // Mint identifier and return 201 CREATED
                    doiIdentifierProvider.register(context, item, new TrueFilter());
                    httpStatus = HttpStatus.CREATED;
                } else {
                    // This DOI exists and isn't in a state where it can be queued for registration
                    // We'll return 302 FOUND to indicate it's here and not an error, but no creation was performed
                    httpStatus = HttpStatus.FOUND;
                }
            } else {
                throw new IllegalStateException("No DOI provider is configured");
            }
        } catch (IdentifierNotFoundException e) {
            httpStatus = HttpStatus.NOT_FOUND;
        } catch (IdentifierException e) {
            throw new IllegalStateException("Failed to register identifier: " + identifier);
        }
        // We didn't exactly change the item, but we did queue an identifier which is closely associated with it
        // so we should update the last modified date here
        itemService.updateLastModified(context, item);
        itemRest = converter.toRest(item, utils.obtainProjection());
        context.complete();
        ItemResource itemResource = converter.toResource(itemRest);
        // Return the status and item resource
        return ControllerUtils.toResponseEntity(httpStatus, new HttpHeaders(), itemResource);
    }

}
