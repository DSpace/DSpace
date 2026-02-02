/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DuplicateDetectionServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.virtual.PotentialDuplicate;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;

/**
 * Duplicate Detection Service handles get, search and validation operations for duplicate detection.
 * @see DuplicateDetectionServiceImpl for implementation details
 *
 * @author Kim Shepherd
 */
public interface DuplicateDetectionService {

    /**
     * Logger
     */
    Logger log = LogManager.getLogger(DuplicateDetectionService.class);

    /**
     * Get a list of PotentialDuplicate objects (wrappers with some metadata included for previewing) that
     * are identified as potential duplicates of the given item
     *
     * @param context DSpace context
     * @param item    Item to check
     * @return        List of potential duplicates (empty if none found)
     * @throws SearchServiceException if an error occurs performing the discovery search
     */
    List<PotentialDuplicate> getPotentialDuplicates(Context context, Item item)
            throws SearchServiceException;

    /**
     * Validate an indexable object (returned by discovery search) to ensure it is permissible, readable and valid
     * and can be added to a list of results.
     * An Optional is returned, if it is empty then it was invalid or did not pass validation.
     *
     * @param context The DSpace context
     * @param indexableObject The discovery search result
     * @param original The original item (to compare IDs, submitters, etc)
     * @return An Optional potential duplicate
     * @throws SQLException
     * @throws AuthorizeException
     */
    Optional<PotentialDuplicate> validateDuplicateResult(Context context, IndexableObject indexableObject,
                                                                Item original) throws SQLException, AuthorizeException;

    /**
     * Search discovery for potential duplicates of a given item. The search uses levenshtein distance (configurable)
     * and a single-term "comparison value" constructed out of the item title
     *
     * @param context DSpace context
     * @param item The item to check
     * @return DiscoverResult as a result of performing search. Null if invalid.
     *
     * @throws SearchServiceException if an error was encountered during the discovery search itself.
     */
    DiscoverResult searchDuplicates(Context context, Item item) throws SearchServiceException;

    /**
     * Build a comparison value string made up of values of configured fields, used when indexing and querying
     * items for deduplication
     * @param context DSpace context
     * @param item The DSpace item
     * @return a constructed, normalised string
     */
    String buildComparisonValue(Context context, Item item);
}
