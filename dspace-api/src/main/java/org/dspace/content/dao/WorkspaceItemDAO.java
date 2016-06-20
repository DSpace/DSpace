/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database Access Object interface class for the WorkspaceItem object.
 * The implementation of this class is responsible for all database calls for the WorkspaceItem object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkspaceItemDAO extends GenericDAO<WorkspaceItem> {

    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep) throws SQLException;

    public List<WorkspaceItem> findByCollection(Context context, Collection c) throws SQLException;

    public WorkspaceItem findByItem(Context context, Item i) throws SQLException;

    public List<WorkspaceItem> findAll(Context context) throws SQLException;

    public List<WorkspaceItem> findWithSupervisedGroup(Context context) throws SQLException;

    public List<WorkspaceItem> findBySupervisedGroupMember(Context context, EPerson ePerson) throws SQLException;

    int countRows(Context context) throws SQLException;

    List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException;
}
