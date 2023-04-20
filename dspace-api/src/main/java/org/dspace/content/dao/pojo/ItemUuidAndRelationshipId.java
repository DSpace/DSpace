/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.pojo;

import java.util.UUID;

import org.dspace.content.Relationship;
import org.dspace.content.dao.RelationshipDAO;
import org.springframework.lang.NonNull;

/**
 * Used by {@link RelationshipDAO#findByLatestItemAndRelationshipType} to avoid creating {@link Relationship}s.
 */
public class ItemUuidAndRelationshipId {

    private final UUID itemUuid;
    private final int relationshipId;

    public ItemUuidAndRelationshipId(@NonNull UUID itemUuid, @NonNull int relationshipId) {
        this.itemUuid = itemUuid;
        this.relationshipId = relationshipId;
    }

    public UUID getItemUuid() {
        return this.itemUuid;
    }

    public int getRelationshipId() {
        return this.relationshipId;
    }

}
