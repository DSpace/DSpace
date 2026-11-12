/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.audit.MetadataEvent;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipConfigurationServiceImpl;
import org.dspace.content.RelationshipTypeConfiguration;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Relationship service for authority-backed metadata values, shared with CRIS integration.
 * Relationship stores endpoints and configuration identity; metadata references it.
 * There is no secondary-table ownership, implicit ORM cascade or background
 * synchronization between these storage parts.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class AuthorityBackedRelationshipServiceImpl implements AuthorityBackedRelationshipService {
    private static final Logger log = LogManager.getLogger(AuthorityBackedRelationshipServiceImpl.class);

    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private MetadataValueService metadataValueService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RelationshipConfigurationService relationshipConfigurationService;

    @Override
    public Relationship promoteResolvedAuthority(Context context, Item owner, MetadataValue value, Item target)
        throws SQLException, AuthorizeException {
        if (value == null || target == null) {
            return null;
        }
        requireOwner(value, owner);
        if (!owner.isArchived() || !target.isArchived()) {
            return null;
        }
        authorizeRelationshipOperation(context, owner, target);

        Relationship existing = value.getRelationship();
        if (existing != null) {
            // Return an existing relationship only if it points to the same target.
            if (!opposite(existing, owner).getID().equals(target.getID())) {
                throw new IllegalArgumentException(
                        "An existing relationship to another target cannot be replaced");
            }
            validateAuthority(value, existing);
            return existing;
        }
        RelationshipTypeConfiguration configuration =
            relationshipConfigurationService.findForMetadataValue(context, value);
        if (configuration == null) {
            return null;
        }
        boolean ownerOnLeft = configuration.isOwnerOnLeft(RelationshipConfigurationServiceImpl.fieldName(value),
                                                          itemService.getEntityTypeLabel(owner));
        Item left = ownerOnLeft ? owner : target;
        Item right = ownerOnLeft ? target : owner;
        // Validate before persistence, so invalid input does not leave a half-created link.
        validateAuthorityTarget(value, target);
        Relationship relationship =
            relationshipService.createConfigBackedRelationship(context, left, right, configuration);
        value.setRelationship(relationship);
        metadataValueService.update(context, value);
        itemService.update(context, owner);
        log.debug("Linked metadata {} to relationship {} using configuration {}",
            value.getID(), relationship.getID(), configuration.getId());
        return relationship;
    }

    @Override
    public void detachRelationshipKeepMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException {
        requireConfiguredRelationship(relationship);
        List<MetadataValue> values = metadataValueService.findByRelationship(context, relationship);
        authorizeRelationshipMetadataValues(context, values);
        for (MetadataValue value : values) {
            value.setRelationship(null);
            if (isUuid(value.getAuthority())) {
                value.setAuthority(null);
            }
            // Explicit detachment is not an invitation to resolve again at next update.
            value.setConfidence(Choices.CF_REJECTED);
            itemService.update(context, (Item) value.getDSpaceObject());
            metadataValueService.update(context, value);
        }
        relationshipService.delete(context, relationship);
    }

    @Override
    public void removeRelationshipAndMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException {
        removeRelationshipAndMetadata(context, relationship, null);
    }

    private void removeRelationshipAndMetadata(Context context, Relationship relationship,
                                               DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException {
        requireConfiguredRelationship(relationship);
        List<MetadataValue> values = metadataValueService.findByRelationship(context, relationship);
        authorizeRelationshipMetadataValues(context, values);
        // Relationship deletion checks write access even if there are no associated metadata values.
        assertWriteOnRelationship(context, relationship);
        for (MetadataValue value : values) {
            // The primary and its additionals are all removed together.
            deleteRelationshipMetadata(context, value, metadataOwnerWithActiveIterator);
        }
        relationshipService.delete(context, relationship);
    }

    @Override
    public void removeMetadataValue(Context context, MetadataValue value,
                                    DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException {
        if (value == null || value.getRelationship() == null) {
            return;
        }
        Relationship relationship = value.getRelationship();
        // A relationship-backed value is the primary metadata value, so removing it also removes the relationship.
        removeRelationshipAndMetadata(context, relationship, metadataOwnerWithActiveIterator);
    }

    /**
     * Removes a stored relationship metadata value, recording its removal event and
     * clearing its relationship association. If the owning metadata collection is
     * currently being iterated, the caller handles removing the value from the collection.
     */
    private void deleteRelationshipMetadata(Context context, MetadataValue value,
                                  DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException {
        DSpaceObject owner = value.getDSpaceObject();
        owner.addMetadataEventDetails(new MetadataEvent(value, MetadataEvent.REMOVE));
        value.setRelationship(null);
        if (metadataOwnerWithActiveIterator == null
            || !owner.getID().equals(metadataOwnerWithActiveIterator.getID())) {
            owner.getMetadata().remove(value);
            metadataValueService.delete(context, value);
        }
    }

    /**
     * Checks WRITE authorization for all affected metadata owners before any changes
     * are made, ensuring a multi-value operation does not partially modify the
     * relationship before an authorization failure is discovered.
     */
    private void authorizeRelationshipMetadataValues(Context context, List<MetadataValue> values)
        throws SQLException, AuthorizeException {
        // Validate all permissions before the first mutation, including the other side.
        for (MetadataValue value : values) {
            authorizeService.authorizeAction(context, value.getDSpaceObject(), Constants.WRITE);
        }
    }

    private void assertWriteOnRelationship(Context context, Relationship relationship)
        throws SQLException, AuthorizeException {
        if (!authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE)
            && !authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
            throw new AuthorizeException("Write access to a relationship endpoint is required");
        }
    }

    private void requireOwner(MetadataValue value, Item owner) {
        if (owner == null || value.getDSpaceObject() == null
            || !owner.getID().equals(value.getDSpaceObject().getID())) {
            throw new IllegalArgumentException("Metadata does not belong to the supplied owner");
        }
    }

    private void requirePersistentRelationship(Relationship relationship) {
        if (relationship == null || relationship.getID() == null || relationship.getID() <= 0) {
            throw new IllegalArgumentException("A persistent relationship is required");
        }
    }

    private void requireConfiguredRelationship(Relationship relationship) {
        requirePersistentRelationship(relationship);
        if (!relationship.isConfigurationBacked()) {
            throw new IllegalArgumentException("This operation requires a configuration-backed relationship");
        }
    }

    /**
     * Returns the opposite endpoint of the relationship for the supplied metadata owner.
     *
     * @throws IllegalArgumentException if the owner is null or not a relationship endpoint
     */
    private Item opposite(Relationship relationship, DSpaceObject owner) {
        if (owner == null) {
            throw new IllegalArgumentException("A metadata owner is required");
        }
        if (owner.getID().equals(relationship.getLeftItem().getID())) {
            return relationship.getRightItem();
        }
        if (owner.getID().equals(relationship.getRightItem().getID())) {
            return relationship.getLeftItem();
        }
        throw new IllegalArgumentException("Metadata owner must be an endpoint of its relationship");
    }

    private void validateAuthority(MetadataValue value, Relationship relationship) {
        validateAuthorityTarget(value, opposite(relationship, value.getDSpaceObject()));
    }

    private void validateAuthorityTarget(MetadataValue value, Item target) {
        if (isUuid(value.getAuthority()) && !UUID.fromString(value.getAuthority()).equals(target.getID())) {
            throw new IllegalArgumentException("Authority target UUID does not match the relationship target UUID");
        }
    }

    private boolean isUuid(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void authorizeRelationshipOperation(Context context, DSpaceObject owner, Item target)
        throws SQLException, AuthorizeException {
        authorizeService.authorizeAction(context, owner, Constants.WRITE);
        authorizeService.authorizeAction(context, target, Constants.READ);
    }
}
