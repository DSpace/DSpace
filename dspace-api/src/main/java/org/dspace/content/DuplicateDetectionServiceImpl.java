/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.dspace.app.itemupdate.MetadataUtilities;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.duplicatedetection.DuplicateComparison;
import org.dspace.content.duplicatedetection.DuplicateComparisonValueTransformer;
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
    @Autowired
    SearchService searchService;
    Map<String, DuplicateComparisonValueTransformer> duplicateComparisonValueTransformers;

    /**
     * Initializes the DuplicateDetectionServiceImpl instance by ensuring the
     * internal map for duplicate comparison value transformers is properly
     * instantiated. This method is annotated with @PostConstruct, indicating
     * that it will be invoked after the dependency injection is complete.
     *
     * If the field 'duplicateComparisonValueTransformers' is null during
     * initialization, it will be initialized to an empty HashMap.
     */
    @PostConstruct
    public void init() {
        if (duplicateComparisonValueTransformers == null) {
            duplicateComparisonValueTransformers = new HashMap<>();
        }
    }

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
     * @param context         The DSpace context
     * @param indexableObject The discovery search result
     * @param original        The original item (to compare IDs, submitters, etc)
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
     * @param item    The item to check
     * @return DiscoverResult as a result of performing search. Null if invalid.
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

        // Build normalised comparison value groups
        List<List<DuplicateComparison>> groupedComparisonValues = buildComparisonValueGroups(context, item);

        // Construct query
        if (groupedComparisonValues != null && !groupedComparisonValues.isEmpty()) {
            List<String> queryGroups = new ArrayList<>();
            // iterate over each group (groups will be combined using OR)
            for (List<DuplicateComparison> comparisonValues : groupedComparisonValues) {
                if (comparisonValues != null && !comparisonValues.isEmpty()) {
                    // get a map of DuplicateComparison objects grouped by fieldName
                    Map<String, List<DuplicateComparison>> queryComparisonMap =
                        comparisonValues.stream()
                            .collect(
                                Collectors.groupingBy(DuplicateComparison::fieldName, TreeMap::new,
                                    Collectors.toList()));

                    // iterate over DuplicateComparison objects grouped by fieldName (combined using OR)
                    List<String> queryParts = queryComparisonMap.values().stream()
                        .map(duplicateComparisons -> "(" + duplicateComparisons.stream()
                            .map(comparisonValue ->
                                DuplicateComparison.getSolrFieldPrefix(configurationService, comparisonValue) + ":" +
                                    searchService.escapeQueryChars(comparisonValue.value()) +
                                    "~" + comparisonValue.distance())
                            .collect(Collectors.joining(" OR ")) + ")")
                        .toList();
                    // combine fieldName based subqueries using AND
                    queryGroups.add("(" + StringUtils.join(queryParts.iterator(), " AND ") + ")");
                }
            }
            // Combine the query groups
            String keywordQuery = StringUtils.join(queryGroups.iterator(), " OR ");
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setQuery(keywordQuery);
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
     *
     * @param context DSpace context
     * @param item    The DSpace item
     * @return a list of lists of DuplicateComparison objects, each list representing a group of comparison values
     */
    @Override
    public List<List<DuplicateComparison>> buildComparisonValueGroups(Context context, Item item) {
        // Get configured fields to use for comparison values
        List<List<String>> comparisonFieldGroups = new ArrayList<>();
        comparisonFieldGroups.add(Arrays.asList(
            configurationService.getArrayProperty("duplicate.comparison.metadata.field", new String[] {"dc.title"})));
        int groupNumber = 1;
        while (configurationService.hasProperty("duplicate.comparison.metadata.field." + groupNumber)) {
            comparisonFieldGroups.add(Arrays.asList(
                configurationService.getArrayProperty("duplicate.comparison.metadata.field." + groupNumber)));
            groupNumber++;
        }
        int comparisonDistance = configurationService.getIntProperty("duplicate.comparison.distance", 0);
        List<List<DuplicateComparison>> groupedComparisonValues = new ArrayList<>();
        for (int i = 0; i < comparisonFieldGroups.size(); i++) {
            // Get all values, in order, for these fields
            List<DuplicateComparison> comparisonValues = new ArrayList<>();
            for (String field : comparisonFieldGroups.get(i)) {
                try {
                    // Get field components
                    String[] fieldDefinitions = field.split(":");
                    String fieldName = fieldDefinitions[0];
                    // use the field comparison distance if provided,
                    // otherwise use the setting from 'duplicate.comparison.distance'
                    int fieldComparisonDistance =
                        fieldDefinitions.length > 1 ? Integer.parseInt(fieldDefinitions[1]) : comparisonDistance;
                    String[] fieldParts = MetadataUtilities.parseCompoundForm(fieldDefinitions[0]);

                    // Get all values of this field
                    List<MetadataValue> metadataValues = itemService.getMetadata(item,
                        fieldParts[0], fieldParts[1], (fieldParts.length > 2 ? fieldParts[2] : null), Item.ANY);
                    // Sort metadata values by text value, so their 'position' in db doesn't matter for dedupe purposes
                    List<String> values = metadataValues.stream().map(metadataValue -> {
                        // Apply value modification in case it's configured for the current metadata field
                        if (duplicateComparisonValueTransformers.containsKey(field)) {
                            return duplicateComparisonValueTransformers.get(field).transform(metadataValue.getValue());
                        }
                        return metadataValue.getValue();
                    }).sorted().toList();
                    for (String value : values) {
                        // Add each found value to the list as a DuplicateComparison record
                        // (null and empty values are skipped)
                        if (StringUtils.isNotBlank(value)) {
                            // Normalise according to configuration
                            if (configurationService.getBooleanProperty(
                                "duplicate.comparison.normalise.lowercase")) {
                                value = value.toLowerCase(context.getCurrentLocale());
                            }
                            if (configurationService.getBooleanProperty(
                                "duplicate.comparison.normalise.whitespace")) {
                                value = value.replaceAll("\\s+", "");
                            }
                            DuplicateComparison comparisonValue =
                                new DuplicateComparison(fieldName, value, fieldComparisonDistance);
                            comparisonValues.add(comparisonValue);
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
            groupedComparisonValues.add(comparisonValues);
        }
        return groupedComparisonValues;
    }

    /**
     * Sets the map of duplicate comparison value transformers, which are responsible for
     * transforming comparison values during the duplicate detection process. Each entry
     * in the map associates a string key with a specific {@link DuplicateComparisonValueTransformer},
     * defining how certain fields' values should be transformed for comparison purposes.
     *
     * @param duplicateComparisonValueTransformers a map where the keys are strings
     *        representing field identifiers or transformation contexts, and the values
     *        are instances of {@link DuplicateComparisonValueTransformer} that handle
     *        the transformation logic for each specified field or context
     */
    public void setDuplicateComparisonValueTransformers(
        Map<String, DuplicateComparisonValueTransformer> duplicateComparisonValueTransformers) {
        this.duplicateComparisonValueTransformers = duplicateComparisonValueTransformers;
    }
}
