/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;

/**
 * Database Access Object interface class for the CollectionRole object.
 * The implementation of this class is responsible for all database calls for the CollectionRole object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CollectionRoleDAO extends GenericDAO<CollectionRole> {

    public List<CollectionRole> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * 
     * @param context
     *            DSpace context
     * @param group
     *            EPerson Group
     * @return the list of CollectionRole assigned to the specified group
     * @throws SQLException
     */
    public List<CollectionRole> findByGroup(Context context, Group group) throws SQLException;

    public CollectionRole findByCollectionAndRole(Context context, Collection collection, String role)
        throws SQLException;

    public void deleteByCollection(Context context, Collection collection) throws SQLException;
}
