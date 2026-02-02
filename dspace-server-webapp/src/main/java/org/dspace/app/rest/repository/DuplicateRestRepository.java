/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.PotentialDuplicateRest;
import org.dspace.app.rest.utils.ContextUtil;
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
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * The REST repository for the api/submission/duplicates endpoint, which handles requests for finding
 * potential duplicates of a given item (archived or in-progress).
 *
 * Find one and find all are not implemented as actual REST methods because a duplicate is the result
 * of comparing an item with other indexed items, not an object that can be referenced by some kind of ID, but
 * we must at least implement the Java methods here in order to extend DSpaceRestRepository and implement
 * SearchRestMethods.
 *
 * @author Kim Shepherd
 */
@ConditionalOnProperty("duplicate.enable")
@Component(PotentialDuplicateRest.CATEGORY + "." + PotentialDuplicateRest.NAME)
public class DuplicateRestRepository extends DSpaceRestRepository<PotentialDuplicateRest, String>
        implements InitializingBean {

    /**
     * Discoverable endpoints service
     */
    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    /**
     * Duplicate detection service
     */
    @Autowired
    DuplicateDetectionService duplicateDetectionService;

    /**
     * Item service
     */
    @Autowired
    ItemService itemService;

    /**
     * Logger
     */
    private final static Logger log = LogManager.getLogger();

    /**
     * Register this repository endpoint as /api/submission/duplicates
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
                .register(this, Arrays.asList(Link.of(
                        "/api/" + PotentialDuplicateRest.CATEGORY + "/" + PotentialDuplicateRest.NAME + "/search",
                        PotentialDuplicateRest.NAME + "-search")));
    }

    /**
     * This REST method is NOT IMPLEMENTED - it does not make sense in duplicate detection, in which the only
     * real addressable objects involved are Items.
     *
     * @param context
     *            the dspace context
     * @param name
     *            the rest object id
     * @return not implemented
     * @throws RepositoryMethodNotImplementedException
     */
    @PreAuthorize("permitAll()")
    @Override
    public PotentialDuplicateRest findOne(Context context, String name) {
        throw new RepositoryMethodNotImplementedException("Duplicate detection endpoint only implements searchBy", "");
    }

    /**
     * This REST method is NOT IMPLEMENTED - it does not make sense in duplicate detection, where there can be no "all"
     *
     * @param context
     *            the dspace context
     * @return not implemented
     * @throws RepositoryMethodNotImplementedException
     */
    @PreAuthorize("permitAll()")
    @Override
    public Page<PotentialDuplicateRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("Duplicate detection endpoint only implements searchBy", "");
    }

    /**
     * Return a paged list of potential duplicate matches for the given item ID. This may be an item wrapped in
     * an in-progress item wrapper like workspace or workflow, as long as the current user has READ access to this item.
     * Results from the service search method will only contain matches that lead to items which are readable by
     * the current user.
     *
     * @param uuid The item UUID to search
     * @param pageable Pagination options
     * @return Paged list of potential duplicates
     * @throws Exception
     */
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @SearchRestMethod(name = "findByItem")
    public Page<PotentialDuplicateRest> findByItem(@Parameter(value = "uuid", required = true) UUID uuid,
                                                       Pageable pageable) {
        // Instantiate object to represent this item
        Item item;
        // Instantiate list of potential duplicates which we will convert and return as paged ItemRest list
        List<PotentialDuplicate> potentialDuplicates = new LinkedList<>();
        // Instantiate total count
        int total = 0;
        // Obtain context
        Context context = ContextUtil.obtainCurrentRequestContext();

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

        // Construct rest pages and return
        Page<PotentialDuplicateRest> restPage = converter.toRestPage(potentialDuplicates, pageable, total,
                utils.obtainProjection());

        return restPage;

    }

    /**
     * Return the domain class for potential duplicate objects
     * @return PotentialDuplicateRest.class
     */
    @Override
    public Class<PotentialDuplicateRest> getDomainClass() {
        return PotentialDuplicateRest.class;
    }

}
