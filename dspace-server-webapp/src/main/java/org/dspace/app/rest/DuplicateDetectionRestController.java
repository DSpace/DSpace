/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.PotentialDuplicateRest;
import org.dspace.app.rest.model.hateoas.PotentialDuplicateResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Item;
import org.dspace.content.service.DuplicateDetectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.virtual.PotentialDuplicate;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for the api/duplicates endpoint, which handles requests for finding
 * potential duplicates
 *
 * @author Kim Shepherd
 */
@RestController
@ConditionalOnProperty("duplicate.enable")
@RequestMapping("/api/" + PotentialDuplicateRest.CATEGORY)
public class DuplicateDetectionRestController implements InitializingBean {

    private static final Logger log = LogManager.getLogger(DuplicateDetectionRestController.class);

    @Autowired
    protected Utils utils;
    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private DuplicateDetectionService duplicateDetectionService;
    @Autowired
    private ConverterService converter;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays.asList(Link.of("/api/" + PotentialDuplicateRest.CATEGORY,
                    PotentialDuplicateRest.CATEGORY)));
    }

    /**
     * Return a paged list of potential duplicate matches for the given item ID. This may be an item wrapped in
     * an in-progress item wrapper like workspace or workflow, as long as the current user has READ access to this item.
     * Results from the service search method will only contain matches that lead to items which are readable by
     * the current user.
     *
     * @param uuid The item UUID to search
     * @param page Pagination options
     * @param assembler The paged resources assembler to construct the paged model
     * @return Paged list of potential duplicates
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    public PagedModel<PotentialDuplicateResource> searchPotentialDuplicates(
            @RequestParam(name = "uuid", required = true) UUID uuid, Pageable page, PagedResourcesAssembler assembler) {

        // Instantiate object to represent this item
        Item item;
        // Instantiate list of potential duplicates which we will convert and return as paged ItemRest list
        List<PotentialDuplicate> potentialDuplicates = new LinkedList<>();
        // Instantiate total count
        int total = 0;
        // Obtain context
        Context context = ContextUtil.obtainCurrentRequestContext();
        // Get pagination
        Pageable pageable = utils.getPageable(page);

        // Try to get item based on UUID parameter
        try {
            item = itemService.find(context, uuid);
        } catch (SQLException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }

        // If the item is null or otherwise invalid (template, etc) then throw an appropriate error
        if (item == null) {
            throw new ResourceNotFoundException("No such item: " + uuid);
        }
        if (item.getTemplateItemOf() != null) {
            throw new IllegalArgumentException("Cannot get duplicates for template item");
        }

        try {
            // Search for the list of potential duplicates
            potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item);
        } catch (SearchServiceException e) {
            // If the search fails, log an error and return an empty list rather than throwing a fatal error
            log.error("Search service error retrieving duplicates: {}", e.getMessage());
        }

        // Construct rest and resource pages
        Page<PotentialDuplicateRest> restPage = converter.toRestPage(potentialDuplicates, pageable, total,
                utils.obtainProjection());
        Page<PotentialDuplicateResource> resourcePage = restPage.map(potentialDuplicateRest ->
                new PotentialDuplicateResource(potentialDuplicateRest));

        // Return the list of items along with pagination info and max results, assembled as PagedModel
        return assembler.toModel(resourcePage);

    }
}
