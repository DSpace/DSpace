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
import java.util.Objects;
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
 * CRIS integration adapter and initial compound metadata/relationship service.
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
        if (!owner.isArchived() || !target.isArchived() || value.getConfidence() == Choices.CF_REJECTED) {
            return null;
        }
        authorizeService.authorizeAction(context, owner, Constants.WRITE);
        authorizeService.authorizeAction(context, target, Constants.READ);

        Relationship existing = value.getRelationship();
        if (existing != null) {
            // Never silently accept a stale or different authority as idempotency.
            if (!opposite(existing, owner).getID().equals(target.getID())) {
                throw new IllegalArgumentException(
                        "Use replaceRelatedObject to change an existing relationship target");
            }
            validateAuthority(value, existing);
            return existing;
        }
        RelationshipTypeConfiguration configuration = relationshipConfigurationService.findForMetadataValue(value);
        if (configuration == null) {
            return null;
        }
        boolean ownerOnLeft = configuration.isOwnerOnLeft(RelationshipConfigurationServiceImpl.fieldName(value));
        Item left = ownerOnLeft ? owner : target;
        Item right = ownerOnLeft ? target : owner;
        relationshipConfigurationService.validate(context, configuration, left, right);
        // Validate before persistence, so invalid input does not leave a half-created link.
        validateAuthorityTarget(value, target);
        Relationship relationship = relationshipService.createConfigBackedRelationship(
            context, left, right, configuration.getId());
        value.setRelationship(relationship);
        metadataValueService.update(context, value);
        itemService.update(context, owner);
        log.debug("Linked metadata {} to relationship {} using configuration {}",
            value.getID(), relationship.getID(), configuration.getId());
        return relationship;
    }

    @Override
    public void attachMetadataToRelationship(Context context, MetadataValue value, Relationship relationship)
        throws SQLException, AuthorizeException {
        requirePersistentRelationship(relationship);
        if (value == null || value.getDSpaceObject() == null) {
            throw new IllegalArgumentException("A stored metadata value and owner are required");
        }
        Item target = opposite(relationship, value.getDSpaceObject());
        authorizeService.authorizeAction(context, value.getDSpaceObject(), Constants.WRITE);
        authorizeService.authorizeAction(context, target, Constants.READ);
        if (value.getRelationship() != null && !sameRelationship(value.getRelationship(), relationship)) {
            throw new IllegalArgumentException("Metadata is already associated with another relationship");
        }
        validateAuthority(value, relationship);
        value.setRelationship(relationship);
        metadataValueService.update(context, value);
        itemService.update(context, (Item) value.getDSpaceObject());
    }

    @Override
    public void replaceRelatedObject(Context context, Item owner, Relationship relationship, Item newTarget)
        throws SQLException, AuthorizeException {
        requireConfiguredRelationship(relationship);
        if (newTarget == null) {
            throw new IllegalArgumentException("A new target item is required");
        }
        Item oldTarget = opposite(relationship, owner);
        List<MetadataValue> values = metadataValueService.findByRelationship(context, relationship);
        authorizeService.authorizeAction(context, owner, Constants.WRITE);
        authorizeService.authorizeAction(context, newTarget, Constants.READ);
        authorizeProjections(context, values);
        boolean ownerOnLeft = owner.getID().equals(relationship.getLeftItem().getID());
        RelationshipTypeConfiguration configuration = relationshipConfigurationService.getByKey(
            relationship.getRelationshipConfigKey());
        relationshipConfigurationService.validate(context, configuration,
            ownerOnLeft ? owner : newTarget, ownerOnLeft ? newTarget : owner);
        // Moving opposite-side metadata to a different owning item needs a policy of
        // its own. Reject BEFORE changing anything rather than orphaning those rows.
        for (MetadataValue value : values) {
            if (!value.getDSpaceObject().getID().equals(owner.getID())) {
                throw new IllegalArgumentException(
                        "Relinking a two-sided projection requires an explicit transfer policy");
            }
            String anchor = ownerOnLeft ? configuration.getLeftMetadataField() : configuration.getRightMetadataField();
            if (!RelationshipConfigurationServiceImpl.fieldName(value).equals(anchor)) {
                throw new IllegalArgumentException("Relinking dependent projections requires a refresh policy");
            }
        }
        if (ownerOnLeft) {
            relationship.setRightItem(newTarget);
        } else {
            relationship.setLeftItem(newTarget);
        }
        for (MetadataValue value : values) {
            // Preserve publication-specific text and external identifiers. UUID
            // authority caches, if retained for CRIS compatibility, must follow the link.
            if (isUuid(value.getAuthority())) {
                value.setAuthority(newTarget.getID().toString());
            }
        }
        relationshipService.update(context, relationship);
        itemService.update(context, owner);
        itemService.update(context, oldTarget);
        itemService.update(context, newTarget);
    }

    @Override
    public void detachRelationshipKeepMetadata(Context context, Relationship relationship)
        throws SQLException, AuthorizeException {
        requireConfiguredRelationship(relationship);
        List<MetadataValue> values = metadataValueService.findByRelationship(context, relationship);
        authorizeProjections(context, values);
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
        authorizeProjections(context, values);
        // Relationship deletion checks write access even if there are no projections.
        assertWriteOnRelationship(context, relationship);
        for (MetadataValue value : values) {
            deleteProjection(context, value, metadataOwnerWithActiveIterator);
        }
        relationshipService.delete(context, relationship);
    }

    @Override
    public void removeMetadataProjection(Context context, MetadataValue value)
        throws SQLException, AuthorizeException {
        removeMetadataProjection(context, value, null);
    }

    private void removeMetadataProjection(Context context, MetadataValue value,
                                          DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException {
        authorizeService.authorizeAction(context, value.getDSpaceObject(), Constants.WRITE);
        if (value.isRelationshipBacked() && isFinalAnchor(context, value)) {
            throw new IllegalArgumentException("Use removeMetadataValue or detach for the final relationship anchor");
        }
        deleteProjection(context, value, metadataOwnerWithActiveIterator);
    }

    @Override
    public void removeMetadataValue(Context context, MetadataValue value)
        throws SQLException, AuthorizeException {
        removeMetadataValue(context, value, null);
    }

    @Override
    public void removeMetadataValue(Context context, MetadataValue value,
                                    DSpaceObject metadataOwnerWithActiveIterator)
        throws SQLException, AuthorizeException {
        if (value.isRelationshipBacked() && isFinalAnchor(context, value)) {
            removeRelationshipAndMetadata(context, value.getRelationship(), metadataOwnerWithActiveIterator);
        } else {
            removeMetadataProjection(context, value, metadataOwnerWithActiveIterator);
        }
    }

    private boolean isFinalAnchor(Context context, MetadataValue value) throws SQLException {
        Relationship relationship = value.getRelationship();
        if (!relationship.isConfigurationBacked()) {
            // Legacy typed projections require explicit migration/policy first.
            throw new IllegalArgumentException(
                    "Migrate this legacy relationship before editing its stored projections");
        }
        RelationshipTypeConfiguration configuration = relationshipConfigurationService.getByKey(
            relationship.getRelationshipConfigKey());
        String field = RelationshipConfigurationServiceImpl.fieldName(value);
        boolean ownerOnLeft = value.getDSpaceObject().getID().equals(relationship.getLeftItem().getID());
        String anchor = ownerOnLeft ? configuration.getLeftMetadataField() : configuration.getRightMetadataField();
        if (!field.equals(anchor)) {
            return false;
        }
        // One language-specific value may be removed without destroying another
        // variant of the same anchor. Association, not place, identifies the group.
        return metadataValueService.findByRelationship(context, relationship).stream().noneMatch(other ->
            !other.getID().equals(value.getID())
                && other.getDSpaceObject().getID().equals(value.getDSpaceObject().getID())
                && RelationshipConfigurationServiceImpl.fieldName(other).equals(field));
    }

    private void deleteProjection(Context context, MetadataValue value,
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

    private void authorizeProjections(Context context, List<MetadataValue> values)
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
            throw new IllegalArgumentException("UUID authority cache disagrees with the durable relationship");
        }
    }

    private boolean sameRelationship(Relationship first, Relationship second) {
        return first == second || first.getID() != null && Objects.equals(first.getID(), second.getID());
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
}
