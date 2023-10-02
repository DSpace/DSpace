/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the WorkspaceItem object.
 * The implementation of this class is responsible for all database calls for
 * the WorkspaceItem object and is autowired by Spring.
 * This class should only be accessed from a single service and should never be
 * exposed outside of the API.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkspaceItemDAO extends GenericDAO<WorkspaceItem> {

    public List<WorkspaceItem> findByEPerson(Session session, EPerson ep) throws SQLException;

    public List<WorkspaceItem> findByEPerson(Session session, EPerson ep, Integer limit, Integer offset)
        throws SQLException;

    public List<WorkspaceItem> findByCollection(Session session, Collection c) throws SQLException;

    public WorkspaceItem findByItem(Session session, Item i) throws SQLException;

    public List<WorkspaceItem> findAll(Session session) throws SQLException;

    public List<WorkspaceItem> findAll(Session session, Integer limit, Integer offset) throws SQLException;

    int countRows(Session session) throws SQLException;

    List<Map.Entry<Integer, Long>> getStageReachedCounts(Session session) throws SQLException;

    public int countRows(Session session, EPerson ep) throws SQLException;

}
