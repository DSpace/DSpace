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

    private OrcidQueueDAO dao;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired
    public OrcidQueueServiceImpl(OrcidQueueDAO dao) {
        this.dao = dao;
    }

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException {
        return dao.findByOwnerId(context, ownerId, limit, offset);
    }

    @Override
    public long countByOwnerId(Context context, UUID ownerId) throws SQLException {
        return dao.countByOwnerId(context, ownerId);
    }

    @Override
    public OrcidQueue create(Context context, Item owner, Item entity) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(entity);
        orcidQueue.setOwner(owner);
        return dao.create(context, orcidQueue);
    }

    @Override
    public void deleteById(Context context, Integer id) throws SQLException {
        OrcidQueue orcidQueue = dao.findByID(context, OrcidQueue.class, id);
        if (orcidQueue != null) {
            dao.delete(context, orcidQueue);
        }
    }

    @Override
    public void sendToOrcid(Context context, Integer id) throws SQLException {
        System.out.println(id);
        OrcidQueue orcidQueue = dao.findByID(context, OrcidQueue.class, id);
        if (orcidQueue == null) {
            throw new IllegalArgumentException("No ORCID Queue record found with id " + id);
        }

        Item owner = orcidQueue.getOwner();
        String orcid = getMetadataValue(owner, "person.identifier.orcid");
        Item entity = orcidQueue.getEntity();
    }

    private String getMetadataValue(Item item, String metadataField) {
        return item.getMetadata().stream()
            .filter(metadata -> metadata.getMetadataField().toString('.').equals(metadataField))
            .map(metadata -> metadata.getValue())
            .findFirst()
            .orElse(null);
    }

    @Override
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a Tab");
        }
        dao.delete(context, orcidQueue);
    }

    @Override
    public OrcidQueue create(Context context) throws SQLException, AuthorizeException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OrcidQueue find(Context context, int id) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void update(Context context, OrcidQueue t) throws SQLException, AuthorizeException {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(Context context, List<OrcidQueue> t) throws SQLException, AuthorizeException {
        // TODO Auto-generated method stub
    }
}
