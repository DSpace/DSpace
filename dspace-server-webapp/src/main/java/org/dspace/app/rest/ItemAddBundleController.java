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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.BundleResource;
import org.dspace.app.rest.repository.ItemRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Controller to add bundles to a certain item, indicated by a uuid in the request
 * Usage: POST /api/core/items/<:uuid>/bundles (with name and metadata of bundle in request json)
 * Example:
 * <pre>
 * {@code
 * curl -X POST https://<dspace.server.url>/api/core/items/1911e8a4-6939-490c-b58b-a5d70f8d91fb/bundles
 *  -H 'Authorization: Bearer eyJhbGciOiJI...'
 *  -H 'Content-Type: application/json
 *  -d {
 *      "name": "ORIGINAL",
 *      "metadata": {...}
 *     }
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/" + ItemRest.CATEGORY + "/" + ItemRest.PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID
        + "/" + BundleRest.PLURAL_NAME)
public class ItemAddBundleController {

    private static final Logger log = LoggerFactory.getLogger(ItemAddBundleController.class);

    @Autowired
    ConverterService converter;

    @Autowired
    ItemService itemService;

    @Autowired
    ItemRestRepository itemRestRepository;

    @Autowired
    MetadataConverter metadataConverter;

    @Autowired
    Utils utils;

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    /**
     * Method to add a Bundle to an Item with the given UUID in the URL. This will create a Bundle with the
     * name provided in the request and attach this to the Item that matches the UUID in the URL.
     *
     * @return The created BundleResource
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public ResponseEntity<RepresentationModel<?>> addBundleToItem(@PathVariable UUID uuid,
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

        Bundle bundle = itemRestRepository.addBundleToItem(context, item, bundleRest);
        BundleResource bundleResource = converter.toResource(converter.toRest(bundle, utils.obtainProjection()));
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), bundleResource);
    }

    /**
     * Method to update the license for all bitstreams in the bundle of an item with the given UUID in the URL.
     * This will remove the old license and attach the new license to all bitstreams in the bundle.
     * The license is identified by the licenseID parameter in the request. If the licenseID is not found (-1), the old
     * license will be detached, but the new license will not be attached.
     * @param uuid
     * @param licenseId
     * @param request
     * @param response
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     */
    @RequestMapping(method = RequestMethod.PUT)
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public ItemRest updateLicenseForBundle(@PathVariable UUID uuid,
                                                                  @RequestParam("licenseID") Integer licenseId,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        if (Objects.isNull(context)) {
            throw new BadRequestException("No context found for current user");
        }
        Item item = itemService.find(context, uuid);

        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }

        ClarinLicense clarinLicense = clarinLicenseService.find(context, licenseId);
        if (Objects.isNull(clarinLicense)) {
            log.warn("Cannot find clarin license with id: " + licenseId + ". The old license will be detached, " +
                    "but the new one will not be attached.");
        }
        List<Bundle> bundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        for (Bundle clarinBundle : bundles) {
            List<Bitstream> bitstreamList = clarinBundle.getBitstreams();
            for (Bitstream bundleBitstream : bitstreamList) {
                // in case bitstream ID exists in license table for some reason .. just remove it
                this.clarinLicenseResourceMappingService.detachLicenses(context, bundleBitstream);
                if (Objects.nonNull(clarinLicense)) {
                    // add the license to bitstream
                    this.clarinLicenseResourceMappingService.attachLicense(context, clarinLicense, bundleBitstream);
                }
            }
        }
        clarinLicenseService.clearLicenseMetadataFromItem(context, item);
        if (Objects.nonNull(clarinLicense)) {
            clarinLicenseService.addLicenseMetadataToItem(context, clarinLicense, item);
        }

        itemService.update(context, item);
        context.commit();

        return converter.toRest(item, utils.obtainProjection());
    }
}
