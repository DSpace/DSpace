/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.dspace.app.itemupdate.MetadataUtilities;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.DuplicateDetectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.virtual.PotentialDuplicate;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of DuplicateDetectionService.
 * Duplicate Detection Service handles get, search and validation operations for duplicate detection.
 *
 * @author Kim Shepherd
 */
public class DuplicateDetectionServiceImpl implements DuplicateDetectionService {

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    VersionHistoryService versionHistoryService;
    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    GroupService groupService;
    @Autowired
    MetadataFieldService metadataFieldService;
    @Autowired
    MetadataValueService metadataValueService;
    @Autowired
    XmlWorkflowItemService workflowItemService;
    @Autowired
    WorkspaceItemService workspaceItemService;
    @Autowired
    ItemService itemService;

    /**
     * Get a list of PotentialDuplicate objects (wrappers with some metadata included for previewing) that
     * are identified as potential duplicates of the given item
     *
     * @param context DSpace context
     * @param item    Item to check
     * @return        List of potential duplicates (empty if none found)
     * @throws SearchServiceException if an error occurs performing the discovery search
     */
    @Override
    public List<PotentialDuplicate> getPotentialDuplicates(Context context, Item item)
            throws SearchServiceException {
        // Instantiate a new list of potential duplicates
        List<PotentialDuplicate> potentialDuplicates = new LinkedList<>();

        // Immediately return an empty if this feature is not configured
        if (!configurationService.getBooleanProperty("duplicate.enable", false)) {
            return potentialDuplicates;
        }

        // Search duplicates of this item and get discovery search result
        DiscoverResult discoverResult = searchDuplicates(context, item);

        // If the search result is valid, iterate results and validate / transform
        if (discoverResult != null) {
            for (IndexableObject result : discoverResult.getIndexableObjects()) {
                if (result != null) {
                    try {
                        // Validate this result and check permissions to read the item
                        Optional<PotentialDuplicate> potentialDuplicateOptional =
                                validateDuplicateResult(context, result, item);
                        if (potentialDuplicateOptional.isPresent()) {
                            // Add the potential duplicate to the list
                            potentialDuplicates.add(potentialDuplicateOptional.get());
                        }
                    } catch (SQLException e) {
                        log.error("SQL Error obtaining duplicate result: " + e.getMessage());
                    } catch (AuthorizeException e) {
                        log.error("Authorize Error obtaining duplicate result: " + e.getMessage());
                    }
                }
            }
        }

        // Return the list of potential duplicates
        return potentialDuplicates;
    }



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
    @Override
    public Optional<PotentialDuplicate> validateDuplicateResult(Context context, IndexableObject indexableObject,
                                                                Item original)
            throws SQLException,
            AuthorizeException {

        Item resultItem = null;
        PotentialDuplicate potentialDuplicate = null;
        WorkspaceItem workspaceItem = null;
        WorkflowItem workflowItem = null;

        // Inspect the indexable object, and extract the DSpace item depending on
        // what submission / archived state it is in
        if (indexableObject instanceof IndexableWorkspaceItem) {
            workspaceItem = ((IndexableWorkspaceItem) indexableObject).getIndexedObject();
            // Only process workspace items that belong to the submitter
            if (workspaceItem != null && workspaceItem.getSubmitter() != null
                    && workspaceItem.getSubmitter().equals(context.getCurrentUser())) {
                resultItem = workspaceItem.getItem();
            }
        }
        if (indexableObject instanceof IndexableWorkflowItem) {
            workflowItem = ((IndexableWorkflowItem) indexableObject).getIndexedObject();
            if (workflowItem != null) {
                resultItem = workflowItem.getItem();
            }
        }
        if (indexableObject instanceof IndexableItem) {
            resultItem = ((IndexableItem) indexableObject).getIndexedObject();
            // Attempt resolution of workflow or workspace items, tested later
            workflowItem = workflowItemService.findByItem(context, resultItem);
            workspaceItem = workspaceItemService.findByItem(context, resultItem);
        }

        // Result item must not be null, a template item, or actually identical to the original
        if (resultItem == null) {
            log.warn("skipping null item in duplicate search results");
            return Optional.empty();
        } else if (resultItem.getTemplateItemOf() != null) {
            log.info("skipping template item in duplicate search results, item={}", resultItem.getID());
            return Optional.empty();
        } else if (resultItem.getID().equals(original.getID())) {
            log.info("skipping a duplicate search result for the original item", resultItem.getID());
            return Optional.empty();
        }

        // If our item and the duplicate candidate share the same versionHistory, they are two different
        // versions of the same item.
        VersionHistory versionHistory = versionHistoryService.findByItem(context, original);
        VersionHistory candiateVersionHistory = versionHistoryService.findByItem(context, resultItem);
        // if the versionHistory is null, either versioning is switched off or the item doesn't have
        // multiple versions
        if (versionHistory != null && versionHistory.equals(candiateVersionHistory)) {
            log.warn("skipping item that is just another version of this item");
            return Optional.empty();
        }

        // Construct new potential duplicate object
        potentialDuplicate = new PotentialDuplicate(resultItem);

        // Get configured list of metadata fields to copy
        List<String> fields = new ArrayList<>(Arrays.asList(
                configurationService.getArrayProperty("duplicate.preview.metadata.field", new String[]{})));

        // Get item metadata and if it's configured for mapping, copy it across to the potential duplicate object
        List<MetadataValue> metadata = resultItem.getCachedMetadata();

        // Prepare a map of metadata to set on the potential duplicate object
        for (MetadataValue metadatum : metadata) {
            String fieldName = metadatum.getMetadataField().toString('.');
            if (fields.contains(fieldName)) {
                potentialDuplicate.getMetadataValueList().add(metadatum);
            }
        }

        // Only if the current user is also the submitter of the item will we add this information
        if (workspaceItem != null && workspaceItem.getSubmitter() != null
                && workspaceItem.getSubmitter().equals(context.getCurrentUser())) {
            potentialDuplicate.setWorkspaceItemId(workspaceItem.getID());
            return Optional.of(potentialDuplicate);
        }

        // More authorisation checks
        if (workflowItem != null) {
            Collection c = workflowItem.getCollection();
            if (groupService.isMember(context, context.getCurrentUser(), c.getWorkflowStep1(context)) ||
                    groupService.isMember(context, context.getCurrentUser(), c.getWorkflowStep2(context)) ||
                    groupService.isMember(context, context.getCurrentUser(), c.getWorkflowStep3(context))) {
                // Current user is a member of one of the workflow role groups
                potentialDuplicate.setWorkflowItemId(workflowItem.getID());
                return Optional.of(potentialDuplicate);
            }
        } else if (resultItem.isArchived() && !resultItem.isWithdrawn() && resultItem.isDiscoverable()) {
            // Not a workspace or workflow item, but is it archived, not withdrawn, and discoverable?
            // Is it readable by the current user?
            if (authorizeService.authorizeActionBoolean(context, resultItem, Constants.READ)) {
                return Optional.of(potentialDuplicate);
            }
        } else if (authorizeService.isAdmin(context, resultItem)) {
            // Admins can always read, return immediately
            return Optional.of(potentialDuplicate);
        } else {
            log.info("Potential duplicate result is not readable by the current user, skipping item={}",
                    potentialDuplicate.getUuid());
        }

            // By default, return an empty result
        return Optional.empty();
    }

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
    @Override
    public DiscoverResult searchDuplicates(Context context, Item item) throws SearchServiceException {

        // If the item is null or otherwise invalid (template, etc) then throw an appropriate error
        if (item == null) {
            throw new ResourceNotFoundException("Duplicate search error: item is null");
        }
        if (item.getTemplateItemOf() != null) {
            throw new IllegalArgumentException("Cannot get duplicates for template item");
        }

        // Build normalised comparison value
        String comparisonValue = buildComparisonValue(context, item);

        // Construct query
        if (StringUtils.isNotBlank(comparisonValue)) {
            // Get search service
            SearchService searchService = SearchUtils.getSearchService();

            // Escape reserved solr characters
            comparisonValue = searchService.escapeQueryChars(comparisonValue);

            // Construct discovery query based on comparison value
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setQuery("(" + configurationService.getProperty("duplicate.comparison.solr.field",
                    "deduplication_keyword") + ":" + comparisonValue + "~" +
                    configurationService.getIntProperty("duplicate.comparison.distance", 0) + ")");
            // Add filter queries for the resource type
            discoverQuery.addFilterQueries("(search.resourcetype:Item OR " +
                    "search.resourcetype:WorkspaceItem OR " +
                    "search.resourcetype:XmlWorkflowItem OR search.resourcetype:WorkflowItem)");
            // Skip this item itself so it isn't a false positive
            discoverQuery.addFilterQueries("-search.resourceid:" + item.getID());

            // Perform search and populate list with results, update total count integer
            return searchService.search(context, discoverQuery);
        } else {
            log.warn("empty item comparison value, ignoring for duplicate search");
        }

        // Return null by default
        return null;

    }

    /**
     * Build a comparison value string made up of values of configured fields, used when indexing and querying
     * items for deduplication
     * @param context DSpace context
     * @param item The DSpace item
     * @return a constructed, normalised string
     */
    @Override
    public String buildComparisonValue(Context context, Item item) {
        // Get configured fields to use for comparison values
        String[] comparisonFields = configurationService.getArrayProperty("duplicate.comparison.metadata.field",
                new String[]{"dc.title"});
        // Get all values, in order, for these fields
        StringBuilder comparisonValueBuilder = new StringBuilder();
        String comparisonValue = null;
        for (String field : comparisonFields) {
            try {
                // Get field components
                String[] fieldParts = MetadataUtilities.parseCompoundForm(field);
                // Get all values of this field
                List<MetadataValue> metadataValues = itemService.getMetadata(item,
                        fieldParts[0], fieldParts[1], (fieldParts.length > 2 ? fieldParts[2] : null), Item.ANY);
                // Sort metadata values by text value, so their 'position' in db doesn't matter for dedupe purposes
                metadataValues.sort(comparing(MetadataValue::getValue, naturalOrder()));
                for (MetadataValue metadataValue : metadataValues) {
                    // Add each found value to the string builder (null values interpreted as empty)
                    if (metadataValue != null) {
                        comparisonValueBuilder.append(metadataValue.getValue());
                    }
                }
            } catch (ParseException e) {
                // Log error and continue processing
                log.error("Error parsing configured field for deduplication comparison: item={}, field={}",
                        item.getID(), field);
            } catch (NullPointerException e) {
                log.error("Null pointer encountered, probably during metadata value sort, when deduping:" +
                        "item={}, field={}", item.getID(), field);
            }
        }

        // Build string
        comparisonValue = comparisonValueBuilder.toString();

        // Normalise according to configuration
        if (!StringUtils.isBlank(comparisonValue)) {
            if (configurationService.getBooleanProperty("duplicate.comparison.normalise.lowercase")) {
                comparisonValue = comparisonValue.toLowerCase(context.getCurrentLocale());
            }
            if (configurationService.getBooleanProperty("duplicate.comparison.normalise.whitespace")) {
                comparisonValue = comparisonValue.replaceAll("\\s+", "");
            }
        }

        // Return comparison value
        return comparisonValue;
    }

}
