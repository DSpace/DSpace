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
 * Compound operations for stored metadata and real internal relationships.
 * Every operation participates in the caller's Context transaction; none commits.
 * Relationships can have multiple projections. Matching endpoints alone never
 * causes two independent relationship occurrences to be merged.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public interface AuthorityBackedRelationshipService {
    /**
     * Promote a resolved reference, returning null while either endpoint is unarchived or the field is unmapped.
     */
    Relationship promoteResolvedAuthority(Context context, Item owner, MetadataValue value, Item target)
        throws SQLException, AuthorizeException;

    /**
     * Associate an additional stored projection with an existing relationship.
     */
    void attachMetadataToRelationship(Context context, MetadataValue value, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Replace the opposite endpoint, keeping the owner and the relationship's identity.
     */
    void replaceRelatedObject(Context context, Item owner, Relationship relationship, Item newTarget)
        throws SQLException, AuthorizeException;

    /**
     * Remove the relationship but retain its metadata; prevent automatic re-resolution.
     */
    void detachRelationshipKeepMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Remove the relationship and all its stored projections as one logical operation.
     */
    void removeRelationshipAndMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException;

    /**
     * Remove only a dependent projection; removing the final anchor requires a logical deletion instead.
     */
    void removeMetadataProjection(Context context, MetadataValue value)
        throws SQLException, AuthorizeException;

    /**
     * Metadata-editing entry point. Removing the final configured anchor on either
     * side removes the logical relationship; other projections are removed alone.
     */
    void removeMetadataValue(Context context, MetadataValue value)
        throws SQLException, AuthorizeException;

    /**
     * Metadata-editing entry point for callers which are iterating an owner's metadata list.
     * The service will not mutate the in-memory metadata list of that owner; the caller must
     * remove affected values through its iterator instead. Metadata lists of other owners are
     * still kept in sync by this service.
     *
     * @param context                         current DSpace context
     * @param value                           metadata value to remove
     * @param metadataOwnerWithActiveIterator owner whose metadata list is currently being iterated
     * @throws SQLException                   if a database error occurs
     * @throws AuthorizeException             if the current user is not authorized to perform the removal
     */
    void removeMetadataValue(Context context, MetadataValue value, DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException;
}
