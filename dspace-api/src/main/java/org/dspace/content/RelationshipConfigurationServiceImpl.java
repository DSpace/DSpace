/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * POC bridge: keep existing CRIS authority configuration intact. Explicit Spring
 * definitions take precedence. The fallback stores an "authority:<field>" key on
 * the relationship, rather than inferring its meaning from a surviving metadata row.
 * Repositories can replace that fallback with a bean using the same stable key.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class RelationshipConfigurationServiceImpl implements RelationshipConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(RelationshipConfigurationServiceImpl.class);

    public static final String AUTHORITY_PREFIX = "authority:";

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired(required = false)
    private List<RelationshipTypeConfiguration> configurations = new ArrayList<>();

    private record PrimaryMetadataMapping(
        String field,
        String configurationId,
        String side,
        Set<String> entityTypes
    ) {
    }

    /**
     * Validates all explicitly configured relationship definitions at startup.
     *
     * @throws RelationshipConfigurationException if a configured metadata field has invalid syntax
     * @throws IllegalArgumentException if a relationship configuration contains other invalid values
     */
    @PostConstruct
    public void validateConfigurations() {
        Set<String> ids = new HashSet<>();
        List<PrimaryMetadataMapping> mappings = new ArrayList<>();

        for (RelationshipTypeConfiguration configuration : configurations) {
            // Reject null relationship configurations.
            if (configuration == null) {
                throw new IllegalArgumentException("Relationship configuration cannot be null");
            }

            // Ensure every configuration has a valid ID.
            String id = configuration.getId();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Relationship configuration must have an ID");
            }

            // Ensure configuration IDs are unique.
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Duplicate relationship configuration ID: " + id);
            }

            // Validate endpoint entity types, metadata fields, and generator mappings.
            validateConfiguration(configuration);

            // Reject primary metadata mappings with overlapping endpoint entity types.
            configuration.getLeftPrimaryMetadataField().ifPresent(field -> validatePrimaryMapping(
                mappings, new PrimaryMetadataMapping(field, id, "left", configuration.getLeftEntityTypes())));

            configuration.getRightPrimaryMetadataField().ifPresent(field -> validatePrimaryMapping(
                mappings, new PrimaryMetadataMapping(field, id, "right", configuration.getRightEntityTypes())));
        }
    }

    /**
     * Rejects primary metadata mappings that share a field and at least one owning entity type.
     * The check applies across configurations and across relationship endpoints.
     */
    private void validatePrimaryMapping(List<PrimaryMetadataMapping> mappings, PrimaryMetadataMapping candidate) {
        for (PrimaryMetadataMapping existing : mappings) {
            if (!existing.field().equals(candidate.field())) {
                continue;
            }

            Set<String> overlappingTypes = new HashSet<>(existing.entityTypes());
            overlappingTypes.retainAll(candidate.entityTypes());

            if (!overlappingTypes.isEmpty()) {
                throw new IllegalArgumentException(
                    "Ambiguous primary metadata field " + candidate.field()
                        + " between relationship configurations " + existing.configurationId()
                        + " (" + existing.side() + ") and " + candidate.configurationId()
                        + " (" + candidate.side() + ") for entity types " + overlappingTypes);
            }
        }

        mappings.add(candidate);
    }

    /**
     * Validates the metadata fields, entity types and generators of a relationship configuration.
     *
     * @param configuration relationship configuration to validate
     * @throws RelationshipConfigurationException if a configured metadata field has invalid syntax
     * @throws IllegalArgumentException if the configuration contains invalid entity types or conflicting values
     */
    private void validateConfiguration(RelationshipTypeConfiguration configuration) {
        String id = configuration.getId();

        Optional<String> leftPrimary = configuration.getLeftPrimaryMetadataField();
        Optional<String> rightPrimary = configuration.getRightPrimaryMetadataField();

        // Both endpoints of an explicit relationship configuration must define at least one allowed entity type.
        if (configuration.getLeftEntityTypes().isEmpty() || configuration.getRightEntityTypes().isEmpty()) {
            throw new IllegalArgumentException(
                "Relationship configuration " + id + " must define at least one entity type for each endpoint");
        }

        // Validate the primary metadata field and additional metadata generator mappings for each endpoint.
        validateEndpointConfiguration(id, "left", leftPrimary, configuration.getLeftMetadataGenerators());
        validateEndpointConfiguration(id, "right", rightPrimary, configuration.getRightMetadataGenerators());
    }

    /**
     * Validates the primary metadata field and additional metadata generators of an endpoint.
     *
     * @param configurationId stable identifier of the relationship configuration
     * @param side            endpoint being validated
     * @param primaryField    optional primary metadata field for the endpoint
     * @param generators      additional metadata generators keyed by metadata field
     * @throws RelationshipConfigurationException if a configured metadata field has invalid syntax
     * @throws IllegalArgumentException if a primary field is also configured as additional metadata
     */
    private void validateEndpointConfiguration(
        String configurationId,
        String side,
        Optional<String> primaryField,
        Map<String, RelationshipMetadataGenerator> generators) {

        // Validate the primary metadata field, if configured.
        primaryField.ifPresent(field -> validateMetadataField(configurationId, side, field, true));

        for (Map.Entry<String, RelationshipMetadataGenerator> entry : generators.entrySet()) {
            String field = entry.getKey();

            // Validate each additional metadata field.
            validateMetadataField(configurationId, side, field, false);

            // Prevent a primary metadata field from also being configured as additional metadata.
            if (primaryField.filter(field::equals).isPresent()) {
                throw new IllegalArgumentException(
                    "Primary metadata field cannot also be additional metadata in relationship configuration "
                        + configurationId + " on the " + side + " endpoint: " + field);
            }

            // Validate the source field declared by the generator.
            validateMetadataField(configurationId, side, entry.getValue().getSourceField(), "source");
        }
    }

    /**
     * Validates the syntax of a configured metadata field, without checking its existence in the metadata registry.
     * Accepts fields like "dc.title" and "dc.contributor.author", while rejecting empty or malformed field names.
     *
     * @param configurationId stable identifier of the relationship configuration
     * @param side            endpoint containing the field
     * @param field           metadata field name to validate
     * @param isPrimary       whether the field is a primary metadata field
     * @throws RelationshipConfigurationException if a primary or additional metadata field has invalid syntax
     */
    private void validateMetadataField(String configurationId, String side, String field, boolean isPrimary) {
        validateMetadataField(configurationId, side, field, isPrimary ? "primary" : "additional");
    }

    /**
     * Validates the syntax of a configured metadata field, without checking its existence in the metadata registry.
     *
     * @param configurationId stable identifier of the relationship configuration
     * @param side            endpoint containing the field
     * @param field           metadata field name to validate
     * @param role            role of the field in the configuration (primary, additional or source)
     * @throws RelationshipConfigurationException if the metadata field has invalid syntax
     */
    private void validateMetadataField(String configurationId, String side, String field, String role) {
        if (field == null || !field.matches("[^.\\s]+\\.[^.\\s]+(\\.[^.\\s]+)?")) {
            throw new RelationshipConfigurationException(
                "Invalid " + role + " metadata field '" + field +
                    "' in relationship configuration " + configurationId + " on the " + side + " endpoint");
        }
    }

    /**
     * Validates that primary metadata fields exist in the metadata registry and filters out
     * additional metadata fields that are not registered.
     *
     * Missing primary metadata fields and missing generator source fields invalidate the relationship configuration.
     * Missing additional metadata fields are logged and excluded from the resolved configuration.
     *
     * @param context       current DSpace context
     * @param configuration relationship configuration to validate
     * @return the original configuration if all fields are valid, or a filtered configuration
     *         containing only registered additional metadata fields
     * @throws RelationshipConfigurationException if a configured primary or source metadata field does not exist
     * @throws SQLException if an error occurs while accessing the metadata registry
     */
    private RelationshipTypeConfiguration validateMetadataFields(Context context,
                                                                 RelationshipTypeConfiguration configuration)
        throws SQLException {

        // Missing primary metadata invalidates the configuration.
        if (configuration.getLeftPrimaryMetadataField().isPresent()) {
            validatePrimaryMetadataField(context, configuration.getId(),
                                         configuration.getLeftPrimaryMetadataField().get());
        }
        if (configuration.getRightPrimaryMetadataField().isPresent()) {
            validatePrimaryMetadataField(context, configuration.getId(),
                                         configuration.getRightPrimaryMetadataField().get());
        }

        Map<String, RelationshipMetadataGenerator> leftGenerators =
            filterMetadataGenerators(context, configuration.getId(), "left", configuration.getLeftMetadataGenerators());

        Map<String, RelationshipMetadataGenerator> rightGenerators =
            filterMetadataGenerators(context, configuration.getId(), "right",
                                     configuration.getRightMetadataGenerators());

        // Return the original configuration if no additional fields were removed.
        if (leftGenerators.size() == configuration.getLeftMetadataGenerators().size()
            && rightGenerators.size() == configuration.getRightMetadataGenerators().size()) {
            return configuration;
        }

        // Preserve the relationship configuration while excluding unregistered additional metadata fields.
        RelationshipTypeConfiguration filtered = new RelationshipTypeConfiguration();
        filtered.setId(configuration.getId());
        filtered.setLeftEntityTypes(configuration.getLeftEntityTypes());
        filtered.setRightEntityTypes(configuration.getRightEntityTypes());
        filtered.setLeftPrimaryMetadataField(configuration.getLeftPrimaryMetadataField().orElse(null));
        filtered.setRightPrimaryMetadataField(configuration.getRightPrimaryMetadataField().orElse(null));
        filtered.setLeftMetadataGenerators(leftGenerators);
        filtered.setRightMetadataGenerators(rightGenerators);

        return filtered;
    }

    /**
     * Validates that a primary metadata field exists in the metadata registry.
     *
     * @throws RelationshipConfigurationException if the field does not exist
     * @throws SQLException if an error occurs while accessing the metadata registry
     */
    private void validatePrimaryMetadataField(Context context, String configurationId, String field)
        throws SQLException {

        if (!metadataFieldExists(context, field)) {
            throw new RelationshipConfigurationException(
                "Relationship configuration " + configurationId + " references a nonexistent primary metadata field: " +
                    field);
        }
    }

    /**
     * Filters out metadata generators referencing unregistered additional metadata fields and validates the
     * source field declared by each remaining generator.
     *
     * Missing additional metadata fields are logged as errors without invalidating the relationship configuration.
     * Because every generator backs an additional field, a generator whose source field is missing is logged and
     * dropped as well, following the same severity rule.
     *
     * @param context         current DSpace context
     * @param configurationId stable identifier of the relationship configuration
     * @param side            endpoint owning the generators
     * @param generators      additional metadata generators keyed by target metadata field
     * @return a map containing only generators for registered metadata fields
     * @throws RelationshipConfigurationException if a generator source metadata field does not exist
     * @throws SQLException if an error occurs while accessing the metadata registry
     */
    private Map<String, RelationshipMetadataGenerator> filterMetadataGenerators(
        Context context,
        String configurationId,
        String side,
        Map<String, RelationshipMetadataGenerator> generators) throws SQLException {

        Map<String, RelationshipMetadataGenerator> validGenerators = new HashMap<>();

        for (Map.Entry<String, RelationshipMetadataGenerator> entry : generators.entrySet()) {
            String field = entry.getKey();

            if (!metadataFieldExists(context, field)) {
                log.error("Relationship configuration {} references a nonexistent additional metadata field: {}",
                          configurationId, field);
                continue;
            }

            // Validate the source field declared by the generator, which is read on the related item.
            // A generator backs an additional field, so a missing source field follows the additional
            // severity rule: log it and drop this generator instead of invalidating the configuration.
            String sourceField = entry.getValue().getSourceField();
            if (sourceField == null || !metadataFieldExists(context, sourceField)) {
                log.error("Relationship configuration {} references a generator for {} with a nonexistent " +
                              "source metadata field: {} on the {} endpoint",
                          configurationId, field, sourceField, side);
                continue;
            }

            validGenerators.put(field, entry.getValue());
        }

        return validGenerators;
    }

    /**
     * Checks whether a metadata field exists in the metadata registry.
     *
     * @return {@code true} if the field exists, {@code false} otherwise
     * @throws SQLException if an error occurs while accessing the metadata registry
     */
    private boolean metadataFieldExists(Context context, String field) throws SQLException {
        return metadataFieldService.findByString(context, field, '.') != null;
    }

    @Override
    public RelationshipTypeConfiguration findForMetadataValue(Context context, MetadataValue value)
        throws SQLException {

        String field = fieldName(value);
        String ownerEntityType = itemService.getEntityTypeLabel((Item) value.getDSpaceObject());

        RelationshipTypeConfiguration match = null;

        for (RelationshipTypeConfiguration candidate : configurations) {
            if (candidate.mapsField(field, ownerEntityType)) {
                if (match != null) {
                    throw new IllegalArgumentException(
                        "Ambiguous relationship configuration for field " + field + " and entity type " +
                            ownerEntityType);
                }
                match = candidate;
            }
        }

        // Use the explicit mapping when it applies to this owner. Otherwise, retain the legacy linked-entity
        // authority fallback, even if a more specific Spring mapping uses the same authority:* key. Resolution
        // of persisted Relationships must also consider their endpoint types (see resolveConfiguration below).
        RelationshipTypeConfiguration configuration = match != null ? match : fromLinkedEntityAuthority(field);

        // Validate metadata fields against the registry before returning an explicit or authority-derived
        // configuration.
        if (configuration != null) {
            configuration = validateMetadataFields(context, configuration);
        }

        return configuration;
    }

    /**
     * Resolves a configuration for a persisted Relationship or a proposed pair of endpoints.
     *
     * An explicit Spring configuration takes precedence when its entity-type constraints fit the endpoints.
     * For authority:* keys, a different owner entity type can still use the linked-entity authority fallback
     * under the same persisted key. Resolving by key AND endpoints prevents that fallback from later being
     * interpreted as the unrelated explicit Spring definition.
     *
     * @param context  current DSpace context
     * @param key      relationship configuration key
     * @param left     left endpoint, or null when endpoint types are not available
     * @param right    right endpoint, or null when endpoint types are not available
     * @param validate whether to validate fields against the metadata registry
     * @return the applicable relationship configuration
     * @throws IllegalArgumentException if the configuration key is missing or not resolvable
     * @throws SQLException if an error occurs while accessing the metadata registry
     */
    private RelationshipTypeConfiguration resolveConfiguration(Context context, String key, Item left, Item right,
                                                              boolean validate) throws SQLException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Missing relationship configuration key");
        }
        RelationshipTypeConfiguration match = null;
        for (RelationshipTypeConfiguration candidate : configurations) {
            if (key.equals(candidate.getId())) {
                if (match != null) {
                    throw new IllegalArgumentException("Duplicate relationship configuration key " + key);
                }
                match = candidate;
            }
        }

        if (key.startsWith(AUTHORITY_PREFIX)
            && (match == null || !matchesEndpointTypes(match, left, right))) {
            RelationshipTypeConfiguration fallback =
                fromLinkedEntityAuthority(key.substring(AUTHORITY_PREFIX.length()));
            if (fallback != null && matchesEndpointTypes(fallback, left, right)) {
                match = fallback;
            } else if (match != null) {
                throw new IllegalArgumentException(
                    "Relationship configuration " + key + " does not support the relationship endpoints");
            }
        }
        if (match == null) {
            throw new IllegalArgumentException("Unknown relationship configuration " + key);
        }

        // Only normal resolution checks the registry. Primary/additional classification remains lightweight.
        return validate ? validateMetadataFields(context, match) : match;
    }

    /**
     * Returns whether the configured entity types permit the supplied endpoints.
     * Missing endpoints or unknown entity types do not disambiguate a key; validation of concrete endpoints
     * is still performed separately by validate().
     */
    private boolean matchesEndpointTypes(RelationshipTypeConfiguration configuration, Item left, Item right) {
        if (left == null || right == null) {
            return true;
        }
        String leftType = itemService.getEntityTypeLabel(left);
        String rightType = itemService.getEntityTypeLabel(right);
        return (leftType == null || configuration.getLeftEntityTypes().isEmpty()
                || configuration.getLeftEntityTypes().contains(leftType))
            && (rightType == null || configuration.getRightEntityTypes().isEmpty()
                || configuration.getRightEntityTypes().contains(rightType));
    }

    @Override
    public RelationshipTypeConfiguration getRelationshipConfig(Context context, Relationship relationship)
        throws SQLException {
        return resolveConfiguration(context, relationship.getRelationshipConfigKey(),
                                    relationship.getLeftItem(), relationship.getRightItem(), true);
    }

    /**
     * Builds a relationship configuration for an authority field linked to an entity type.
     *
     * @return the relationship configuration, or {@code null} if the field is not configured
     *         as a linked entity authority
     */
    private RelationshipTypeConfiguration fromLinkedEntityAuthority(String field) {
        String fieldKey = field.replace('.', '_');
        if (!choiceAuthorityService.isChoicesConfigured(fieldKey, Constants.ITEM, (Collection) null)) {
            return null;
        }
        String[] targetTypes = choiceAuthorityService.getLinkedEntityTypes(fieldKey);
        if (targetTypes == null || targetTypes.length == 0) {
            return null;
        }

        // Validate the dynamically resolved authority field, which bypasses startup validation.
        validateMetadataField(AUTHORITY_PREFIX + field, "left", field, true);

        RelationshipTypeConfiguration configuration = new RelationshipTypeConfiguration();
        configuration.setId(AUTHORITY_PREFIX + field);
        configuration.setLeftPrimaryMetadataField(field);
        // Allow all entity types supported by the configured authority.
        // Use an explicit Spring definition for custom relationship configurations or inverse anchors.
        configuration.setRightEntityTypes(Set.of(targetTypes));
        return configuration;
    }

    @Override
    public void validate(Context context, RelationshipTypeConfiguration configuration, Item left, Item right)
        throws SQLException {
        validate(context, configuration, left, right, false);
    }

    @Override
    public void validate(Context context, RelationshipTypeConfiguration configuration, Item left, Item right,
                         boolean allowUnarchivedItems) throws SQLException {
        if (left == null || right == null || configuration == null || configuration.getId() == null) {
            throw new IllegalArgumentException("A relationship requires two items and configured semantics");
        }
        // Allow unarchived Items when copying relationships to a new Item version.
        if (!allowUnarchivedItems && (!left.isArchived() || !right.isArchived())) {
            throw new IllegalArgumentException("Concrete relationships require two archived items");
        }
        validateType(configuration.getLeftEntityTypes(), left, "left");
        validateType(configuration.getRightEntityTypes(), right, "right");
    }

    @Override
    public void validateForCreation(Context context, RelationshipTypeConfiguration configuration, Item left, Item right,
                                    boolean allowUnarchivedItems) throws SQLException {
        if (configuration == null || configuration.getId() == null) {
            throw new IllegalArgumentException("A registered relationship configuration is required for creation");
        }

        // Creation may receive a configuration resolved earlier from a MetadataValue, but the persisted key
        // must resolve to the same endpoint and primary-field semantics. This is particularly important for
        // authority:* keys, where an explicit bean can override a legacy authority-derived configuration.
        RelationshipTypeConfiguration registered = resolveConfiguration(context, configuration.getId(),
                                                                       left, right, true);
        if (!configuration.getLeftEntityTypes().equals(registered.getLeftEntityTypes())
            || !configuration.getRightEntityTypes().equals(registered.getRightEntityTypes())
            || !Objects.equals(configuration.getLeftPrimaryMetadataField(),
                               registered.getLeftPrimaryMetadataField())
            || !Objects.equals(configuration.getRightPrimaryMetadataField(),
                               registered.getRightPrimaryMetadataField())) {
            throw new IllegalArgumentException(
                "Relationship configuration " + configuration.getId()
                    + " does not match the registered configuration for its persisted key");
        }

        // Use the registered definition for endpoint validation, not an arbitrary caller-provided object.
        validate(context, registered, left, right, allowUnarchivedItems);
    }

    @Override
    public Optional<String> getPrimaryMetadataField(Context context, Relationship relationship, Item endpoint)
        throws SQLException {
        RelationshipTypeConfiguration configuration = getRelationshipConfig(context, relationship);
        return isLeftEndpoint(relationship, endpoint)
            ? configuration.getLeftPrimaryMetadataField()
            : configuration.getRightPrimaryMetadataField();
    }

    @Override
    public boolean isPrimaryMetadata(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException {

        RelationshipTypeConfiguration configuration =
            resolveConfiguration(context, relationship.getRelationshipConfigKey(),
                                 relationship.getLeftItem(), relationship.getRightItem(), false);

        Item endpoint = (Item) metadataValue.getDSpaceObject();

        Optional<String> primaryField = isLeftEndpoint(relationship, endpoint)
            ? configuration.getLeftPrimaryMetadataField()
            : configuration.getRightPrimaryMetadataField();

        return primaryField.filter(fieldName(metadataValue)::equals).isPresent();
    }

    @Override
    public boolean isAdditionalMetadata(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException {

        RelationshipTypeConfiguration configuration =
            resolveConfiguration(context, relationship.getRelationshipConfigKey(),
                                 relationship.getLeftItem(), relationship.getRightItem(), false);

        Item endpoint = (Item) metadataValue.getDSpaceObject();

        Set<String> additionalFields = isLeftEndpoint(relationship, endpoint)
            ? configuration.getLeftAdditionalMetadataFields()
            : configuration.getRightAdditionalMetadataFields();

        return additionalFields.contains(fieldName(metadataValue));
    }

    @Override
    public Set<String> getAdditionalMetadataFields(Context context, Relationship relationship, Item endpoint)
        throws SQLException {
        RelationshipTypeConfiguration configuration = getRelationshipConfig(context, relationship);
        return isLeftEndpoint(relationship, endpoint)
            ? configuration.getLeftAdditionalMetadataFields()
            : configuration.getRightAdditionalMetadataFields();
    }

    @Override
    public Optional<RelationshipMetadataGenerator> getMetadataGenerator(Context context,
                                                                        Relationship relationship, Item endpoint,
                                                                        String metadataField)
        throws SQLException {

        RelationshipTypeConfiguration configuration = getRelationshipConfig(context, relationship);

        Map<String, RelationshipMetadataGenerator> generators =
            isLeftEndpoint(relationship, endpoint)
                ? configuration.getLeftMetadataGenerators()
                : configuration.getRightMetadataGenerators();

        return Optional.ofNullable(generators.get(metadataField));
    }

    /**
     * Determines whether the given Item is the left or right endpoint of the relationship.
     *
     * @throws IllegalArgumentException if the Item is not an endpoint of the relationship
     */
    private boolean isLeftEndpoint(Relationship relationship, Item endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Relationship endpoint cannot be null");
        }

        if (endpoint.getID().equals(relationship.getLeftItem().getID())) {
            return true;
        }

        if (endpoint.getID().equals(relationship.getRightItem().getID())) {
            return false;
        }

        throw new IllegalArgumentException(
            "Item " + endpoint.getID() + " is not an endpoint of relationship " + relationship.getID());
    }

    private void validateType(Set<String> allowed, Item item, String side) {
        if (!allowed.isEmpty() && !allowed.contains(itemService.getEntityTypeLabel(item))) {
            throw new IllegalArgumentException("Item type is not allowed on the " + side + " side of the relationship");
        }
    }

    public static String fieldName(MetadataValue value) {
        return value.getSchema() + "." + value.getElement()
            + (value.getQualifier() == null ? "" : "." + value.getQualifier());
    }
}
