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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.core.Context;

/**
 * Service responsible for managing the logical combination of DSpace
 * {@link MetadataValue}s and {@link Relationship}s.
 * <p>
 * In the unified relationship model, a concrete internal relationship is stored
 * as a first-class {@link Relationship}, while one or more metadata values may
 * provide the metadata representation of that relationship.
 * </p>
 *
 * <p>
 * For example:
 * </p>
 *
 * <pre>
 * Publication
 *     dc.contributor.author = "Smith, Jane"
 *              |
 *              | relationship_id
 *              v
 * Relationship
 *     Publication -> Person
 * </pre>
 *
 * <p>
 * The {@link Relationship} is authoritative for the durable link between
 * DSpace objects. The {@link MetadataValue} remains authoritative for metadata
 * concerns such as:
 * </p>
 *
 * <ul>
 *     <li>the metadata field;</li>
 *     <li>the displayed value;</li>
 *     <li>language;</li>
 *     <li>place/order;</li>
 *     <li>authority/confidence information where applicable.</li>
 * </ul>
 *
 * <p>
 * Callers should normally use this service when an operation affects the
 * logical relationship as a whole, instead of modifying the
 * {@link MetadataValue} and {@link Relationship} independently.
 * </p>
 *
 * <p>
 * This allows submission, workflow, REST, migration and authority-resolution
 * code to work with one logical relationship operation without needing to know
 * how that operation is persisted internally.
 * </p>
 */
public interface MetadataRelationshipService {

    /**
     * Creates a new concrete internal relationship for an existing metadata
     * value.
     * <p>
     * This operation is used when both endpoints of the relationship are
     * already known. It creates the durable {@link Relationship} between the
     * owner and target Items and associates the supplied metadata value with
     * that relationship.
     * </p>
     *
     * <p>
     * The existing metadata value is preserved. Its value, language, place,
     * authority and confidence are not rewritten merely because the
     * relationship is created.
     * </p>
     *
     * <p>
     * Relationship configuration determines the semantics of the created
     * relationship, including the relationship type/configuration identifier
     * and the correct left/right orientation.
     * </p>
     *
     * @param context       current DSpace context
     * @param ownerItem     Item which owns the metadata value
     * @param metadataValue metadata value representing the relationship
     * @param targetItem    concrete Item referenced by the relationship
     * @return the newly created durable relationship
     * @throws SQLException       if the relationship or metadata association
     *                            cannot be persisted
     * @throws AuthorizeException if the current user is not authorized to
     *                            create the relationship
     */
    Relationship createInternalRelationship(
            Context context,
            Item ownerItem,
            MetadataValue metadataValue,
            Item targetItem
    ) throws SQLException, AuthorizeException;


    /**
     * Associates an additional metadata value with an existing relationship.
     * <p>
     * A relationship may have more than one metadata projection. For example,
     * an authorship relationship might be represented by a display-name
     * metadata value and one or more additional contextual metadata values.
     * </p>
     *
     * <pre>
     * Relationship 123
     *     Publication -> Person
     *          ^
     *          |
     *          +-- dc.contributor.author = "Smith, Jane"
     *          |
     *          +-- another relationship-backed metadata projection
     * </pre>
     *
     * <p>
     * This operation does not create a second relationship. It associates the
     * supplied metadata value with the existing relationship.
     * </p>
     *
     * <p>
     * The implementation should verify that the metadata value belongs to an
     * appropriate endpoint and that its metadata field is permitted by the
     * relationship configuration.
     * </p>
     *
     * @param context       current DSpace context
     * @param relationship relationship to which the metadata should be linked
     * @param metadataValue metadata value to associate with the relationship
     * @throws SQLException       if the association cannot be persisted
     * @throws AuthorizeException if the current user is not authorized to
     *                            modify the relationship or metadata
     */
    void attachMetadata(
            Context context,
            Relationship relationship,
            MetadataValue metadataValue
    ) throws SQLException, AuthorizeException;


    /**
     * Replaces one endpoint of an existing relationship with a new target Item.
     * <p>
     * This operation changes the durable internal link without implicitly
     * replacing the metadata representation of that link.
     * </p>
     *
     * <p>
     * For example:
     * </p>
     *
     * <pre>
     * Before:
     *
     * Relationship 123
     *     Publication -> Person A
     *
     * Metadata:
     *     dc.contributor.author = "Jane Smith"
     *
     * After:
     *
     * Relationship 123
     *     Publication -> Person B
     *
     * Metadata:
     *     dc.contributor.author = "Jane Smith"
     * </pre>
     *
     * <p>
     * Preserving the metadata value is intentional. Display metadata and the
     * concrete relationship target have separate responsibilities in the
     * unified model.
     * </p>
     *
     * <p>
     * Where additional metadata projections depend on the old related Item, the
     * implementation must either update those projections according to
     * configuration or reject the operation until it can be performed safely.
     * It must not silently leave target-dependent metadata inconsistent.
     * </p>
     *
     * @param context        current DSpace context
     * @param relationship   relationship whose endpoint should be replaced
     * @param retainedItem   relationship endpoint which should remain unchanged
     * @param newRelatedItem new Item which should replace the endpoint opposite the retained Item
     * @return the updated relationship
     * @throws SQLException       if the relationship cannot be updated
     * @throws AuthorizeException if the current user is not authorized to
     *                            modify the relationship
     */
    Relationship replaceTarget(
            Context context,
            Relationship relationship,
            Item retainedItem,
            Item newRelatedItem
    ) throws SQLException, AuthorizeException;


    /**
     * Removes the concrete internal relationship from a metadata value while
     * retaining the metadata itself.
     * <p>
     * This represents a deliberate transition from:
     * </p>
     *
     * <pre>
     * metadata + concrete internal relationship
     * </pre>
     *
     * <p>
     * to:
     * </p>
     *
     * <pre>
     * ordinary / unresolved metadata only
     * </pre>
     *
     * <p>
     * The metadata value, displayed text, language and place/order are
     * retained. Its {@code relationship_id} is cleared.
     * </p>
     *
     * <p>
     * Authority and confidence may also need to be adjusted so that the
     * metadata value is no longer interpreted as an immediately resolvable
     * internal relationship. The exact authority behavior should follow the
     * configured detach policy and existing DSpace authority semantics.
     * </p>
     *
     * <p>
     * If this metadata value is the final metadata anchor for the relationship,
     * the relationship may also be removed according to the configured
     * lifecycle rules.
     * </p>
     *
     * @param context       current DSpace context
     * @param metadataValue metadata value to detach
     * @throws SQLException       if the metadata or relationship cannot be
     *                            updated
     * @throws AuthorizeException if the current user is not authorized to
     *                            perform the operation
     */
    void detachRelationship(
            Context context,
            MetadataValue metadataValue
    ) throws SQLException, AuthorizeException;


    /**
     * Removes one metadata projection from a relationship.
     * <p>
     * Removing a secondary metadata projection does not necessarily remove the
     * underlying relationship.
     * </p>
     *
     * <p>
     * For example, if a relationship has both an author display value and an
     * additional generated identifier value, deleting only the identifier
     * should normally leave the authorship relationship intact.
     * </p>
     *
     * <p>
     * If the supplied metadata value represents the final configured anchor of
     * the logical relationship, removing it may result in the relationship and
     * its dependent projections being removed as one logical operation.
     * </p>
     *
     * @param context       current DSpace context
     * @param metadataValue metadata projection to remove
     * @throws SQLException       if the metadata or relationship cannot be
     *                            updated
     * @throws AuthorizeException if the current user is not authorized to
     *                            perform the operation
     */
    void removeMetadataValue(
            Context context,
            MetadataValue metadataValue
    ) throws SQLException, AuthorizeException;


    /**
     * Removes a complete logical relationship.
     * <p>
     * This removes the durable {@link Relationship} and processes all metadata
     * values linked to that relationship according to the relationship
     * configuration.
     * </p>
     *
     * <p>
     * Depending on the operation and configured policy, associated metadata
     * values may be:
     * </p>
     *
     * <ul>
     *     <li>deleted;</li>
     *     <li>detached and retained as ordinary metadata; or</li>
     *     <li>otherwise transformed according to the configured lifecycle
     *         rules.</li>
     * </ul>
     *
     * <p>
     * Callers should use this method for a logical user operation such as
     * "remove this author relationship", rather than independently deleting
     * metadata and relationship rows.
     * </p>
     *
     * @param context      current DSpace context
     * @param relationship relationship to remove
     * @throws SQLException       if the relationship or associated metadata
     *                            cannot be updated
     * @throws AuthorizeException if the current user is not authorized to
     *                            remove the relationship
     */
    void removeRelationship(
            Context context,
            Relationship relationship
    ) throws SQLException, AuthorizeException;


    /**
     * Returns the metadata values which currently refer to a relationship.
     * <p>
     * This is the inverse of {@code MetadataValue.relationship}. It should be
     * queried explicitly rather than represented as an eagerly loaded
     * collection on the {@link Relationship} entity.
     * </p>
     *
     * <p>
     * Avoiding an automatic Hibernate collection is important because a
     * relationship may eventually have multiple projections, and callers
     * should not be forced to load all metadata merely when loading a
     * relationship.
     * </p>
     *
     * @param context      current DSpace context
     * @param relationship relationship whose metadata should be returned
     * @return metadata values linked to the relationship, never {@code null}
     * @throws SQLException if the metadata cannot be read
     */
    List<MetadataValue> findMetadataByRelationship(
            Context context,
            Relationship relationship
    ) throws SQLException;


    /**
     * Determines whether the supplied metadata value is currently backed by a
     * concrete internal relationship.
     *
     * <p>
     * This is equivalent conceptually to checking whether the metadata value
     * has a non-null {@code relationship_id}, but callers should use the
     * service-level operation where that makes the intent clearer.
     * </p>
     *
     * @param metadataValue metadata value to inspect
     * @return {@code true} when the metadata value references a concrete
     *         relationship, otherwise {@code false}
     */
    boolean isRelationshipBacked(MetadataValue metadataValue);
}
