/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.core.Context;

/**
 * Service for authority-aware operations which connect stored metadata to durable internal DSpace relationships.
 *
 * Authority-backed metadata can represent a reference before a concrete DSpace-to-DSpace relationship exists. Once
 * an authority value resolves to an internal {@link Item}, this service can validate the authority-specific state and
 * promote that reference to a durable {@link Relationship}. It also provides authority-aware operations for attaching
 * metadata to an existing relationship, replacing a related Item, and removing or detaching relationship-backed
 * metadata.
 *
 * Authority-specific validation belongs in this service. Generic metadata/relationship persistence and lifecycle
 * operations may be delegated to the corresponding metadata/relationship service layer. Callers should use the
 * compound operations defined here instead of independently changing authority metadata and the durable relationship,
 * as doing so may leave the two representations inconsistent.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public interface AuthorityBackedRelationshipService {

    /**
     * Promote an authority-backed metadata reference to a durable internal relationship.
     *
     * Promotion is only applicable when both Items are archived and the metadata value is eligible for promotion. If
     * promotion is not currently applicable, or if the metadata field is not configured as a relationship field, this
     * method may return {@code null} without creating a relationship.
     *
     * If the metadata value is already associated with the relationship to the same target, the existing relationship
     * is returned. An existing relationship to another target is not silently replaced; callers must use
     * {@link #replaceRelatedObject(Context, Item, Relationship, Item)} for that operation.
     *
     * @param context current DSpace context
     * @param owner Item which owns the metadata value and should remain one endpoint of the relationship
     * @param value authority-backed metadata value to promote
     * @param target resolved internal Item which should become the related endpoint
     * @return the existing or newly created relationship, or {@code null} when promotion is not applicable
     * @throws SQLException if a database error occurs while reading or persisting the relationship
     * @throws AuthorizeException if the current user is not authorized to perform the required Item operations
     * @throws IllegalArgumentException if the metadata owner, existing relationship, or authority is inconsistent with
     *         the supplied Items
     */
    Relationship promoteResolvedAuthority(Context context, Item owner, MetadataValue value, Item target)
        throws SQLException, AuthorizeException;

    /**
     * Associate an existing stored metadata value with an existing durable relationship.
     *
     * Before attaching the metadata value, authority-specific information is checked against the relationship. In
     * particular, when the metadata authority is an internal Item UUID, it must identify the Item at the opposite
     * endpoint of the relationship. The metadata value must also be owned by one of the relationship endpoints.
     *
     * Attaching a metadata value adds another stored metadata projection of the same logical relationship; it does not
     * create another relationship occurrence. A value which is already linked to a different relationship cannot be
     * reassigned implicitly through this method.
     *
     * @param context current DSpace context
     * @param value stored metadata value to associate with the relationship
     * @param relationship existing durable relationship to associate with the metadata value
     * @throws SQLException if a database error occurs while reading or updating the metadata value
     * @throws AuthorizeException if the current user is not authorized to modify the metadata owner or read the
     *         related Item
     * @throws IllegalArgumentException if the relationship or metadata value is invalid, the metadata owner is not an
     *         endpoint, the authority conflicts with the relationship, or the metadata is already linked elsewhere
     */
    void attachMetadataToRelationship(Context context, MetadataValue value, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Replace the Item opposite {@code owner} while preserving the identity of an existing relationship.
     *
     * The supplied {@code owner} identifies the endpoint which remains unchanged. The opposite endpoint is replaced by
     * {@code newTarget}. Relationship configuration and existing metadata projections are validated before the
     * relationship is changed so unsupported two-sided or dependent projection cases are not left inconsistent.
     *
     * Metadata display text is preserved. Where authority metadata contains an internal Item UUID used as a cache of
     * the relationship target, that UUID is updated to the new target as part of the authority-aware operation.
     *
     * @param context current DSpace context
     * @param owner relationship endpoint which must remain unchanged
     * @param relationship relationship whose opposite endpoint should be replaced
     * @param newTarget new Item which should replace the endpoint opposite {@code owner}
     * @throws SQLException if a database error occurs while validating or updating the relationship or metadata
     * @throws AuthorizeException if the current user is not authorized to modify the retained endpoint, read the new
     *         target, or modify affected metadata projections
     * @throws IllegalArgumentException if {@code owner} is not a relationship endpoint, the new endpoint is invalid for
     *         the configured relationship, or existing projections cannot be safely retained
     */
    void replaceRelatedObject(Context context, Item owner, Relationship relationship, Item newTarget)
        throws SQLException, AuthorizeException;

    /**
     * Remove the durable relationship while retaining its stored metadata values.
     *
     * This is a detach operation rather than a logical metadata deletion. Metadata remains on the owning
     * Items, but their relationship association is removed. Authority state is adjusted as needed so an explicitly
     * detached internal reference is not immediately recreated by the normal authority-resolution workflow.
     *
     * Use this operation when the descriptive metadata should remain, but the concrete internal DSpace-to-DSpace link
     * should no longer exist.
     *
     * @param context current DSpace context
     * @param relationship relationship to detach from its metadata projections and remove
     * @throws SQLException if a database error occurs while updating metadata or deleting the relationship
     * @throws AuthorizeException if the current user is not authorized to modify the affected metadata projections
     * @throws IllegalArgumentException if the relationship is not a supported configuration-backed relationship
     */
    void detachRelationshipKeepMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Remove a complete logical relationship together with all stored metadata projections which refer to it.
     *
     * This operation is intended for deleting the logical association itself. All relationship-backed metadata values
     * belonging to the relationship are removed, followed by the durable relationship. This differs from
     * {@link #detachRelationshipKeepMetadata(Context, Relationship)}, which keeps the descriptive metadata.
     *
     * @param context current DSpace context
     * @param relationship relationship to remove together with its stored metadata projections
     * @throws SQLException if a database error occurs while deleting metadata or the relationship
     * @throws AuthorizeException if the current user is not authorized to modify the affected metadata owners or
     *         relationship endpoints
     * @throws IllegalArgumentException if the relationship is not a supported configuration-backed relationship
     */
    void removeRelationshipAndMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Remove a single non-anchor metadata projection without removing the durable relationship.
     *
     * A relationship may be represented by more than one stored metadata value. This method removes only the supplied
     * projection when the remaining metadata still contains the configured anchor required to represent the logical
     * relationship. It must not be used to remove the final anchor of a relationship; use
     * {@link #removeMetadataValue(Context, MetadataValue)} for normal metadata-editing semantics.
     *
     * @param context current DSpace context
     * @param value relationship-backed metadata projection to remove
     * @throws SQLException if a database error occurs while inspecting or deleting the metadata value
     * @throws AuthorizeException if the current user is not authorized to modify the metadata owner
     * @throws IllegalArgumentException if the value is the final configured anchor or its relationship cannot be edited
     */
    void removeMetadataProjection(Context context, MetadataValue value)
        throws SQLException, AuthorizeException;

    /**
     * Remove a relationship-backed metadata value using normal metadata-editing lifecycle semantics.
     *
     * If the supplied value is a dependent projection, or if another value still provides the configured anchor on the
     * same relationship side, only that metadata value is removed. If it is the final configured anchor, the complete
     * logical relationship and its stored projections are removed.
     *
     * This is the preferred removal entry point for callers editing metadata when they should not need to determine
     * whether deleting a value also requires deleting the relationship.
     *
     * @param context current DSpace context
     * @param value metadata value to remove
     * @throws SQLException if a database error occurs while evaluating or performing the removal
     * @throws AuthorizeException if the current user is not authorized to modify the affected metadata or relationship
     * @throws IllegalArgumentException if the relationship is not editable using the configured relationship lifecycle
     */
    void removeMetadataValue(Context context, MetadataValue value)
        throws SQLException, AuthorizeException;

    /**
     * Remove relationship-backed metadata while a caller is actively iterating one owner's in-memory metadata list.
     *
     * This overload has the same logical removal semantics as
     * {@link #removeMetadataValue(Context, MetadataValue)}, but avoids modifying the in-memory metadata collection of
     * {@code metadataOwnerWithActiveIterator}. The caller is responsible for removing affected metadata values from
     * that owner's collection through its active iterator. In-memory metadata collections belonging to other owners
     * are still kept synchronized by this service.
     *
     * This overload exists to prevent concurrent modification of the caller's active metadata iterator. Callers which
     * are not iterating the owner's metadata collection should use
     * {@link #removeMetadataValue(Context, MetadataValue)} instead.
     *
     * @param context current DSpace context
     * @param value metadata value to remove
     * @param metadataOwnerWithActiveIterator owner whose metadata collection is currently being iterated by the caller
     * @throws SQLException if a database error occurs while evaluating or performing the removal
     * @throws AuthorizeException if the current user is not authorized to modify the affected metadata or relationship
     * @throws IllegalArgumentException if the relationship is not editable using the configured relationship lifecycle
     */
    void removeMetadataValue(Context context, MetadataValue value, DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException;
}
