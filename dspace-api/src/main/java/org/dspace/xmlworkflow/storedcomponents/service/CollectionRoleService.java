/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the CollectionRole object.
 * The implementation of this class is responsible for all business logic calls for the CollectionRole object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CollectionRoleService {

    public CollectionRole find(Context context, int id) throws SQLException;

    public CollectionRole find(Context context, Collection collection, String role) throws SQLException;

    public List<CollectionRole> findByCollection(Context context, Collection collection) throws SQLException;

    public CollectionRole create(Context context, Collection collection, String roleId, Group group) throws SQLException;

    public void update(Context context, CollectionRole collectionRole) throws SQLException;

    public void delete(Context context, CollectionRole collectionRole) throws SQLException;

    public void deleteByCollection(Context context, Collection collection) throws SQLException;
}
