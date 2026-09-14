/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of {@link AuthorityBackedRelationshipService}. It is
 * the only place authority-backed relationship rows are minted, keeping the
 * system path and the user path aligned.
 *
 * @author DSpace
 */
public class AuthorityBackedRelationshipServiceImpl implements AuthorityBackedRelationshipService {

    private static final Logger log = LogManager.getLogger(AuthorityBackedRelationshipServiceImpl.class);

    @Autowired(required = true)
    protected RelationshipService relationshipService;

    @Autowired(required = true)
    protected MetadataValueService metadataValueService;

    @Override
    public Relationship createRelationshipForResolvedAuthority(Context context, Item ownerItem,
        MetadataValue ownerMetadataValue, Item relatedItem) throws SQLException, AuthorizeException {

        Integer existingRelationshipId = ownerMetadataValue.getOwnedRelationshipId();
        if (existingRelationshipId != null) {
            Relationship existing = relationshipService.find(context, existingRelationshipId);
            if (existing != null) {
                return existing;
            }
            log.warn("Owning metadata value {} referenced relationship id {} which no longer exists; re-minting",
                ownerMetadataValue.getID(), existingRelationshipId);
        }

        Relationship relationship = relationshipService.createTypeLessRelationship(context, ownerItem, relatedItem);

        ownerMetadataValue.setOwnedRelationshipId(relationship.getID());
        metadataValueService.update(context, ownerMetadataValue);

        return relationship;
    }

}
