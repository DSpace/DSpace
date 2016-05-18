/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.dao;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.TaskListItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the TaskListItem object.
 * The implementation of this class is responsible for all database calls for the TaskListItem object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface TaskListItemDAO extends GenericDAO<TaskListItem> {

    public void deleteByWorkflowItem(Context context, BasicWorkflowItem workflowItem) throws SQLException;

    public List<TaskListItem> findByEPerson(Context context, EPerson ePerson) throws SQLException;
}
