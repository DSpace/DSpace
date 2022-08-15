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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to register identifiers
 */
@RestController
@RequestMapping("/api/" + ItemRest.CATEGORY + "/" + ItemRest.PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID
        + "/" + IdentifiersRest.PLURAL_NAME)
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

    public IdentifiersRest get(@PathVariable UUID uuid, HttpServletRequest request,
                                          HttpServletResponse response)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        Item item = itemService.find(context, uuid);
        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }
        IdentifiersRest identifiersRest = new IdentifiersRest();
        List<IdentifierRest> identifierRests = new ArrayList<>();
        DOI doi = doiService.findDOIByDSpaceObject(context, item);
        String handle = HandleServiceFactory.getInstance().getHandleService().findHandle(context, item);
        try {
            if (doi != null) {
                String doiUrl = doiService.DOIToExternalForm(doi.getDoi());
                IdentifierRest identifierRest = new IdentifierRest(doiUrl, "doi", String.valueOf(doi.getStatus()));
                identifierRests.add(identifierRest);
            }
            if (handle != null) {
                identifierRests.add(new IdentifierRest(handle, "handle", null));
            }
        } catch (IdentifierException e) {
            throw new IllegalStateException("Failed to register identifier: " + e.getMessage());
        }

        return identifiersRest;
    }

    /**
     * Method to register an identifier for an item.
     *
     * @return OK
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADMIN')")
    public ResponseEntity<RepresentationModel<?>> registerIdentifierForItem(@PathVariable UUID uuid,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        Item item = itemService.find(context, uuid);

        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }

        String identifier = null;
        try {
            DOIIdentifierProvider doiIdentifierProvider = DSpaceServicesFactory.getInstance().getServiceManager()
                    .getServiceByName("org.dspace.identifier.DOIIdentifierProvider", DOIIdentifierProvider.class);
            doiIdentifierProvider.register(context, item, new TrueFilter());
            if (context != null) {
                context.commit();
            }
        } catch (IdentifierNotFoundException e) {
            return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
        } catch (IdentifierException e) {
            throw new IllegalStateException("Failed to register identifier: " + identifier);
        }
        return ControllerUtils.toEmptyResponse(HttpStatus.CREATED);
    }

}
