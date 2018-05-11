/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.service;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.TaskListItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the TaskListItem object.
 * The implementation of this class is responsible for all business logic calls for the TaskListItem object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface TaskListItemService {

    public TaskListItem create(Context context, BasicWorkflowItem workflowItem, EPerson ePerson) throws SQLException;

    public void deleteByWorkflowItem(Context context, BasicWorkflowItem workflowItem) throws SQLException;

    public void update(Context context, TaskListItem taskListItem) throws SQLException;

    public List<TaskListItem> findByEPerson(Context context, EPerson ePerson) throws SQLException;
}
