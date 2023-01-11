/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.FullTextContentStreams;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.configuration.DiscoveryMoreLikeThisConfiguration;
import org.dspace.discovery.configuration.DiscoveryRecentSubmissionsConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.HierarchicalSidebarFacetConfiguration;
import org.dspace.discovery.indexobject.factory.ItemIndexFactory;
import org.dspace.discovery.indexobject.factory.WorkflowItemIndexFactory;
import org.dspace.discovery.indexobject.factory.WorkspaceItemIndexFactory;
import org.dspace.eperson.EPerson;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.util.SolrUtils;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving items in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ItemIndexFactoryImpl extends DSpaceObjectIndexFactoryImpl<IndexableItem, Item>
        implements ItemIndexFactory {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemIndexFactoryImpl.class);
    public static final String VARIANTS_STORE_SEPARATOR = "###";
    public static final String STORE_SEPARATOR = "\n|||\n";
    public static final String STATUS_FIELD = "database_status";
    public static final String STATUS_FIELD_PREDB = "predb";


    @Autowired
    protected HandleService handleService;
    @Autowired
    protected ItemService itemService;
    @Autowired(required = true)
    protected ChoiceAuthorityService choiceAuthorityService;
    @Autowired
    protected MetadataAuthorityService metadataAuthorityService;
    @Autowired
    protected WorkspaceItemService workspaceItemService;
    @Autowired
    protected XmlWorkflowItemService xmlWorkflowItemService;
    @Autowired
    protected WorkflowItemIndexFactory workflowItemIndexFactory;
    @Autowired
    protected WorkspaceItemIndexFactory workspaceItemIndexFactory;
    @Autowired
    protected VersionHistoryService versionHistoryService;


    @Override
    public Iterator<IndexableItem> findAll(Context context) throws SQLException {
        Iterator<Item> items = itemService.findAllRegularItems(context);
        return new Iterator<IndexableItem>() {
            @Override
            public boolean hasNext() {
                return items.hasNext();
            }

            @Override
            public IndexableItem next() {
                return new IndexableItem(items.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableItem.TYPE;
    }

    /**
     * Build a Solr document for a DSpace Item and write the index
     *
     * @param context       Users Context
     * @param indexableItem The IndexableItem Item to be indexed
     * @throws SQLException if database error
     * @throws IOException  if IO error
     */
    @Override
    public SolrInputDocument buildDocument(Context context, IndexableItem indexableItem)
            throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        SolrInputDocument doc = super.buildDocument(context, indexableItem);

        final Item item = indexableItem.getIndexedObject();

        doc.addField("archived", item.isArchived());
        doc.addField("withdrawn", item.isWithdrawn());
        doc.addField("discoverable", item.isDiscoverable());
        doc.addField("lastModified", SolrUtils.getDateFormatter().format(item.getLastModified()));
        doc.addField("latestVersion", isLatestVersion(context, item));

        EPerson submitter = item.getSubmitter();
        if (submitter != null) {
            addFacetIndex(doc, "submitter", submitter.getID().toString(),
                    submitter.getFullName());
        }

        // Add the item metadata
        List<DiscoveryConfiguration> discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations(item);
        addDiscoveryFields(doc, context, indexableItem.getIndexedObject(), discoveryConfigurations);

        //mandatory facet to show status on mydspace
        final String typeText = StringUtils.deleteWhitespace(indexableItem.getTypeText().toLowerCase());
        String acvalue = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(
                "discovery.facet.namedtype." + typeText,
                typeText + SearchUtils.AUTHORITY_SEPARATOR + typeText);
        if (StringUtils.isNotBlank(acvalue)) {
            addNamedResourceTypeIndex(doc, acvalue);
        }

        // write the index and close the inputstreamreaders
        try {
            log.info("Wrote Item: " + item.getID() + " to Index");
        } catch (RuntimeException e) {
            log.error("Error while writing item to discovery index: " + item.getID() + " message:"
                    + e.getMessage(), e);
        }
        return doc;
    }

    /**
     * Check whether the given item is the latest version.
     * If the latest item cannot be determined, because either the version history or the latest version is not present,
     * assume the item is latest.
     * @param context the DSpace context.
     * @param item the item that should be checked.
     * @return true if the item is the latest version, false otherwise.
     */
    protected boolean isLatestVersion(Context context, Item item) throws SQLException {
        VersionHistory history = versionHistoryService.findByItem(context, item);
        if (history == null) {
            // not all items have a version history
            // if an item does not have a version history, it is by definition the latest version
            return true;
        }

        // start with the very latest version of the given item (may still be in workspace)
        Version latestVersion = versionHistoryService.getLatestVersion(context, history);

        // find the latest version of the given item that is archived
        while (latestVersion != null && !latestVersion.getItem().isArchived()) {
            latestVersion = versionHistoryService.getPrevious(context, history, latestVersion);
        }

        // could not find an archived version of the given item
        if (latestVersion == null) {
            // this scenario should never happen, but let's err on the side of showing too many items vs. to little
            // (see discovery.xml, a lot of discovery configs filter out all items that are not the latest version)
            return true;
        }

        // sanity check
        assert latestVersion.getItem().isArchived();

        return item.equals(latestVersion.getItem());
    }

    @Override
    public SolrInputDocument buildNewDocument(Context context, IndexableItem indexableItem)
            throws SQLException, IOException {
        SolrInputDocument doc = buildDocument(context, indexableItem);
        doc.addField(STATUS_FIELD, STATUS_FIELD_PREDB);
        return doc;
    }

    @Override
    public void addDiscoveryFields(SolrInputDocument doc, Context context, Item item,
                                   List<DiscoveryConfiguration> discoveryConfigurations)
            throws SQLException, IOException {
        // use the item service to retrieve the owning collection also for inprogress submission
        Collection collection = (Collection) itemService.getParentObject(context, item);
        //Keep a list of our sort values which we added, sort values can only be added once
        List<String> sortFieldsAdded = new ArrayList<>();
        Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<>();
        Set<String> hitHighlightingFields = new HashSet<>();
        try {
            //A map used to save each sidebarFacet config by the metadata fields
            Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<>();
            Map<String, DiscoveryRecentSubmissionsConfiguration> recentSubmissionsConfigurationMap = new
                    HashMap<>();
            Set<String> moreLikeThisFields = new HashSet<>();
            // some configuration are returned multiple times, skip them to save CPU cycles
            Set<String> appliedConf = new HashSet<>();
            // it is common to have search filter shared between multiple configurations
            Set<String> appliedDiscoverySearchFilter = new HashSet<>();

            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations) {
                if (appliedConf.contains(discoveryConfiguration.getId())) {
                    continue;
                } else {
                    appliedConf.add(discoveryConfiguration.getId());
                }
                for (int i = 0; i < discoveryConfiguration.getSearchFilters().size(); i++) {
                    if (appliedDiscoverySearchFilter
                            .contains(discoveryConfiguration.getSearchFilters().get(i).getIndexFieldName())) {
                        continue;
                    } else {
                        appliedDiscoverySearchFilter
                                .add(discoveryConfiguration.getSearchFilters().get(i).getIndexFieldName());
                    }
                    List<MetadataValue> metadataValueList = new LinkedList<>();
                    boolean shouldExposeMinMax = false;
                    DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration.getSearchFilters().get(i);
                    if (StringUtils.equalsIgnoreCase(discoverySearchFilter.getFilterType(), "facet")) {
                        if (((DiscoverySearchFilterFacet) discoverySearchFilter).exposeMinAndMaxValue()) {
                            shouldExposeMinMax = true;
                        }
                    }
                    for (int j = 0; j < discoverySearchFilter.getMetadataFields().size(); j++) {
                        String metadataField = discoverySearchFilter.getMetadataFields().get(j);
                        List<DiscoverySearchFilter> resultingList;
                        if (searchFilters.get(metadataField) != null) {
                            resultingList = searchFilters.get(metadataField);
                        } else {
                            //New metadata field, create a new list for it
                            resultingList = new ArrayList<>();
                        }


                        if (shouldExposeMinMax) {
                            String[] splittedMetadataField = metadataField.split("\\.");
                            String schema = splittedMetadataField[0];
                            String element = splittedMetadataField.length > 1 ? splittedMetadataField[1] : null;
                            String qualifier = splittedMetadataField.length > 2 ? splittedMetadataField[2] : null;

                            metadataValueList.addAll(itemService.getMetadata(item, schema,
                                    element, qualifier, Item.ANY));

                        }

                        resultingList.add(discoverySearchFilter);

                        searchFilters.put(metadataField, resultingList);
                    }

                    if (!metadataValueList.isEmpty() && shouldExposeMinMax) {
                        metadataValueList.sort((mdv1, mdv2) -> mdv1.getValue().compareTo(mdv2.getValue()));
                        MetadataValue firstMetadataValue = metadataValueList.get(0);
                        MetadataValue lastMetadataValue = metadataValueList.get(metadataValueList.size() - 1);

                        doc.addField(discoverySearchFilter.getIndexFieldName() + "_min", firstMetadataValue.getValue());
                        doc.addField(discoverySearchFilter.getIndexFieldName()
                                + "_min_sort", firstMetadataValue.getValue());
                        doc.addField(discoverySearchFilter.getIndexFieldName() + "_max", lastMetadataValue.getValue());
                        doc.addField(discoverySearchFilter.getIndexFieldName()
                                + "_max_sort", lastMetadataValue.getValue());

                    }
                }

                DiscoverySortConfiguration sortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
                if (sortConfiguration != null) {
                    for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration
                            .getSortFields()) {
                        sortFields.put(discoverySortConfiguration.getMetadataField(), discoverySortConfiguration);
                    }
                }

                DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration
                        .getRecentSubmissionConfiguration();
                if (recentSubmissionConfiguration != null) {
                    recentSubmissionsConfigurationMap
                            .put(recentSubmissionConfiguration.getMetadataSortField(), recentSubmissionConfiguration);
                }

                DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration = discoveryConfiguration
                        .getHitHighlightingConfiguration();
                if (hitHighlightingConfiguration != null) {
                    List<DiscoveryHitHighlightFieldConfiguration> fieldConfigurations = hitHighlightingConfiguration
                            .getMetadataFields();
                    for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : fieldConfigurations) {
                        hitHighlightingFields.add(fieldConfiguration.getField());
                    }
                }
                DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration = discoveryConfiguration
                        .getMoreLikeThisConfiguration();
                if (moreLikeThisConfiguration != null) {
                    for (String metadataField : moreLikeThisConfiguration.getSimilarityMetadataFields()) {
                        moreLikeThisFields.add(metadataField);
                    }
                }
            }


            List<String> toProjectionFields = new ArrayList<>();
            String[] projectionFields = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getArrayProperty("discovery.index.projection");
            if (projectionFields != null) {
                for (String field : projectionFields) {
                    toProjectionFields.add(field.trim());
                }
            }

            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(item.getType());
            List<MetadataValue> mydc = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (MetadataValue meta : mydc) {
                MetadataField metadataField = meta.getMetadataField();
                MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                String field = metadataSchema.getName() + "." + metadataField.getElement();
                String unqualifiedField = field;

                String value = meta.getValue();

                if (value == null) {
                    continue;
                }

                if (metadataField.getQualifier() != null && !metadataField.getQualifier().trim().equals("")) {
                    field += "." + metadataField.getQualifier();
                }

                //We are not indexing provenance, this is useless
                if (toIgnoreMetadataFields != null && (toIgnoreMetadataFields.contains(field) || toIgnoreMetadataFields
                        .contains(unqualifiedField + "." + Item.ANY))) {
                    continue;
                }

                String authority = null;
                String preferedLabel = null;
                List<String> variants = null;
                boolean isAuthorityControlled = metadataAuthorityService
                        .isAuthorityControlled(metadataField);

                int minConfidence = isAuthorityControlled ? metadataAuthorityService
                        .getMinConfidence(metadataField) : Choices.CF_ACCEPTED;

                if (isAuthorityControlled && meta.getAuthority() != null
                        && meta.getConfidence() >= minConfidence) {
                    boolean ignoreAuthority =
                            DSpaceServicesFactory
                                    .getInstance()
                                    .getConfigurationService()
                                    .getPropertyAsType("discovery.index.authority.ignore." + field,
                                            DSpaceServicesFactory
                                                    .getInstance()
                                                    .getConfigurationService()
                                                    .getPropertyAsType(
                                                            "discovery.index.authority.ignore",
                                                            Boolean.FALSE),
                                            true);
                    if (!ignoreAuthority) {
                        authority = meta.getAuthority();

                        boolean ignorePrefered =
                                DSpaceServicesFactory
                                        .getInstance()
                                        .getConfigurationService()
                                        .getPropertyAsType("discovery.index.authority.ignore-prefered." + field,
                                                DSpaceServicesFactory
                                                        .getInstance()
                                                        .getConfigurationService()
                                                        .getPropertyAsType(
                                                                "discovery.index.authority.ignore-prefered",
                                                                Boolean.FALSE),
                                                true);

                        if (!ignorePrefered) {
                            try {
                                preferedLabel = choiceAuthorityService.getLabel(meta, collection, meta.getLanguage());
                            } catch (Exception e) {
                                log.warn("Failed to get preferred label for " + field, e);
                            }
                        }

                        boolean ignoreVariants =
                                DSpaceServicesFactory
                                        .getInstance()
                                        .getConfigurationService()
                                        .getPropertyAsType("discovery.index.authority.ignore-variants." + field,
                                                DSpaceServicesFactory
                                                        .getInstance()
                                                        .getConfigurationService()
                                                        .getPropertyAsType("discovery.index.authority.ignore-variants",
                                                                Boolean.FALSE),
                                                true);
                        if (!ignoreVariants) {
                            try {
                                variants = choiceAuthorityService
                                    .getVariants(meta, collection);
                            } catch (Exception e) {
                                log.warn("Failed to get variants for " + field, e);
                            }
                        }

                    }
                }

                if ((searchFilters.get(field) != null || searchFilters
                        .get(unqualifiedField + "." + Item.ANY) != null)) {
                    List<DiscoverySearchFilter> searchFilterConfigs = searchFilters.get(field);
                    if (searchFilterConfigs == null) {
                        searchFilterConfigs = searchFilters.get(unqualifiedField + "." + Item.ANY);
                    }

                    for (DiscoverySearchFilter searchFilter : searchFilterConfigs) {
                        Date date = null;
                        String separator = DSpaceServicesFactory.getInstance().getConfigurationService()
                                .getProperty("discovery.solr.facets.split.char");
                        if (separator == null) {
                            separator = SearchUtils.FILTER_SEPARATOR;
                        }
                        if (searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                            //For our search filters that are dates we format them properly
                            date = MultiFormatDateParser.parse(value);
                            if (date != null) {
                                //TODO: make this date format configurable !
                                value = DateFormatUtils.formatUTC(date, "yyyy-MM-dd");
                            }
                        }
                        doc.addField(searchFilter.getIndexFieldName(), value);
                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", value);

                        if (authority != null && preferedLabel == null) {
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", value + SearchUtils.AUTHORITY_SEPARATOR
                                    + authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_authority", authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_acid", value.toLowerCase()
                                    + separator + value
                                    + SearchUtils.AUTHORITY_SEPARATOR + authority);
                        }

                        if (preferedLabel != null) {
                            doc.addField(searchFilter.getIndexFieldName(),
                                    preferedLabel);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", preferedLabel);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", preferedLabel
                                    + SearchUtils.AUTHORITY_SEPARATOR + authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_authority", authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_acid", preferedLabel.toLowerCase()
                                    + separator + preferedLabel
                                    + SearchUtils.AUTHORITY_SEPARATOR + authority);
                        }
                        if (variants != null) {
                            for (String var : variants) {
                                doc.addField(searchFilter.getIndexFieldName() + "_keyword", var);
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_acid", var.toLowerCase()
                                        + separator + var
                                        + SearchUtils.AUTHORITY_SEPARATOR + authority);
                            }
                        }

                        //Add a dynamic fields for auto complete in search
                        doc.addField(searchFilter.getIndexFieldName() + "_ac",
                                value.toLowerCase() + separator + value);
                        if (preferedLabel != null) {
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_ac", preferedLabel.toLowerCase()
                                    + separator + preferedLabel);
                        }
                        if (variants != null) {
                            for (String var : variants) {
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_ac", var.toLowerCase() + separator
                                        + var);
                            }
                        }

                        if (searchFilter.getFilterType().equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET)) {
                            if (searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT)) {
                                //Add a special filter
                                //We use a separator to split up the lowercase and regular case, this is needed to
                                // get our filters in regular case
                                //Solr has issues with facet prefix and cases
                                if (authority != null) {
                                    String facetValue = preferedLabel != null ? preferedLabel : value;
                                    doc.addField(searchFilter.getIndexFieldName() + "_filter", facetValue
                                            .toLowerCase() + separator + facetValue + SearchUtils.AUTHORITY_SEPARATOR
                                            + authority);
                                } else {
                                    doc.addField(searchFilter.getIndexFieldName() + "_filter",
                                            value.toLowerCase() + separator + value);
                                }
                            } else if (searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                                if (date != null) {
                                    String indexField = searchFilter.getIndexFieldName() + ".year";
                                    String yearUTC = DateFormatUtils.formatUTC(date, "yyyy");
                                    doc.addField(searchFilter.getIndexFieldName() + "_keyword", yearUTC);
                                    // add the year to the autocomplete index
                                    doc.addField(searchFilter.getIndexFieldName() + "_ac", yearUTC);
                                    doc.addField(indexField, yearUTC);

                                    if (yearUTC.startsWith("0")) {
                                        doc.addField(
                                                searchFilter.getIndexFieldName()
                                                        + "_keyword",
                                                yearUTC.replaceFirst("0*", ""));
                                        // add date without starting zeros for autocomplete e filtering
                                        doc.addField(
                                                searchFilter.getIndexFieldName()
                                                        + "_ac",
                                                yearUTC.replaceFirst("0*", ""));
                                        doc.addField(
                                                searchFilter.getIndexFieldName()
                                                        + "_ac",
                                                value.replaceFirst("0*", ""));
                                        doc.addField(
                                                searchFilter.getIndexFieldName()
                                                        + "_keyword",
                                                value.replaceFirst("0*", ""));
                                    }

                                    //Also save a sort value of this year, this is required for determining the upper
                                    // & lower bound year of our facet
                                    if (doc.getField(indexField + "_sort") == null) {
                                        //We can only add one year so take the first one
                                        doc.addField(indexField + "_sort", yearUTC);
                                    }
                                }
                            } else if (searchFilter.getType()
                                    .equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)) {
                                HierarchicalSidebarFacetConfiguration hierarchicalSidebarFacetConfiguration =
                                        (HierarchicalSidebarFacetConfiguration) searchFilter;
                                String[] subValues = value.split(hierarchicalSidebarFacetConfiguration.getSplitter());
                                if (hierarchicalSidebarFacetConfiguration
                                        .isSkipFirstNodeLevel() && 1 < subValues.length) {
                                    //Remove the first element of our array
                                    subValues = (String[]) ArrayUtils.subarray(subValues, 1, subValues.length);
                                }
                                for (int i = 0; i < subValues.length; i++) {
                                    StringBuilder valueBuilder = new StringBuilder();
                                    for (int j = 0; j <= i; j++) {
                                        valueBuilder.append(subValues[j]);
                                        if (j < i) {
                                            valueBuilder.append(hierarchicalSidebarFacetConfiguration.getSplitter());
                                        }
                                    }

                                    String indexValue = valueBuilder.toString().trim();
                                    doc.addField(searchFilter.getIndexFieldName() + "_tax_" + i + "_filter",
                                            indexValue.toLowerCase() + separator + indexValue);
                                    //We add the field x times that it has occurred
                                    for (int j = i; j < subValues.length; j++) {
                                        doc.addField(searchFilter.getIndexFieldName() + "_filter",
                                                indexValue.toLowerCase() + separator + indexValue);
                                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", indexValue);
                                    }
                                }
                            }
                        }
                    }
                }

                if ((sortFields.get(field) != null || recentSubmissionsConfigurationMap
                        .get(field) != null) && !sortFieldsAdded.contains(field)) {
                    //Only add sort value once
                    String type;
                    if (sortFields.get(field) != null) {
                        type = sortFields.get(field).getType();
                    } else {
                        type = recentSubmissionsConfigurationMap.get(field).getType();
                    }

                    if (type.equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                        Date date = MultiFormatDateParser.parse(value);
                        if (date != null) {
                            String stringDate = SolrUtils.getDateFormatter().format(date);
                            doc.addField(field + "_dt", stringDate);
                        } else {
                            log.warn("Error while indexing sort date field, item: " + item
                                    .getHandle() + " metadata field: " + field + " date value: " + date);
                        }
                    } else {
                        doc.addField(field + "_sort", value);
                    }
                    sortFieldsAdded.add(field);
                }

                if (hitHighlightingFields.contains(field) || hitHighlightingFields
                        .contains("*") || hitHighlightingFields.contains(unqualifiedField + "." + Item.ANY)) {
                    if (authority != null) {
                        doc.addField(field + "_hl", value + SearchUtils.AUTHORITY_SEPARATOR + authority);
                    } else {
                        doc.addField(field + "_hl", value);
                    }
                }

                if (moreLikeThisFields.contains(field) || moreLikeThisFields
                        .contains(unqualifiedField + "." + Item.ANY)) {
                    doc.addField(field + "_mlt", value);
                }

                doc.addField(field, value);
                if (authority != null) {
                    doc.addField(field + "_authority", authority);
                }
                if (toProjectionFields.contains(field) || toProjectionFields
                        .contains(unqualifiedField + "." + Item.ANY)) {
                    StringBuffer variantsToStore = new StringBuffer();
                    if (variants != null) {
                        for (String var : variants) {
                            variantsToStore.append(VARIANTS_STORE_SEPARATOR);
                            variantsToStore.append(var);
                        }
                    }
                    doc.addField(
                            field + "_stored",
                            value + STORE_SEPARATOR + preferedLabel
                                    + STORE_SEPARATOR
                                    + (variantsToStore.length() > VARIANTS_STORE_SEPARATOR
                                    .length() ? variantsToStore
                                    .substring(VARIANTS_STORE_SEPARATOR
                                            .length()) : "null")
                                    + STORE_SEPARATOR + authority
                                    + STORE_SEPARATOR + meta.getLanguage());
                }

                if (meta.getLanguage() != null && !meta.getLanguage().trim().equals("")) {
                    String langField = field + "." + meta.getLanguage();
                    doc.addField(langField, value);
                }
            }

        } catch (Exception e) {
            log.error(LogHelper.getHeader(context, "item_metadata_discovery_error",
                    "Item identifier: " + item.getID()), e);
        }


        log.debug("  Added Metadata");

        try {

            List<MetadataValue> values = itemService.getMetadataByMetadataString(item, "dc.relation.ispartof");

            if (values != null && values.size() > 0 && values.get(0) != null && values.get(0).getValue() != null) {
                // group on parent
                String handlePrefix = handleService.getCanonicalPrefix();

                doc.addField("publication_grp", values.get(0).getValue().replaceFirst(handlePrefix, ""));

            } else {
                // group on self
                doc.addField("publication_grp", item.getHandle());
            }

        } catch (Exception e) {
            log.error(LogHelper.getHeader(context, "item_publication_group_discovery_error",
                    "Item identifier: " + item.getID()), e);
        }


        log.debug("  Added Grouping");
    }

    @Override
    public void writeDocument(Context context, IndexableItem indexableObject, SolrInputDocument solrInputDocument)
            throws SQLException, IOException, SolrServerException {
        writeDocument(solrInputDocument, new FullTextContentStreams(context, indexableObject.getIndexedObject()));
    }

    @Override
    public List<String> getLocations(Context context, IndexableItem indexableDSpaceObject)
            throws SQLException {
        final Item item = indexableDSpaceObject.getIndexedObject();
        List<String> locations = new ArrayList<>();

        // build list of community ids
        List<Community> communities = itemService.getCommunities(context, item);

        // build list of collection ids
        List<Collection> collections = item.getCollections();

        // now put those into strings
        int i = 0;

        for (i = 0; i < communities.size(); i++) {
            locations.add("m" + communities.get(i).getID());
        }

        for (i = 0; i < collections.size(); i++) {
            locations.add("l" + collections.get(i).getID());
        }

        return locations;
    }

    @Override
    public void delete(IndexableItem indexableObject) throws IOException, SolrServerException {
        super.delete(indexableObject);
        deleteInProgressData(indexableObject.getUniqueIndexID());
    }

    private void deleteInProgressData(String indexableObjectIdentifier) throws SolrServerException, IOException {
        // Also delete any possible workflowItem / workspaceItem / tasks related to this item
        String query = "inprogress.item:\"" + indexableObjectIdentifier + "\"";
        log.debug("Try to delete all in progress submission [DELETEBYQUERY]:" + query);
        solrSearchCore.getSolr().deleteByQuery(query);
    }

    @Override
    public void delete(String indexableObjectIdentifier) throws IOException, SolrServerException {
        super.delete(indexableObjectIdentifier);
        deleteInProgressData(indexableObjectIdentifier);
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof Item;
    }

    @Override
    public List getIndexableObjects(Context context, Item item) throws SQLException {
        if (item.isArchived() || item.isWithdrawn()) {
            // we only want to index an item as an item if it is not in workflow
            return List.of(new IndexableItem(item));
        }

        final WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, item);
        if (workspaceItem != null) {
            // a workspace item is linked to the given item
            return List.copyOf(workspaceItemIndexFactory.getIndexableObjects(context, workspaceItem));
        }

        final XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemService.findByItem(context, item);
        if (xmlWorkflowItem != null) {
            // a workflow item is linked to the given item
            return List.copyOf(workflowItemIndexFactory.getIndexableObjects(context, xmlWorkflowItem));
        }

        if (!isLatestVersion(context, item)) {
            // the given item is an older version of another item
            return List.of(new IndexableItem(item));
        }

        // nothing to index
        return List.of();
    }

    @Override
    public Optional<IndexableItem> findIndexableObject(Context context, String id) throws SQLException {
        final Item item = itemService.find(context, UUID.fromString(id));
        return item == null ? Optional.empty() : Optional.of(new IndexableItem(item));
    }
}
