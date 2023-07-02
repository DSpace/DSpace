/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.GenericDAO;
import org.dspace.eperson.Group;
import org.dspace.eperson.Group2GroupCache;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Group2GroupCache object.
 * The implementation of this class is responsible for all database calls for the Group2GroupCache object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface Group2GroupCacheDAO extends GenericDAO<Group2GroupCache> {

    public List<Group2GroupCache> findByParent(Session session, Group group) throws SQLException;

    public List<Group2GroupCache> findByChildren(Session session, Iterable<Group> groups) throws SQLException;

    public Group2GroupCache findByParentAndChild(Session session, Group parent, Group child) throws SQLException;

    public Group2GroupCache find(Session session, Group parent, Group child) throws SQLException;

    public void deleteAll(Session session) throws SQLException;
}
