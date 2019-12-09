/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.HarvestedCollectionConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.app.rest.model.hateoas.HarvestedCollectionResource;
import org.dspace.app.rest.repository.HarvestedCollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles the harvest settings for collections
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@RestController
@RequestMapping("/api/core/collections/" +
    "{collectionUuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12" +
    "}}/harvester")
public class CollectionHarvestSettingsController {

    @Autowired
    HarvestedCollectionConverter harvestedCollectionConverter;

    @Autowired
    CollectionService collectionService;

    @Autowired
    ConverterService converter;

    @Autowired
    HarvestedCollectionService harvestedCollectionService;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    HarvestedCollectionRestRepository harvestedCollectionRestRepository;

    @Autowired
    private Utils utils;

    /**
     * GET endpoint that returns the harvest settings of the given collection
     * @param request   The request object
     * @param response  The response object
     * @return a HarvesterMetadataResource containing all available metadata formats
     */
    @PreAuthorize("hasPermission(#collectionUuid, 'COLLECTION', 'WRITE')")
    @RequestMapping(method = RequestMethod.GET)
    public HarvestedCollectionResource get(@PathVariable UUID collectionUuid,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);

        if (collection == null) {
            throw new ResourceNotFoundException("Collection with uuid: " + collectionUuid + " not found");
        }

        HarvestedCollectionRest harvestedCollectionRest = harvestedCollectionRestRepository.findOne(collection);
        HarvestedCollectionResource resource = converter.toResource(harvestedCollectionRest);

        return resource;
    }

    /**
     * PUT Endpoint for updating the settings of a collection.
     *
     * @param collectionUuid    The collection whose settings should be changed
     * @param response          The response object
     * @param request           The request object
     * @throws SQLException
     */
    @PreAuthorize("hasPermission(#collectionUuid, 'COLLECTION', 'WRITE')")
    @RequestMapping(method = RequestMethod.PUT, consumes = {"application/json"})
    public HarvestedCollectionResource updateHarvestSettingsEndpoint(@PathVariable UUID collectionUuid,
                                              HttpServletResponse response,
                                              HttpServletRequest request) throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);
        HarvestedCollectionResource harvestedCollectionResource = null;

        if (collection == null) {
            throw new ResourceNotFoundException("Collection with uuid: " + collectionUuid + " not found");
        }

        HarvestedCollectionRest harvestedCollectionRest =
            harvestedCollectionRestRepository.update(context, request, collection);

        // Return a harvestedCollectionResource only if a new harvestedCollection was created
        if (harvestedCollectionRest != null) {
            harvestedCollectionResource = converter.toResource(harvestedCollectionRest);
        }

        context.commit();

        return harvestedCollectionResource;
    }
}
