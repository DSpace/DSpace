/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipTypeConfiguration;
import org.dspace.core.Context;

/**
 * Resolves and validates the configured semantics of configuration-backed relationships.
 *
 * Relationship behavior is defined in configuration rather than stored as a
 * {@link org.dspace.content.RelationshipType} database entity. A persisted {@link org.dspace.content.Relationship}
 * may store a configuration key identifying which configured semantics apply to that relationship.
 *
 * Implementations may also provide compatibility mappings from existing authority configuration, for example while
 * migrating existing CRIS authority-backed relationships to the configuration-backed relationship model.
 */
public interface RelationshipConfigurationService {

    /**
     * Resolves the relationship configuration applicable to the supplied metadata value.
     *
     * The metadata field is used to determine whether it participates in a configured relationship. If no
     * relationship configuration applies to the metadata value, this method returns {@code null}.
     *
     * @param value metadata value for which relationship configuration should be resolved
     * @return the matching relationship configuration, or {@code null} if the metadata value is not mapped to a
     * configured relationship
     * @throws IllegalArgumentException if multiple configurations match the metadata field
     */
    RelationshipTypeConfiguration findForMetadataValue(MetadataValue value);

    /**
     * Resolves a relationship configuration by its stable configuration key.
     *
     * The key identifies configured relationship semantics; it is not itself a persisted relationship type definition.
     *
     * @param key stable identifier of the relationship configuration
     * @return the relationship configuration identified by the supplied key
     * @throws IllegalArgumentException if the key is missing, unknown or ambiguous
     */
    RelationshipTypeConfiguration getByKey(String key);

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
}
