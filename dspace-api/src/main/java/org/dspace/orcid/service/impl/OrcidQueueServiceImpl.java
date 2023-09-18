/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.dao.OrcidQueueDAO;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.profile.OrcidEntitySyncPreference;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidQueueService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueServiceImpl implements OrcidQueueService {

    @Autowired
    private OrcidQueueDAO orcidQueueDAO;

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipService relationshipService;

    @Override
    public List<OrcidQueue> findByProfileItemId(Session session, UUID profileItemId) throws SQLException {
        return orcidQueueDAO.findByProfileItemId(session, profileItemId, -1, 0);
    }

    @Override
    public List<OrcidQueue> findByProfileItemId(Context context, UUID profileItemId, Integer limit, Integer offset)
        throws SQLException {
        return orcidQueueDAO.findByProfileItemId(context.getSession(), profileItemId, limit, offset);
    }

    @Override
    public List<OrcidQueue> findByProfileItemAndEntity(Session session, Item profileItem, Item entity)
        throws SQLException {
        return orcidQueueDAO.findByProfileItemAndEntity(session, profileItem, entity);
    }

    @Override
    public List<OrcidQueue> findByProfileItemOrEntity(Session session, Item item) throws SQLException {
        return orcidQueueDAO.findByProfileItemOrEntity(session, item);
    }

    @Override
    public long countByProfileItemId(Context context, UUID profileItemId) throws SQLException {
        return orcidQueueDAO.countByProfileItemId(context.getSession(), profileItemId);
    }

    @Override
    public List<OrcidQueue> findAll(Session session) throws SQLException {
        return orcidQueueDAO.findAll(session, OrcidQueue.class);
    }

    @Override
    public OrcidQueue create(Context context, Item profileItem, Item entity) throws SQLException {
        Optional<String> putCode = orcidHistoryService.findLastPutCode(context.getSession(), profileItem, entity);
        if (putCode.isPresent()) {
            return createEntityUpdateRecord(context, profileItem, entity, putCode.get());
        } else {
            return createEntityInsertionRecord(context, profileItem, entity);
        }
    }

    @Override
    public OrcidQueue createEntityInsertionRecord(Context context, Item profileItem, Item entity) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(entity);
        orcidQueue.setRecordType(itemService.getEntityTypeLabel(entity));
        orcidQueue.setProfileItem(profileItem);
        orcidQueue.setDescription(getMetadataValue(entity, "dc.title"));
        orcidQueue.setOperation(OrcidOperation.INSERT);
        return orcidQueueDAO.create(context.getSession(), orcidQueue);
    }

    @Override
    public OrcidQueue createEntityUpdateRecord(Context context, Item profileItem, Item entity, String putCode)
        throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setProfileItem(profileItem);
        orcidQueue.setEntity(entity);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setRecordType(itemService.getEntityTypeLabel(entity));
        orcidQueue.setDescription(getMetadataValue(entity, "dc.title"));
        orcidQueue.setOperation(OrcidOperation.UPDATE);
        return orcidQueueDAO.create(context.getSession(), orcidQueue);
    }

    @Override
    public OrcidQueue createEntityDeletionRecord(Context context, Item profileItem, String description, String type,
        String putCode)
        throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setRecordType(type);
        orcidQueue.setProfileItem(profileItem);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setDescription(description);
        orcidQueue.setOperation(OrcidOperation.DELETE);
        return orcidQueueDAO.create(context.getSession(), orcidQueue);
    }

    @Override
    public OrcidQueue createProfileInsertionRecord(Context context, Item profile, String description, String recordType,
        String metadata) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(profile);
        orcidQueue.setRecordType(recordType);
        orcidQueue.setProfileItem(profile);
        orcidQueue.setDescription(description);
        orcidQueue.setMetadata(metadata);
        orcidQueue.setOperation(OrcidOperation.INSERT);
        return orcidQueueDAO.create(context.getSession(), orcidQueue);
    }

    @Override
    public OrcidQueue createProfileDeletionRecord(Context context, Item profile, String description, String recordType,
        String metadata, String putCode) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(profile);
        orcidQueue.setRecordType(recordType);
        orcidQueue.setProfileItem(profile);
        orcidQueue.setDescription(description);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setMetadata(metadata);
        orcidQueue.setOperation(OrcidOperation.DELETE);
        return orcidQueueDAO.create(context.getSession(), orcidQueue);
    }

    @Override
    public void deleteById(Context context, Integer id) throws SQLException {
        OrcidQueue orcidQueue = orcidQueueDAO.findByID(context.getSession(), OrcidQueue.class, id);
        if (orcidQueue != null) {
            delete(context, orcidQueue);
        }
    }

    @Override
    public List<OrcidQueue> findByAttemptsLessThan(Session session, int attempts) throws SQLException {
        return orcidQueueDAO.findByAttemptsLessThan(session, attempts);
    }

    @Override
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.delete(context.getSession(), orcidQueue);
    }

    @Override
    public void deleteByEntityAndRecordType(Context context, Item entity, String recordType) throws SQLException {
        List<OrcidQueue> records
                = orcidQueueDAO.findByEntityAndRecordType(context.getSession(), entity, recordType);
        for (OrcidQueue record : records) {
            orcidQueueDAO.delete(context.getSession(), record);
        }
    }

    @Override
    public void deleteByProfileItemAndRecordType(Context context, Item profileItem, String recordType)
        throws SQLException {
        List<OrcidQueue> records
                = orcidQueueDAO.findByProfileItemAndRecordType(context.getSession(), profileItem, recordType);
        for (OrcidQueue record : records) {
            orcidQueueDAO.delete(context.getSession(), record);
        }
    }

    @Override
    public OrcidQueue find(Session session, int id) throws SQLException {
        return orcidQueueDAO.findByID(session, OrcidQueue.class, id);
    }

    @Override
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.save(context.getSession(), orcidQueue);
    }

    @Override
    public void recalculateOrcidQueue(Context context, Item profileItem, OrcidEntityType orcidEntityType,
        OrcidEntitySyncPreference preference) throws SQLException {

        String entityType = orcidEntityType.getEntityType();
        if (preference == OrcidEntitySyncPreference.DISABLED) {
            deleteByProfileItemAndRecordType(context, profileItem, entityType);
        } else {
            List<Item> entities = findAllEntitiesLinkableWith(context, profileItem, entityType);
            for (Item entity : entities) {
                create(context, profileItem, entity);
            }
        }

    }

    private List<Item> findAllEntitiesLinkableWith(Context context, Item profile, String entityType) {

        return findRelationshipsByItem(context, profile).stream()
            .map(relationship -> getRelatedItem(relationship, profile))
            .filter(item -> entityType.equals(itemService.getEntityTypeLabel(item)))
            .collect(Collectors.toList());

    }

    private List<Relationship> findRelationshipsByItem(Context context, Item item) {
        try {
            return relationshipService.findByItem(context.getSession(), item);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Item getRelatedItem(Relationship relationship, Item item) {
        return relationship.getLeftItem().equals(item) ? relationship.getRightItem() : relationship.getLeftItem();
    }

    private String getMetadataValue(Item item, String metadatafield) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadatafield), Item.ANY);
    }
}
