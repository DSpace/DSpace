/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.dao.OrcidQueueDAO;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidQueueService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueServiceImpl implements OrcidQueueService {

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired
    private OrcidQueueDAO orcidQueueDAO;

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException {
        return orcidQueueDAO.findByOwnerId(context, ownerId, limit, offset);
    }

    @Override
    public List<OrcidQueue> findByOwnerAndEntityId(Context context, UUID ownerId, UUID entityId) throws SQLException {
        return orcidQueueDAO.findByOwnerAndEntityId(context, ownerId, entityId);
    }

    @Override
    public long countByOwnerId(Context context, UUID ownerId) throws SQLException {
        return orcidQueueDAO.countByOwnerId(context, ownerId);
    }

    @Override
    public OrcidQueue create(Context context, Item owner, Item entity) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(entity);
        orcidQueue.setOwner(owner);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public void deleteById(Context context, Integer id) throws SQLException {
        OrcidQueue orcidQueue = orcidQueueDAO.findByID(context, OrcidQueue.class, id);
        if (orcidQueue != null) {
            orcidQueueDAO.delete(context, orcidQueue);
        }
    }

    @Override
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a OrcidQueue");
        }
        orcidQueueDAO.delete(context, orcidQueue);
    }

    @Override
    public OrcidQueue find(Context context, int id) throws SQLException {
        return orcidQueueDAO.findByID(context, OrcidQueue.class, id);
    }

    @Override
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException, AuthorizeException {
        update(context,Collections.singletonList(orcidQueue));
    }

    @Override
    public void update(Context context, List<OrcidQueue> orcidQueueList) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to update a OrcidQueue");
        }
        if (CollectionUtils.isNotEmpty(orcidQueueList)) {
            for (OrcidQueue orcidQueue: orcidQueueList) {
                orcidQueueDAO.save(context, orcidQueue);
            }
        }
    }
}
