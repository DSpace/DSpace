/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipTypeConfiguration;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.core.Context;

/**
 * Resolves and validates the configured semantics of configuration-backed relationships.
 *
 * Relationship behavior is defined in configuration rather than stored as a
 * {@link org.dspace.content.RelationshipType} database entity. A persisted {@link org.dspace.content.Relationship}
 * may store a configuration key identifying which configured semantics apply to that relationship.
 *
 * Implementations may also resolve relationships created from the authority configuration while a repository moves
 * to configuration-backed relationships.
 */
public interface RelationshipConfigurationService {

    /**
     * Resolves the relationship configuration applicable to the supplied metadata value.
     *
     * The metadata field is used to determine whether it participates in a configured relationship. If no
     * relationship configuration applies to the metadata value, this method returns {@code null}.
     *
     * @param context current DSpace context
     * @param value   metadata value for which relationship configuration should be resolved
     * @return the matching relationship configuration, or {@code null} if the metadata value is not mapped to a
     * configured relationship
     * @throws IllegalArgumentException if multiple configurations match the metadata field
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    RelationshipTypeConfiguration findForMetadataValue(Context context, MetadataValue value) throws SQLException;

    /**
     * Resolves the configuration associated with a relationship using its persisted
     * configuration key.
     *
     * @param context      current DSpace context
     * @param relationship relationship whose configuration should be resolved
     * @return the resolved relationship configuration
     * @throws IllegalArgumentException           if the configuration key is missing or unknown
     * @throws RelationshipConfigurationException if a configured primary metadata field does not exist
     * @throws SQLException                       if an error occurs while accessing the metadata registry
     */
    RelationshipTypeConfiguration getRelationshipConfig(Context context, Relationship relationship) throws SQLException;

    /**
     * Validates that the supplied Items can be used as the left and right endpoints of a relationship using the given
     * configuration.
     *
     * Both Items must be archived. Validation is based on the endpoint requirements defined by the relationship
     * configuration, such as the expected entity types for the left and right Items.
     * This method validates the configured relationship semantics; authorization to create or modify the
     * Relationship is handled separately by the relationship service.
     *
     * @param context       current DSpace context
     * @param configuration relationship configuration to validate against
     * @param left          proposed left endpoint of the Relationship
     * @param right         proposed right endpoint of the Relationship
     * @throws IllegalArgumentException if either endpoint does not satisfy the configured relationship requirements
     * @throws SQLException             if a database error occurs during validation
     */
    void validate(Context context, RelationshipTypeConfiguration configuration, Item left, Item right)
        throws SQLException;

    /**
     * Validates the relationship endpoints using the given configuration, optionally allowing unarchived Items.
     *
     * When {@code allowUnarchivedItems} is true, the archived-state requirement is skipped for both Items.
     * All other configured endpoint requirements, including entity types, are still validated.
     * Authorization is handled separately by the relationship service.
     *
     * @param context              current DSpace context
     * @param configuration        relationship configuration to validate against
     * @param left                 proposed left endpoint of the Relationship
     * @param right                proposed right endpoint of the Relationship
     * @param allowUnarchivedItems whether to allow unarchived Items during validation
     * @throws IllegalArgumentException if either endpoint does not satisfy the configured relationship requirements
     * @throws SQLException             if a database error occurs during validation
     */
    void validate(Context context, RelationshipTypeConfiguration configuration, Item left, Item right,
                  boolean allowUnarchivedItems) throws SQLException;

    /**
     * Validates a configuration-backed relationship before persistence, including the configuration key.
     * The supplied configuration must resolve to registered semantics with the same endpoint entity types
     * and primary metadata fields. This prevents persisting a key that would later resolve differently.
     * Registry validation is retained for primary fields, and missing additional fields are filtered as usual.
     *
     * @param context              current DSpace context
     * @param configuration        previously resolved relationship configuration
     * @param left                 left relationship endpoint
     * @param right                right relationship endpoint
     * @param allowUnarchivedItems whether unarchived endpoints are allowed when copying a version
     * @throws IllegalArgumentException if the key is unknown or its registered semantics differ
     * @throws SQLException             if a metadata registry lookup fails
     */
    void validateForCreation(Context context, RelationshipTypeConfiguration configuration, Item left, Item right,
                             boolean allowUnarchivedItems) throws SQLException;

    /**
     * Checks whether the supplied metadata value belongs to the primary metadata field
     * configured for its endpoint in the relationship.
     *
     * @param context       current DSpace context
     * @param relationship  relationship containing the metadata value's Item
     * @param metadataValue metadata value to check
     * @return {@code true} if the metadata field is primary on the corresponding endpoint,
     * {@code false} otherwise
     * @throws IllegalArgumentException if the relationship configuration is invalid or the metadata value
     *                                  does not belong to a relationship endpoint
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    boolean isPrimaryMetadata(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException;

    /**
     * Checks whether the supplied relationship-backed metadata value may be removed.
     *
     * A value is deletable when it is the primary metadata value of its relationship under a resolvable
     * configuration. A value whose configuration key is unknown is also deletable, so that relationships whose
     * configuration has disappeared can still be cleaned up. A resolvable additional (enhancer-managed) value is not
     * deletable: those values are owned by the enhancer and must survive user-facing removals.
     *
     * Other resolution failures raise the same errors as normal configuration resolution: missing or duplicate
     * keys and authority-derived configurations that do not support the relationship endpoints.
     *
     * @param context       current DSpace context
     * @param relationship  relationship containing the metadata value's Item
     * @param metadataValue metadata value to check
     * @return {@code true} if the value may be removed, {@code false} otherwise
     * @throws IllegalArgumentException if the relationship configuration is invalid
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    boolean isMetadataDeletable(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException;

    /**
     * Returns the primary metadata field configured for the specified relationship endpoint.
     * A relationship endpoint may have no primary metadata field configured.
     *
     * @param context      current DSpace context
     * @param relationship relationship containing the endpoint
     * @param endpoint     Item for which to retrieve the primary metadata field
     * @return the configured primary metadata field, or {@link Optional#empty()} if none is configured
     * @throws IllegalArgumentException if the relationship configuration is invalid or the Item
     *                                  is not an endpoint of the relationship
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    Optional<String> getPrimaryMetadataField(Context context, Relationship relationship, Item endpoint)
        throws SQLException;

    /**
     * Checks whether the supplied metadata value belongs to an additional metadata field
     * configured for its endpoint in the relationship.
     *
     * Additional metadata fields are derived from the keys of the metadata generator
     * map configured for that endpoint.
     *
     * @param context       current DSpace context
     * @param relationship  relationship containing the metadata value's Item
     * @param metadataValue metadata value to check
     * @return {@code true} if the metadata field is additional on the corresponding endpoint,
     * {@code false} otherwise
     * @throws IllegalArgumentException if the relationship configuration is invalid or the metadata value
     *                                  does not belong to a relationship endpoint
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    boolean isAdditionalMetadata(Context context, Relationship relationship, MetadataValue metadataValue)
        throws SQLException;

    /**
     * Returns the additional metadata fields configured for the specified relationship endpoint.
     *
     * The fields are derived from the keys of the metadata generator map configured
     * for that endpoint.
     *
     * @param context      current DSpace context
     * @param relationship relationship containing the endpoint
     * @param endpoint     Item for which to retrieve additional metadata fields
     * @return the configured additional metadata fields, or an empty set if none are configured
     * @throws IllegalArgumentException if the relationship configuration is invalid or the Item
     *                                  is not an endpoint of the relationship
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    Set<String> getAdditionalMetadataFields(Context context, Relationship relationship, Item endpoint)
        throws SQLException;

    /**
     * Resolves the metadata generator configured for an additional metadata field
     * on the specified relationship endpoint.
     *
     * @param context       current DSpace context
     * @param relationship  relationship containing the endpoint
     * @param endpoint      Item for which to retrieve the metadata generator
     * @param metadataField additional metadata field for which to retrieve the generator,
     *                      in schema.element.qualifier format
     * @return the configured metadata generator, or {@link Optional#empty()} if none is configured
     * for the supplied field and endpoint
     * @throws IllegalArgumentException if the relationship configuration is invalid or the Item
     *                                  is not an endpoint of the relationship
     * @throws SQLException             if an error occurs while accessing the metadata registry
     */
    Optional<RelationshipMetadataGenerator> getMetadataGenerator(Context context, Relationship relationship,
                                                                 Item endpoint, String metadataField)
        throws SQLException;
}
