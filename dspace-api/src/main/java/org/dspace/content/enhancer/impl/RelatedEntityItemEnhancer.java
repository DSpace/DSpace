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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.impl.ItemMetadataSource;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancer} that derives the additional metadata declared by the
 * relationship configuration.
 *
 * <p>For each relationship-backed primary value, the enhancer resolves the configured
 * {@link RelationshipMetadataGenerator}, reads the metadata of the related entity through an
 * {@link ItemMetadataSource} and saves the extracted values on the owning item, with the
 * relationship id.</p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see ItemEnhancer
 * @see RelationshipMetadataGenerator
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 */
public class RelatedEntityItemEnhancer implements ItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataValueService metadataValueService;

    @Autowired
    private RelationshipConfigurationService relationshipConfigurationService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Override
    public boolean canEnhance(Context context, Item item) {
        return true;
    }

    /**
     * Derive and persist the additional metadata configured for the relationships of the given item.
     *
     * @param context  the DSpace context for database operations
     * @param item     the item whose relationship additionals are enhanced
     * @param deepMode when true, all derived values are rebuilt from scratch
     * @return true when at least one derived value was added, updated or removed
     * @throws SQLRuntimeException if database operations fail during enhancement
     */
    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        try {
            return enhanceRelationshipAdditionals(context, item, deepMode);
        } catch (SQLException e) {
            LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
            throw new SQLRuntimeException(e);
        }
    }

    /**
     * Derive and persist the additional metadata configured for the relationships of the given item.
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
                    : new ItemMetadataSource(relatedItem, itemService);

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
                    desiredValues.add(placeholder(item, targetField, primary.getAuthority()));
                } else {
                    for (MetadataValueDTO extractedValue : extracted) {
                        desiredValues.add(withTargetFieldAndAuthority(item, extractedValue, targetField,
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
     * @param item             the item owning the derived value
     * @param extractedValue   the value produced by the generator
     * @param targetField      the fully qualified target field on the owning item
     * @param primaryAuthority the authority of the primary relationship value
     * @return the value to persist on the owning item
     */
    private MetadataValueDTO withTargetFieldAndAuthority(Item item, MetadataValueDTO extractedValue,
                                                         String targetField, String primaryAuthority) {
        MetadataFieldName target = new MetadataFieldName(targetField);
        String authority = derivedAuthority(item, target, extractedValue.getAuthority(), primaryAuthority);
        return new MetadataValueDTO(target.schema, target.element, target.qualifier,
                extractedValue.getLanguage(), extractedValue.getValue(), authority,
                extractedValue.getConfidence());
    }

    /**
     * Create the placeholder value written when the related entity yields no source values.
     *
     * @param item             the item owning the derived value
     * @param targetField      the fully qualified target field on the owning item
     * @param primaryAuthority the authority inherited from the primary relationship value
     * @return the placeholder value
     */
    private MetadataValueDTO placeholder(Item item, String targetField, String primaryAuthority) {
        MetadataFieldName target = new MetadataFieldName(targetField);
        String authority = derivedAuthority(item, target, null, primaryAuthority);
        return new MetadataValueDTO(target.schema, target.element, target.qualifier, null,
                PLACEHOLDER_PARENT_METADATA_VALUE, authority, Choices.CF_UNSET);
    }

    /**
     * Resolve the authority to stamp on a derived value. The authority declared by the extracted value
     * wins over the inherited primary authority, and neither is used when the target field is not
     * authority controlled on the owning item: such a value would be rejected on copy, for instance
     * when the item is versioned.
     *
     * @param item              the item owning the derived value
     * @param field             the parsed target field on the owning item
     * @param extractedAuthority the authority declared by the extracted value, if any
     * @param primaryAuthority  the authority of the primary relationship value, if any
     * @return the authority to persist, or null when the field does not support authorities
     */
    private String derivedAuthority(Item item, MetadataFieldName field, String extractedAuthority,
                                    String primaryAuthority) {
        String authority = StringUtils.isNotBlank(extractedAuthority) ? extractedAuthority : primaryAuthority;
        if (StringUtils.isBlank(authority)) {
            return null;
        }
        String fieldKey = metadataAuthorityService.makeFieldKey(field.schema, field.element, field.qualifier);
        return metadataAuthorityService.isAuthorityAllowed(fieldKey, item.getType(), item.getOwningCollection())
                ? authority
                : null;
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
                changed = addRelationshipAdditionals(context, item, relationship,
                        entry.getKey().targetField(), entry.getValue()) || changed;
            }
            return changed;
        }

        for (Map.Entry<AdditionalKey, List<MetadataValueDTO>> entry : desired.values().entrySet()) {
            List<MetadataValue> currentValues = current.getOrDefault(entry.getKey(), List.of());
            if (!equivalentToDesired(currentValues, entry.getValue())) {
                changed = deleteRelationshipAdditionals(context, currentValues) || changed;
                Relationship relationship = desired.relationships().get(entry.getKey().relationshipId());
                changed = addRelationshipAdditionals(context, item, relationship,
                        entry.getKey().targetField(), entry.getValue()) || changed;
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
     * Persist all derived values for a single relationship and target field.
     *
     * @param context      the DSpace context
     * @param item         the item being enhanced
     * @param relationship the relationship that produced the values
     * @param targetField  the fully qualified target field on the owning item
     * @param values       the values to persist
     * @return true when at least one value was persisted
     * @throws SQLException if a value cannot be persisted
     */
    private boolean addRelationshipAdditionals(Context context, Item item, Relationship relationship,
                                               String targetField, List<MetadataValueDTO> values)
            throws SQLException {
        boolean changed = false;
        for (MetadataValueDTO value : values) {
            changed = addRelationshipAdditional(context, item, relationship, targetField, value) || changed;
        }
        return changed;
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
            for (MetadataValue value : entry.getValue()) {
                // Additional values point at the relationship but are not its primary value.
                // Clear the link before removing them, otherwise the removal would delete the
                // relationship itself.
                value.setRelationship(null);
                metadataValueService.update(context, value);
            }
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

}
