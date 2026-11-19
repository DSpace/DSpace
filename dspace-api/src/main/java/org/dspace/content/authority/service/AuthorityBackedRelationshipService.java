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
 * promote that reference to a durable {@link Relationship}. It also provides authority-aware operations for removing
 * relationship-backed metadata.
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
     * is returned. An existing relationship to another target is not silently replaced.
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
     * Remove a complete logical relationship together with all stored relationship metadata values which refer to it.
     *
     * This operation is intended for deleting the logical association itself. All relationship-backed metadata values
     * belonging to the relationship are removed, followed by the durable relationship.
     *
     * @param context current DSpace context
     * @param relationship relationship to remove together with its stored relationship metadata values
     * @throws SQLException if a database error occurs while deleting metadata or the relationship
     * @throws AuthorizeException if the current user is not authorized to modify the affected metadata owners or
     *         relationship endpoints
     * @throws IllegalArgumentException if the relationship is not a supported configuration-backed relationship
     */
    void removeRelationshipAndMetadata(Context context, Relationship relationship)
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
     * @param relationship relationship to detach from its relationship metadata values and remove
     * @throws SQLException if a database error occurs while updating metadata or deleting the relationship
     * @throws AuthorizeException if the current user is not authorized to modify the affected relationship metadata
     * values
     * @throws IllegalArgumentException if the relationship is not a supported configuration-backed relationship
     */
    void detachRelationshipKeepMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Remove relationship-backed metadata while a caller is actively iterating one owner's in-memory metadata list.
     *
     * Removing a relationship-backed primary metadata value removes the relationship together with all its
     * relationship-backed metadata values. This overload avoids modifying the in-memory metadata collection of
     * {@code metadataOwnerWithActiveIterator}. The caller is responsible for removing affected metadata values from
     * that owner's collection through its active iterator. In-memory metadata collections belonging to other owners
     * are still kept synchronized by this service.
     *
     * This overload exists to prevent concurrent modification of the caller's active metadata iterator.
     *
     * @param context current DSpace context
     * @param value relationship-backed metadata value to remove
     * @param metadataOwnerWithActiveIterator owner whose metadata collection is currently being iterated by the caller,
     *        or {@code null} when no metadata collection is being iterated
     * @throws SQLException if a database error occurs while evaluating or performing the removal
     * @throws AuthorizeException if the current user is not authorized to modify the affected metadata or relationship
     * @throws IllegalArgumentException if the relationship is not editable using the configured relationship lifecycle
     */
    void removeMetadataValue(Context context, MetadataValue value, DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException;
}
