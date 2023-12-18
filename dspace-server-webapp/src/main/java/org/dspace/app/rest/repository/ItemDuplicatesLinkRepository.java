/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.PotentialDuplicateRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.content.Item;
import org.dspace.content.service.DuplicateDetectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.virtual.PotentialDuplicate;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Item link repository to allow a list of potential duplicates to be searched and returned
 * for the parent item.
 * Used by, e.g. workflow pooled/claimed task page and previews, to show reviewers about potential duplicates
 * @see DuplicateDetectionService
 *
 * @author Kim Shepherd
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.DUPLICATES)
public class ItemDuplicatesLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    private static final Logger log = LogManager.getLogger(ItemDuplicatesLinkRepository.class);

    @Autowired
    ItemService itemService;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    SubmissionService submissionService;
    @Autowired
    DuplicateDetectionService duplicateDetectionService;

    /**
     * Get a list of potential duplicates based on the current item's "signature" (e.g. title)
     *
     * @param request
     * @param itemId
     * @param optionalPageable
     * @param projection
     * @return
     */
    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public Page<PotentialDuplicateRest> getDuplicates(@Nullable HttpServletRequest request,
                                        UUID itemId,
                                        @Nullable Pageable optionalPageable,
                                        Projection projection) {


        // Instantiate object to represent this item
        Item item;
        // Instantiate list of potential duplicates which we will convert and return as paged ItemRest list
        List<PotentialDuplicate> potentialDuplicates = new LinkedList<>();
        // Instantiate total count
        int total = 0;
        // Obtain context
        Context context = obtainContext();
        // Get pagination
        Pageable pageable = utils.getPageable(optionalPageable);

        // Try to get item based on UUID parameter
        try {
            item = itemService.find(context, itemId);
        } catch (SQLException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }

        // If the item is null or otherwise invalid (template, etc) then throw an appropriate error
        if (item == null) {
            throw new ResourceNotFoundException("No such item: " + itemId);
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

        // Return the list of items along with pagination info and max results
        return converter.toRestPage(potentialDuplicates, pageable, total, projection);
    }

}
