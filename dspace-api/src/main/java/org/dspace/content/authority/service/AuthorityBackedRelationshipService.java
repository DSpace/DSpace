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
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.core.Context;

/**
 * Service that mints authority-backed relationships from a resolved authority
 * value. This is the single place where such relationships are born, so that
 * the system path (reference-token resolution) and the user path
 * (directly-picked UUID) cannot drift apart.
 *
 * @author DSpace
 */
public interface AuthorityBackedRelationshipService {

    /**
     * Ensure that the given owning metadata value has an authority-backed
     * relationship to the resolved related item.
     * <p>
     * If the owning metadata value already carries a relationship id (see
     * {@link MetadataValue#getOwnedRelationshipId()}), this method is a no-op
     * and returns the existing relationship (idempotency). Otherwise a
     * type-less relationship is minted (owner &rarr; left, related &rarr;
     * right) and the owning metadata value's relationship id is set to the id
     * of the new row.
     * </p>
     * <p>
     * This method never modifies the owning metadata value's {@code value} or
     * {@code authority}; stamping the authority is the caller's concern.
     * </p>
     *
     * @param context           the DSpace context
     * @param ownerItem         the item that owns the metadata value
     * @param ownerMetadataValue the owning metadata value (its relationship id
     *                          is read and possibly written)
     * @param relatedItem       the resolved target item
     * @return the existing relationship if one was already linked, otherwise
     *         the newly minted type-less relationship
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if the current user may not create the relationship
     */
    Relationship createRelationshipForResolvedAuthority(Context context, Item ownerItem,
        MetadataValue ownerMetadataValue, Item relatedItem) throws SQLException, AuthorizeException;

}
