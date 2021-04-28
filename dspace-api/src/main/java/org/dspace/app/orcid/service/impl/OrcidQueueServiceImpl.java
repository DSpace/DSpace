/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.dao.OrcidQueueDAO;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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
    private ItemService itemService;

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId) throws SQLException {
        return orcidQueueDAO.findByOwnerId(context, ownerId, -1, 0);
    }

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException {
        return orcidQueueDAO.findByOwnerId(context, ownerId, limit, offset);
    }

    @Override
    public List<OrcidQueue> findByOwnerAndEntity(Context context, Item owner, Item entity) throws SQLException {
        return orcidQueueDAO.findByOwnerAndEntity(context, owner, entity);
    }

    @Override
    public List<OrcidQueue> findByOwnerOrEntity(Context context, Item item) throws SQLException {
        return orcidQueueDAO.findByOwnerOrEntity(context, item);
    }

    @Override
    public List<OrcidQueue> findByEntityAndRecordType(Context context, Item entity, String type) throws SQLException {
        return orcidQueueDAO.findByEntityAndRecordType(context, entity, type);
    }

    @Override
    public long countByOwnerId(Context context, UUID ownerId) throws SQLException {
        return orcidQueueDAO.countByOwnerId(context, ownerId);
    }

    @Override
    public List<OrcidQueue> findAll(Context context) throws SQLException {
        return orcidQueueDAO.findAll(context, OrcidQueue.class);
    }

    @Override
    public OrcidQueue createEntityInsertionRecord(Context context, Item owner, Item entity) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(entity);
        orcidQueue.setRecordType(itemService.getEntityType(entity));
        orcidQueue.setOwner(owner);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createEntityUpdateRecord(Context context, Item owner, Item entity, String putCode)
        throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setOwner(owner);
        orcidQueue.setEntity(entity);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setRecordType(itemService.getEntityType(entity));
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createEntityDeletionRecord(Context context, Item owner, String description, String type,
        String putCode)
        throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setRecordType(type);
        orcidQueue.setOwner(owner);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setDescription(description);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createProfileInsertionRecord(Context context, Item profile, String description, String recordType,
        String metadata) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(profile);
        orcidQueue.setRecordType(recordType);
        orcidQueue.setOwner(profile);
        orcidQueue.setDescription(description);
        orcidQueue.setMetadata(metadata);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createProfileDeletionRecord(Context context, Item profile, String description, String recordType,
        String putCode) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(profile);
        orcidQueue.setRecordType(recordType);
        orcidQueue.setOwner(profile);
        orcidQueue.setDescription(description);
        orcidQueue.setPutCode(putCode);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public void deleteById(Context context, Integer id) throws SQLException {
        OrcidQueue orcidQueue = orcidQueueDAO.findByID(context, OrcidQueue.class, id);
        if (orcidQueue != null) {
            delete(context, orcidQueue);
        }
    }

    @Override
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.delete(context, orcidQueue);
    }

    @Override
    public OrcidQueue find(Context context, int id) throws SQLException {
        return orcidQueueDAO.findByID(context, OrcidQueue.class, id);
    }

    @Override
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.save(context, orcidQueue);
    }
}
