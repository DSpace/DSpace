/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.dao.CollectionRoleDAO;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the CollectionRole object.
 * This class is responsible for all business logic calls for the CollectionRole object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionRoleServiceImpl implements CollectionRoleService {

    @Autowired(required = true)
    protected CollectionRoleDAO collectionRoleDAO;

    protected CollectionRoleServiceImpl() {

    }


    @Override
    public CollectionRole find(Session session, int id) throws SQLException {
        return collectionRoleDAO.findByID(session, CollectionRole.class, id);
    }

    @Override
    public CollectionRole find(Session session, Collection collection, String role) throws SQLException {
        return collectionRoleDAO.findByCollectionAndRole(session, collection, role);
    }

    @Override
    public List<CollectionRole> findByCollection(Session session, Collection collection) throws SQLException {
        return collectionRoleDAO.findByCollection(session, collection);
    }

    @Override
    public List<CollectionRole> findByGroup(Session session, Group group) throws SQLException {
        return collectionRoleDAO.findByGroup(session, group);
    }

    @Override
    public CollectionRole create(Context context, Collection collection, String roleId, Group group)
        throws SQLException {
        CollectionRole collectionRole = collectionRoleDAO.create(context.getSession(), new CollectionRole());
        collectionRole.setCollection(collection);
        collectionRole.setRoleId(roleId);
        collectionRole.setGroup(group);
        update(context, collectionRole);
        return collectionRole;
    }

    @Override
    public void update(Context context, CollectionRole collectionRole) throws SQLException {
        collectionRoleDAO.save(context.getSession(), collectionRole);

    }

    @Override
    public void delete(Context context, CollectionRole collectionRole) throws SQLException {
        collectionRoleDAO.delete(context.getSession(), collectionRole);
    }

    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException {
        collectionRoleDAO.deleteByCollection(context.getSession(), collection);
    }
}
