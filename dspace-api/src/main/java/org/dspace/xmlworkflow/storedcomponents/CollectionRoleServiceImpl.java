/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.dao.CollectionRoleDAO;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

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

    protected CollectionRoleServiceImpl()
    {

    }


    @Override
    public CollectionRole find(Context context, int id) throws SQLException {
        return collectionRoleDAO.findByID(context, CollectionRole.class, id);
    }

    @Override
    public CollectionRole find(Context context, Collection collection, String role) throws SQLException {
        return collectionRoleDAO.findByCollectionAndRole(context, collection, role);
    }

    @Override
    public List<CollectionRole> findByCollection(Context context, Collection collection) throws SQLException {
        return collectionRoleDAO.findByCollection(context, collection);
    }

    @Override
    public CollectionRole create(Context context, Collection collection, String roleId, Group group) throws SQLException {
        CollectionRole collectionRole = collectionRoleDAO.create(context, new CollectionRole());
        collectionRole.setCollection(collection);
        collectionRole.setRoleId(roleId);
        collectionRole.setGroup(group);
        update(context, collectionRole);
        return collectionRole;
    }

    @Override
    public void update(Context context, CollectionRole collectionRole) throws SQLException {
        collectionRoleDAO.save(context, collectionRole);

    }

    @Override
    public void delete(Context context, CollectionRole collectionRole) throws SQLException {
        collectionRoleDAO.delete(context, collectionRole);
    }

    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException {
        collectionRoleDAO.deleteByCollection(context, collection);
    }
}
