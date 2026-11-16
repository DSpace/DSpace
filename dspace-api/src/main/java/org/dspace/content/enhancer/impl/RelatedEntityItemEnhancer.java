/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.AbstractItemEnhancer;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.impl.ItemMetadataSource;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancer} that creates virtual metadata fields by copying
 * values from related entities referenced through authority-controlled metadata fields.
 *
 * <p>In addition to the legacy {@code dspace.virtual.*} fields, the enhancer also derives the
 * additional metadata declared by the relationship configuration. For each relationship-backed
 * primary value it resolves the configured {@link RelationshipMetadataGenerator}, reads the related
 * entity through the {@link ItemMetadataSource} adapter and persists the extracted values on the
 * owning item, stamped with the relationship id.</p>
 *
 * <p><strong>Purpose and Functionality:</strong></p>
 * <p>This enhancer enables the CRIS virtual metadata system, which automatically
 * duplicates metadata from related entities onto items that reference them. This provides
 * denormalized access to related entity data without requiring complex joins, improving
 * search performance and enabling faceted search on related entity properties.</p>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>The enhancer is configured through Spring configuration (in
 * {@code dspace/config/spring/api/metadata-enhancers.xml}) with four key properties:</p>
 * <ul>
 *   <li><strong>sourceEntityTypes:</strong> Entity types that can be enhanced (e.g., "Publication", "Product",
 *   "Patent")</li>
 *   <li><strong>sourceItemMetadataFields:</strong> Metadata fields containing entity references
 *       (e.g., "dc.contributor.author", "dc.contributor.editor")</li>
 *   <li><strong>relatedItemMetadataFields:</strong> Metadata fields to copy from related entities
 *       (e.g., "person.identifier.orcid", "person.affiliation.name")</li>
 *   <li><strong>virtualQualifier:</strong> Qualifier for the virtual metadata fields (e.g., "orcid", "department")</li>
 * </ul>
 *
 * <p><strong>Real-world Configuration Examples:</strong></p>
 * <pre>{@code
 * <!-- Example 1: Copy ORCID identifiers from Person entities to Publications -->
 * <bean class="org.dspace.content.enhancer.impl.RelatedEntityItemEnhancer">
 *     <property name="sourceEntityTypes">
 *         <list>
 *             <value>Publication</value>
 *             <value>Product</value>
 *             <value>Patent</value>
 *         </list>
 *     </property>
 *     <property name="sourceItemMetadataFields">
 *         <list>
 *             <value>dc.contributor.author</value>
 *             <value>dc.contributor.editor</value>
 *         </list>
 *     </property>
 *     <property name="relatedItemMetadataFields">
 *         <list>
 *             <value>person.identifier.orcid</value>
 *         </list>
 *     </property>
 *     <property name="virtualQualifier" value="orcid" />
 * </bean>
 * }</pre>
 *
 * <p><strong>Virtual Metadata Structure:</strong></p>
 * <p>Creates two types of virtual metadata fields with the configured qualifier:</p>
 * <ul>
 *   <li><strong>{@code dspace.virtual.[qualifier]}:</strong> Contains the actual copied values</li>
 *   <li><strong>{@code dspace.virtualsource.[qualifier]}:</strong> Contains the UUID of the source entity</li>
 * </ul>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li><strong>Search Enhancement:</strong> Enable faceted search on author ORCIDs without complex joins</li>
 *   <li><strong>Analytics:</strong> Generate reports showing publications by department affiliation</li>
 *   <li><strong>Data Quality:</strong> Identify publications with authors missing ORCID identifiers</li>
 *   <li><strong>Export Integration:</strong> Include related entity metadata in metadata exports</li>
 * </ul>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see ItemEnhancer
 * @see AbstractItemEnhancer
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 * @see org.dspace.content.dao.ItemForMetadataEnhancementUpdateDAO
 */
public class RelatedEntityItemEnhancer extends AbstractItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelatedEntityItemEnhancerUtils relatedEntityItemEnhancerUtils;

    @Autowired
    private MetadataValueService metadataValueService;

    @Autowired
    private RelationshipConfigurationService relationshipConfigurationService;

    /**
     * the entities that can be extended by this enhancer, i.e. Publication
     */
    private List<String> sourceEntityTypes;

    /**
     * the metadata used to navigate the relation, i.e. dc.contributor.author
     */
    private List<String> sourceItemMetadataFields;

    /**
     * the metadata that is copied from the linked entity, i.e. person.identifier.orcid
     */
    private List<String> relatedItemMetadataFields;

    @Override
    public boolean canEnhance(Context context, Item item) {
        return sourceEntityTypes == null || sourceEntityTypes.contains(itemService.getEntityTypeLabel(item));
    }

    /**
     * Enhances the given item by synchronizing virtual metadata fields with values
     * copied from related entities referenced through authority-controlled metadata.
     * 
     * <p><strong>How Enhancement Works:</strong></p>
     * <ol>
     *   <li><strong>Identify Related Entities:</strong> Scans the item's configured source
     *       metadata fields (e.g., dc.contributor.author) for authority values that contain
     *       UUIDs of related entities</li>
     *   <li><strong>Copy Metadata:</strong> For each related entity found, copies values from
     *       configured metadata fields (e.g., person.identifier.orcid) into virtual metadata
     *       fields on the current item</li>
     *   <li><strong>Maintain Relationships:</strong> Creates {@code dspace.virtualsource.*} fields
     *       to track which virtual metadata came from which related entity for dependency management</li>
     *   <li><strong>Handle Missing Entities:</strong> When a related entity is not found or has
     *       no values, inserts placeholder values to maintain metadata field consistency</li>
     * </ol>
     * 
     * <p><strong>Processing Modes:</strong></p>
     * <ul>
     *   <li><strong>Incremental Mode (deepMode=false):</strong> Only adds missing virtual metadata
     *       and removes obsolete entries. More efficient for routine updates.</li>
     *   <li><strong>Deep Mode (deepMode=true):</strong> Completely rebuilds all virtual metadata
     *       by comparing current state with expected state. Used for comprehensive synchronization.</li>
     * </ul>
     * 
     * <p><strong>Virtual Metadata Structure:</strong></p>
     * <ul>
     *   <li><strong>{@code dspace.virtual.[qualifier]}:</strong> Contains the actual copied values
     *       from related entities (e.g., ORCID identifiers, names, affiliations)</li>
     *   <li><strong>{@code dspace.virtualsource.[qualifier]}:</strong> Contains the UUID of the
     *       related entity that provided each virtual value, enabling dependency tracking</li>
     * </ul>
     * 
     * <p><strong>Example Scenario:</strong></p>
     * <pre>
     * Publication item has: dc.contributor.author = "John Doe" with authority = "uuid-123"
     * Person entity uuid-123 has: person.identifier.orcid = "0000-0000-0000-0000"
     * 
     * After enhancement, Publication gains:
     * - dspace.virtual.author-orcid = "0000-0000-0000-0000"
     * - dspace.virtualsource.author-orcid = "uuid-123"
     * </pre>
     *
     * 
     * @param context the DSpace context for database operations
     * @param item the item to enhance with virtual metadata from related entities
     * @param deepMode if true, performs complete rebuild; if false, performs incremental update
     * @return true if any virtual metadata was added, updated, or removed; false if no changes were made
     * @throws SQLRuntimeException if database operations fail during enhancement
     * 
     * @see #performEnhancement(Context, Item) for incremental enhancement logic
     * @see #getToBeVirtualMetadata(Context, Item) for deep mode comparison logic
     * @see #cleanObsoleteVirtualFields(Context, Item) for cleanup of obsolete virtual fields
     */
    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        boolean result = false;
        if (!deepMode) {
            try {
                result = cleanObsoleteVirtualFields(context, item);
                result = performEnhancement(context, item) || result;
                result = enhanceRelationshipAdditionals(context, item, false) || result;
            } catch (SQLException e) {
                LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
                throw new SQLRuntimeException(e);
            }
        } else {
            Map<String, List<MetadataValue>> currMetadataValues = relatedEntityItemEnhancerUtils
                    .getCurrentVirtualsMap(item, getVirtualQualifier());
            Map<String, List<MetadataValueDTO>> toBeMetadataValues = getToBeVirtualMetadata(context, item);
            if (!equivalent(currMetadataValues, toBeMetadataValues)) {
                try {
                    clearAllVirtualMetadata(context, item);
                    addMetadata(context, item, toBeMetadataValues);
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
                result = true;
            }
            try {
                result = enhanceRelationshipAdditionals(context, item, true) || result;
            } catch (SQLException e) {
                LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
                throw new SQLRuntimeException(e);
            }
        }
        return result;
    }

    private void clearAllVirtualMetadata(Context context, Item item) throws SQLException {
        itemService.clearMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT,
                getVirtualQualifier(), Item.ANY);
        itemService.clearMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
                getVirtualQualifier(), Item.ANY);
    }

    private void addMetadata(Context context, Item item, Map<String, List<MetadataValueDTO>> toBeMetadataValues)
            throws SQLException {
        for (Entry<String, List<MetadataValueDTO>> metadataValues : toBeMetadataValues.entrySet()) {
            for (MetadataValueDTO dto : metadataValues.getValue()) {
                addVirtualSourceField(context, item, metadataValues.getKey());
                addVirtualField(context, item, dto.getValue(), dto.getAuthority(), dto.getLanguage(),
                        dto.getConfidence());
            }
        }
    }

    private boolean equivalent(Map<String, List<MetadataValue>> currMetadataValues,
            Map<String, List<MetadataValueDTO>> toBeMetadataValues) {
        if (currMetadataValues.size() != toBeMetadataValues.size()) {
            return false;
        } else {
            for (String key : currMetadataValues.keySet()) {
                if (!equivalent(currMetadataValues.get(key), toBeMetadataValues.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean equivalent(List<MetadataValue> metadataValue, List<MetadataValueDTO> metadataValueDTO) {
        if ((Objects.isNull(metadataValue) && !Objects.isNull(metadataValueDTO)) ||
                (!Objects.isNull(metadataValue) && Objects.isNull(metadataValueDTO)) ||
                    metadataValue.size() != metadataValueDTO.size()) {
            return false;
        } else if (!Objects.isNull(metadataValue) && !Objects.isNull(metadataValueDTO)) {
            for (int i = 0; i < metadataValue.size(); i++) {
                if (!equivalent(metadataValue.get(i), metadataValueDTO.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean equivalent(MetadataValue metadataValue, MetadataValueDTO metadataValueDTO) {
        return StringUtils.equals(metadataValue.getMetadataField().getMetadataSchema().getName(),
                metadataValueDTO.getSchema())
                && StringUtils.equals(metadataValue.getMetadataField().getElement(), metadataValueDTO.getElement())
                && StringUtils.equals(metadataValue.getMetadataField().getQualifier(), metadataValueDTO.getQualifier())
                && StringUtils.equals(metadataValue.getValue(), metadataValueDTO.getValue())
                && StringUtils.equals(metadataValue.getAuthority(), metadataValueDTO.getAuthority());
    }

    private Map<String, List<MetadataValueDTO>> getToBeVirtualMetadata(Context context, Item item) {
        Map<String, List<MetadataValueDTO>> tobeVirtualMetadataMap = new HashMap<String, List<MetadataValueDTO>>();

        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            List<MetadataValueDTO> tobeVirtualMetadata = new ArrayList<>();
            Item relatedItem = null;
            relatedItem = findRelatedEntityItem(context, authority);
            if (relatedItem == null) {
                MetadataValueDTO mvRelated = new MetadataValueDTO();
                mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                mvRelated.setQualifier(getVirtualQualifier());
                mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                tobeVirtualMetadata.add(mvRelated);
            } else {
                boolean foundAtLeastOneValue = false;
                for (String relatedItemMetadataField : relatedItemMetadataFields) {
                    List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem,
                            relatedItemMetadataField);
                    for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                        MetadataValueDTO mvRelated = new MetadataValueDTO();
                        mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                        mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                        mvRelated.setQualifier(getVirtualQualifier());
                        mvRelated.setValue(relatedItemMetadataValue.getValue());
                        String authorityRelated = relatedItemMetadataValue.getAuthority();
                        if (StringUtils.isNotBlank(authorityRelated)) {
                            mvRelated.setAuthority(authorityRelated);
                            mvRelated.setConfidence(Choices.CF_ACCEPTED);
                        }
                        tobeVirtualMetadata.add(mvRelated);
                        foundAtLeastOneValue = true;
                    }
                }
                if (!foundAtLeastOneValue) {
                    MetadataValueDTO mvRelated = new MetadataValueDTO();
                    mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                    mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                    mvRelated.setQualifier(getVirtualQualifier());
                    mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                    tobeVirtualMetadata.add(mvRelated);
                }
            }
            tobeVirtualMetadataMap.put(authority, tobeVirtualMetadata);
        }
        return tobeVirtualMetadataMap;
    }

    private boolean cleanObsoleteVirtualFields(Context context, Item item) throws SQLException {
        boolean result = false;
        List<MetadataValue> metadataValuesToDelete = getObsoleteVirtualFields(item);
        if (!metadataValuesToDelete.isEmpty()) {
            itemService.removeMetadataValues(context, item, metadataValuesToDelete);
            result = true;
        }
        return result;
    }

    private List<MetadataValue> getObsoleteVirtualFields(Item item) {

        List<MetadataValue> obsoleteVirtualFields = new ArrayList<>();
        Map<String, List<MetadataValue>> currentVirtualsMap = relatedEntityItemEnhancerUtils
                .getCurrentVirtualsMap(item, getVirtualQualifier());
        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : currentVirtualsMap.keySet()) {
            if (!virtualSources.contains(authority)) {
                for (MetadataValue mv : getVirtualSourceFields(item, authority)) {
                    obsoleteVirtualFields.add(mv);
                    getRelatedVirtualField(item, mv.getPlace()).ifPresent(obsoleteVirtualFields::add);
                }
            }
        }

        return obsoleteVirtualFields;

    }

    private Set<String> getVirtualSources(Item item) {
        return sourceItemMetadataFields.stream()
                .flatMap(field -> itemService.getMetadataByMetadataString(item, field).stream())
                .filter(mv -> UUIDUtils.fromString(mv.getAuthority()) != null)
                .map(mv -> mv.getAuthority())
                .collect(Collectors.toSet());
    }

    private Optional<MetadataValue> getRelatedVirtualField(Item item, int pos) {
        return getVirtualFields(item).stream()
            .skip(pos)
            .findFirst();
    }

    private boolean performEnhancement(Context context, Item item) throws SQLException {
        boolean result = false;
        Map<String, List<MetadataValue>> currentVirtualsMap = relatedEntityItemEnhancerUtils
                .getCurrentVirtualsMap(item, getVirtualQualifier());
        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            boolean foundAtLeastOne = false;
            if (!currentVirtualsMap.containsKey(authority)) {
                result = true;
                Item relatedItem = findRelatedEntityItem(context, authority);
                if (relatedItem == null) {
                    addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
                    addVirtualSourceField(context, item, authority);
                    continue;
                }

                for (String relatedItemMetadataField : relatedItemMetadataFields) {
                    List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem,
                            relatedItemMetadataField);
                    for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                        foundAtLeastOne = true;
                        addVirtualField(context, item, relatedItemMetadataValue.getValue(),
                                relatedItemMetadataValue.getAuthority(), relatedItemMetadataValue.getLanguage(),
                                relatedItemMetadataValue.getConfidence());
                        addVirtualSourceField(context, item, authority);
                    }
                }
                if (!foundAtLeastOne) {
                    addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
                    addVirtualSourceField(context, item, authority);
                    continue;
                }
            }
        }
        return result;
    }

    private Item findRelatedEntityItem(Context context, String authority) {
        try {
            UUID relatedItemUUID = UUIDUtils.fromString(authority);
            return relatedItemUUID != null ? itemService.find(context, relatedItemUUID) : null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getVirtualSourceFields(Item item, String authority) {
        return getMetadataValues(item, getVirtualSourceMetadataField()).stream()
                .filter(mv -> StringUtils.equals(authority, mv.getValue())).collect(Collectors.toList());
    }

    private List<MetadataValue> getVirtualFields(Item item) {
        return getMetadataValues(item, getVirtualMetadataField());
    }

    private void addVirtualField(Context context, Item item, String value, String authority, String lang,
            int confidence) throws SQLException {
        if (StringUtils.startsWith(authority, AuthorityValueService.GENERATE)
                || StringUtils.startsWith(authority, AuthorityValueService.REFERENCE)) {
            itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
                    getVirtualQualifier(), lang, value, null, Choices.CF_UNSET);
        } else {
            itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
                    getVirtualQualifier(), lang, value, authority, confidence);
        }
    }

    private void addVirtualSourceField(Context context, Item item, String sourceValueAuthority) throws SQLException {
        itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT,
                                getVirtualQualifier(), null, sourceValueAuthority);
    }

    /**
     * Derive and persist the additional metadata configured for the relationships of the given item.
     *
     * <p>The enhancer acts as a thin use case here: it builds the database adapter at the boundary,
     * resolves the generator for each configured target field through the relationship configuration
     * service and asks the generator to extract the values. The generator never sees the related
     * {@link Item} or the {@link ItemService}; it only sees the abstract {@link MetadataSource}.</p>
     *
     * @param context  the DSpace context
     * @param item     the item whose relationship additionals are enhanced
     * @param deepMode when true, all derived values are rebuilt from scratch
     * @return true when at least one derived value was added, updated or removed
     * @throws SQLException if the derived values cannot be read or persisted
     */
    private boolean enhanceRelationshipAdditionals(Context context, Item item, boolean deepMode)
            throws SQLException {
        Map<AdditionalKey, List<MetadataValue>> current = getCurrentRelationshipAdditionals(context, item);
        DesiredState desired = getDesiredRelationshipAdditionals(context, item);
        return reconcileRelationshipAdditionals(context, item, current, desired, deepMode);
    }

    /**
     * Build the desired state of the relationship additionals, keyed by relationship and target field.
     *
     * @param context the DSpace context
     * @param item    the item being enhanced
     * @return the desired derived values together with the relationships that produce them
     * @throws SQLException if the relationship configuration or the related metadata cannot be read
     */
    private DesiredState getDesiredRelationshipAdditionals(Context context, Item item) throws SQLException {
        Map<AdditionalKey, List<MetadataValueDTO>> values = new LinkedHashMap<>();
        Map<Integer, Relationship> relationships = new LinkedHashMap<>();

        for (MetadataValue primary : new ArrayList<>(item.getMetadata())) {
            Relationship relationship = primary.getRelationship();
            if (relationship == null || !isConfigurationBacked(relationship)
                    || !relationshipConfigurationService.isPrimaryMetadata(context, relationship, primary)) {
                continue;
            }
            Item owner = (Item) primary.getDSpaceObject();
            Set<String> targetFields = relationshipConfigurationService
                    .getAdditionalMetadataFields(context, relationship, owner);
            if (targetFields.isEmpty()) {
                continue;
            }

            relationships.putIfAbsent(relationship.getID(), relationship);
            Item relatedItem = getRelatedItem(relationship, owner);
            MetadataSource source = relatedItem == null
                    ? null
                    : new ItemMetadataSource(context, relatedItem, itemService);

            for (String targetField : targetFields) {
                Optional<RelationshipMetadataGenerator> generator = relationshipConfigurationService
                        .getMetadataGenerator(context, relationship, owner, targetField);
                if (generator.isEmpty()) {
                    continue;
                }

                List<MetadataValueDTO> extracted = source == null
                        ? List.of()
                        : generator.get().extract(source);
                List<MetadataValueDTO> desiredValues = values.computeIfAbsent(
                        new AdditionalKey(relationship.getID(), targetField), key -> new ArrayList<>());
                if (extracted.isEmpty()) {
                    desiredValues.add(placeholder(targetField, primary.getAuthority()));
                } else {
                    for (MetadataValueDTO extractedValue : extracted) {
                        desiredValues.add(withTargetFieldAndAuthority(extractedValue, targetField,
                                primary.getAuthority()));
                    }
                }
            }
        }

        return new DesiredState(values, relationships);
    }

    /**
     * Copy an extracted value onto its configured target field, inheriting the primary authority when
     * the derived value does not declare one.
     *
     * @param extractedValue the value produced by the generator
     * @param targetField    the fully qualified target field on the owning item
     * @param primaryAuthority the authority of the primary relationship value
     * @return the value to persist on the owning item
     */
    private MetadataValueDTO withTargetFieldAndAuthority(MetadataValueDTO extractedValue, String targetField,
                                                         String primaryAuthority) {
        MetadataFieldName target = new MetadataFieldName(targetField);
        String authority = StringUtils.isNotBlank(extractedValue.getAuthority())
                ? extractedValue.getAuthority()
                : primaryAuthority;
        return new MetadataValueDTO(target.schema, target.element, target.qualifier,
                extractedValue.getLanguage(), extractedValue.getValue(), authority,
                extractedValue.getConfidence());
    }

    /**
     * Create the placeholder value written when the related entity yields no source values.
     *
     * @param targetField      the fully qualified target field on the owning item
     * @param primaryAuthority the authority inherited from the primary relationship value
     * @return the placeholder value
     */
    private MetadataValueDTO placeholder(String targetField, String primaryAuthority) {
        MetadataFieldName target = new MetadataFieldName(targetField);
        return new MetadataValueDTO(target.schema, target.element, target.qualifier, null,
                PLACEHOLDER_PARENT_METADATA_VALUE, primaryAuthority, Choices.CF_UNSET);
    }

    /**
     * Collect the relationship additionals currently stored on the item, keyed by relationship and
     * target field.
     *
     * @param context the DSpace context
     * @param item    the item being enhanced
     * @return the current derived values
     * @throws SQLException if the relationship configuration cannot be resolved
     */
    private Map<AdditionalKey, List<MetadataValue>> getCurrentRelationshipAdditionals(Context context, Item item)
            throws SQLException {
        Map<AdditionalKey, List<MetadataValue>> current = new LinkedHashMap<>();
        for (MetadataValue value : new ArrayList<>(item.getMetadata())) {
            Relationship relationship = value.getRelationship();
            if (relationship == null || !isConfigurationBacked(relationship)
                    || relationshipConfigurationService.isPrimaryMetadata(context, relationship, value)) {
                continue;
            }

            Item owner = (Item) value.getDSpaceObject();
            String targetField = fieldName(value);
            if (!relationshipConfigurationService.getAdditionalMetadataFields(context, relationship, owner)
                    .contains(targetField)) {
                continue;
            }
            current.computeIfAbsent(new AdditionalKey(relationship.getID(), targetField),
                    key -> new ArrayList<>()).add(value);
        }
        return current;
    }

    /**
     * Reconcile the current relationship additionals with the desired state.
     *
     * @param context   the DSpace context
     * @param item      the item being enhanced
     * @param current   the currently persisted derived values
     * @param desired   the desired derived values
     * @param deepMode  when true, all derived values are rebuilt from scratch
     * @return true when at least one value was added or removed
     * @throws SQLException if a value cannot be added or removed
     */
    private boolean reconcileRelationshipAdditionals(Context context, Item item,
                                                     Map<AdditionalKey, List<MetadataValue>> current,
                                                     DesiredState desired, boolean deepMode) throws SQLException {
        boolean changed = false;
        if (deepMode) {
            changed = deleteRelationshipAdditionals(context,
                    current.values().stream().flatMap(List::stream).collect(Collectors.toList()));
            for (Map.Entry<AdditionalKey, List<MetadataValueDTO>> entry : desired.values().entrySet()) {
                Relationship relationship = desired.relationships().get(entry.getKey().relationshipId());
                for (MetadataValueDTO value : entry.getValue()) {
                    changed = addRelationshipAdditional(context, item, relationship,
                            entry.getKey().targetField(), value) || changed;
                }
            }
            return changed;
        }

        for (Map.Entry<AdditionalKey, List<MetadataValueDTO>> entry : desired.values().entrySet()) {
            List<MetadataValue> currentValues = current.getOrDefault(entry.getKey(), List.of());
            if (!equivalentToDesired(currentValues, entry.getValue())) {
                changed = deleteRelationshipAdditionals(context, currentValues) || changed;
                Relationship relationship = desired.relationships().get(entry.getKey().relationshipId());
                for (MetadataValueDTO value : entry.getValue()) {
                    changed = addRelationshipAdditional(context, item, relationship,
                            entry.getKey().targetField(), value) || changed;
                }
            }
        }
        for (Map.Entry<AdditionalKey, List<MetadataValue>> entry : current.entrySet()) {
            if (!desired.values().containsKey(entry.getKey())) {
                changed = deleteRelationshipAdditionals(context, entry.getValue()) || changed;
            }
        }
        return changed;
    }

    /**
     * Check whether the persisted values already match the desired values.
     *
     * @param current the persisted values
     * @param desired the desired values
     * @return true when no write is needed
     */
    private boolean equivalentToDesired(List<MetadataValue> current, List<MetadataValueDTO> desired) {
        if (current.size() != desired.size()) {
            return false;
        }
        List<MetadataValueDTO> remaining = new ArrayList<>(desired);
        for (MetadataValue value : current) {
            if (!remaining.removeIf(expected -> sameValue(value, expected))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether a persisted value carries the same content as a desired value.
     *
     * @param value    the persisted value
     * @param expected the desired value
     * @return true when value, language, authority and confidence all match
     */
    private boolean sameValue(MetadataValue value, MetadataValueDTO expected) {
        return StringUtils.equals(value.getValue(), expected.getValue())
                && StringUtils.equals(value.getLanguage(), expected.getLanguage())
                && StringUtils.equals(value.getAuthority(), expected.getAuthority())
                && value.getConfidence() == expected.getConfidence();
    }

    /**
     * Persist a single derived value, stamping it with the relationship that produced it.
     *
     * @param context      the DSpace context
     * @param item         the item being enhanced
     * @param relationship the relationship that produced the value
     * @param targetField  the fully qualified target field on the owning item
     * @param value        the value to persist
     * @return true when the value was persisted
     * @throws SQLException if the value cannot be persisted
     */
    private boolean addRelationshipAdditional(Context context, Item item, Relationship relationship,
                                              String targetField, MetadataValueDTO value) throws SQLException {
        MetadataFieldName target = new MetadataFieldName(targetField);
        // The authority is applied after the insert: derived fields are usually not authority
        // controlled, and the item service rejects an authority for such a field on insert.
        MetadataValue added = itemService.addMetadata(context, item, target.schema, target.element,
                target.qualifier, value.getLanguage(), value.getValue(), null, value.getConfidence());
        if (added == null) {
            return false;
        }
        if (relationship != null) {
            added.setRelationship(relationship);
        }
        if (StringUtils.isNotBlank(value.getAuthority())) {
            added.setAuthority(value.getAuthority());
        }
        metadataValueService.update(context, added);
        return true;
    }

    /**
     * Delete derived values through the item service, so the owning item stays consistent.
     *
     * @param context the DSpace context
     * @param values  the values to delete
     * @return true when at least one value was deleted
     * @throws SQLException if a value cannot be deleted
     */
    private boolean deleteRelationshipAdditionals(Context context, List<MetadataValue> values) throws SQLException {
        if (values.isEmpty()) {
            return false;
        }
        Map<Item, List<MetadataValue>> byOwner = new LinkedHashMap<>();
        for (MetadataValue value : values) {
            byOwner.computeIfAbsent((Item) value.getDSpaceObject(), owner -> new ArrayList<>()).add(value);
        }
        for (Map.Entry<Item, List<MetadataValue>> entry : byOwner.entrySet()) {
            itemService.removeMetadataValues(context, entry.getKey(), entry.getValue());
        }
        return true;
    }

    /**
     * Format the fully qualified name of the field a metadata value belongs to.
     *
     * @param value the metadata value
     * @return the field name as {@code schema.element[.qualifier]}
     */
    private String fieldName(MetadataValue value) {
        return new MetadataFieldName(value.getSchema(), value.getElement(), value.getQualifier()).toString();
    }

    /**
     * Whether the relationship is backed by a resolvable configuration.
     *
     * @param relationship the relationship to inspect
     * @return true when the relationship carries a configuration key
     */
    private boolean isConfigurationBacked(Relationship relationship) {
        return StringUtils.isNotBlank(relationship.getRelationshipConfigKey());
    }

    /**
     * Return the entity on the other side of the relationship.
     *
     * @param relationship the relationship
     * @param owner        the owner endpoint
     * @return the related item
     */
    private Item getRelatedItem(Relationship relationship, Item owner) {
        return relationship.getLeftItem().getID().equals(owner.getID())
                ? relationship.getRightItem()
                : relationship.getLeftItem();
    }

    /**
     * Identity of a derived value: the relationship of the hop plus the target metadata field.
     *
     * @param relationshipId the relationship id
     * @param targetField    the fully qualified target metadata field
     */
    private record AdditionalKey(Integer relationshipId, String targetField) {
    }

    /**
     * The desired derived values together with the relationships that produce them.
     *
     * @param values        the desired values per relationship and target field
     * @param relationships the relationships indexed by id
     */
    private record DesiredState(Map<AdditionalKey, List<MetadataValueDTO>> values,
                                Map<Integer, Relationship> relationships) {
    }

    public void setSourceEntityTypes(List<String> sourceEntityTypes) {
        this.sourceEntityTypes = sourceEntityTypes;
    }

    public void setRelatedItemMetadataFields(List<String> relatedItemMetadataFields) {
        this.relatedItemMetadataFields = relatedItemMetadataFields;
    }

    public void setSourceItemMetadataFields(List<String> sourceItemMetadataFields) {
        this.sourceItemMetadataFields = sourceItemMetadataFields;
    }

}
