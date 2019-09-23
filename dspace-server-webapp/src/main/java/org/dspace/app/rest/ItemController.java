/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.BundleConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.hateoas.BundleResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/items" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/bundles")
public class ItemController {

    @Autowired
    ItemService itemService;

    @Autowired
    BundleService bundleService;

    @Autowired
    BundleConverter converter;

    @Autowired
    MetadataConverter metadataConverter;

    @Autowired
    Utils utils;

    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public ResponseEntity<ResourceSupport> addBundleToItem(@PathVariable UUID uuid,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        Item item = itemService.find(context, uuid);

        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }

        BundleRest bundleRest;
        try {
            bundleRest = new ObjectMapper().readValue(request.getInputStream(), BundleRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("Could not parse request body");
        }

        if (item.getBundles(bundleRest.getName()).size() > 0) {
            throw new DSpaceBadRequestException("The bundle name already exists in the item");
        }

        Bundle bundle = bundleService.create(context, item, bundleRest.getName());

        metadataConverter.setMetadata(context, bundle, bundleRest.getMetadata());
        bundle.setName(context, bundleRest.getName());

        context.commit();

        BundleResource bundleResource = new BundleResource(converter.convert(bundle), utils);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, bundleResource);

    }

}
