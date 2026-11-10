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
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;
import org.dspace.content.authority.service.ChoiceAuthorityService;
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

    /**
     * Validates all explicitly configured relationship definitions at startup.
     *
     * @throws RelationshipConfigurationException if a configured metadata field has invalid syntax
     * @throws IllegalArgumentException if a relationship configuration contains other invalid values
     */
    @PostConstruct
    public void validateConfigurations() {
        Set<String> ids = new HashSet<>();

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
        }
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
        Map<String, ItemAuthorityExtraMetadataGenerator> generators) {

        // Validate the primary metadata field, if configured.
        primaryField.ifPresent(field -> validateMetadataField(configurationId, side, field, true));

        for (Map.Entry<String, ItemAuthorityExtraMetadataGenerator> entry : generators.entrySet()) {
            String field = entry.getKey();

            // Validate each additional metadata field.
            validateMetadataField(configurationId, side, field, false);

            // Prevent a primary metadata field from also being configured as additional metadata.
            if (primaryField.filter(field::equals).isPresent()) {
                throw new IllegalArgumentException(
                    "Primary metadata field cannot also be additional metadata in relationship configuration "
                        + configurationId + " on the " + side + " endpoint: " + field);
            }
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
        if (field == null || !field.matches("[^.\\s]+\\.[^.\\s]+(\\.[^.\\s]+)?")) {
            throw new RelationshipConfigurationException(
                "Invalid " + (isPrimary ? "primary" : "additional") + " metadata field '" + field +
                    "' in relationship configuration " + configurationId + " on the " + side + " endpoint");
        }
    }

    /**
     * Validates that primary metadata fields exist in the metadata registry and filters out
     * additional metadata fields that are not registered.
     *
     * Missing primary metadata fields invalidate the relationship configuration.
     * Missing additional metadata fields are logged and excluded from the resolved configuration.
     *
     * @param context       current DSpace context
     * @param configuration relationship configuration to validate
     * @return the original configuration if all fields are valid, or a filtered configuration
     *         containing only registered additional metadata fields
     * @throws RelationshipConfigurationException if a configured primary metadata field does not exist
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

        Map<String, ItemAuthorityExtraMetadataGenerator> leftGenerators =
            filterMetadataGenerators(context, configuration.getId(), configuration.getLeftMetadataGenerators());

        Map<String, ItemAuthorityExtraMetadataGenerator> rightGenerators =
            filterMetadataGenerators(context, configuration.getId(), configuration.getRightMetadataGenerators());

        // Return the original configuration if no additional fields were removed.
        if (leftGenerators.size() == configuration.getLeftMetadataGenerators().size()
            && rightGenerators.size() == configuration.getRightMetadataGenerators().size()) {
            return configuration;
        }

        // Create a filtered copy
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
     * Filters out metadata generators referencing unregistered additional metadata fields.
     * Missing fields are logged as errors without invalidating the relationship configuration.
     *
     * @return a map containing only generators for registered metadata fields
     * @throws SQLException if an error occurs while accessing the metadata registry
     */
    private Map<String, ItemAuthorityExtraMetadataGenerator> filterMetadataGenerators(
        Context context,
        String configurationId,
        Map<String, ItemAuthorityExtraMetadataGenerator> generators) throws SQLException {

        Map<String, ItemAuthorityExtraMetadataGenerator> validGenerators = new HashMap<>();

        for (Map.Entry<String, ItemAuthorityExtraMetadataGenerator> entry : generators.entrySet()) {
            String field = entry.getKey();

            if (metadataFieldExists(context, field)) {
                validGenerators.put(field, entry.getValue());
            } else {
                log.error("Relationship configuration {} references a nonexistent additional metadata field: {}",
                          configurationId, field);
            }
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
        String[] parts = field.split("\\.", -1);
        String qualifier = parts.length == 3 ? parts[2] : null;

        return metadataFieldService.findByElement(context, parts[0], parts[1], qualifier) != null;
    }

    @Override
    public RelationshipTypeConfiguration findForMetadataValue(Context context, MetadataValue value)
        throws SQLException {
        String field = fieldName(value);
        RelationshipTypeConfiguration match = null;
        for (RelationshipTypeConfiguration candidate : configurations) {
            if (candidate.mapsField(field)) {
                if (match != null) {
                    throw new IllegalArgumentException("Ambiguous relationship configuration for field " + field);
                }
                match = candidate;
            }
        }

        RelationshipTypeConfiguration configuration = match != null ? match : fromLinkedEntityAuthority(field);

        // Validate metadata fields against the registry before returning an explicit or authority-derived
        // configuration.
        if (configuration != null) {
            configuration = validateMetadataFields(context, configuration);
        }

        return configuration;
    }

    @Override
    public RelationshipTypeConfiguration getByKey(Context context, String key) throws SQLException {
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
        if (match == null && key.startsWith(AUTHORITY_PREFIX)) {
            match = fromLinkedEntityAuthority(key.substring(AUTHORITY_PREFIX.length()));
        }
        if (match == null) {
            throw new IllegalArgumentException("Unknown relationship configuration " + key);
        }

        // Validate metadata fields against the registry before returning the resolved configuration.
        return validateMetadataFields(context, match);
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
    public Optional<String> getPrimaryMetadataField(Context context, Relationship relationship, Item endpoint)
        throws SQLException {
        RelationshipTypeConfiguration configuration = getByKey(context, relationship.getRelationshipConfigKey());
        return isLeftEndpoint(relationship, endpoint)
            ? configuration.getLeftPrimaryMetadataField()
            : configuration.getRightPrimaryMetadataField();
    }

    @Override
    public boolean isPrimaryMetadata(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException {
        return getPrimaryMetadataField(context, relationship, (Item) metadataValue.getDSpaceObject())
            .filter(fieldName(metadataValue)::equals)
            .isPresent();
    }

    @Override
    public boolean isAdditionalMetadata(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException {
        return getAdditionalMetadataFields(context, relationship, (Item) metadataValue.getDSpaceObject())
            .contains(fieldName(metadataValue));
    }

    @Override
    public Set<String> getAdditionalMetadataFields(Context context, Relationship relationship, Item endpoint)
        throws SQLException {
        RelationshipTypeConfiguration configuration = getByKey(context, relationship.getRelationshipConfigKey());
        return isLeftEndpoint(relationship, endpoint)
            ? configuration.getLeftAdditionalMetadataFields()
            : configuration.getRightAdditionalMetadataFields();
    }

    @Override
    public Optional<ItemAuthorityExtraMetadataGenerator> getMetadataGenerator(Context context,
                                                                              Relationship relationship, Item endpoint,
                                                                              String metadataField)
        throws SQLException {

        RelationshipTypeConfiguration configuration = getByKey(context, relationship.getRelationshipConfigKey());

        Map<String, ItemAuthorityExtraMetadataGenerator> generators =
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
