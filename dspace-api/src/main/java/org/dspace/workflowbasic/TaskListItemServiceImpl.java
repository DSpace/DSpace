/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.dao.TaskListItemDAO;
import org.dspace.workflowbasic.service.TaskListItemService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

/**
 * Service implementation for the TaskListItem object.
 * This class is responsible for all business logic calls for the TaskListItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class TaskListItemServiceImpl implements TaskListItemService {

    @Autowired(required = true)
    protected TaskListItemDAO taskListItemDAO;

    protected TaskListItemServiceImpl()
    {

    }

    @Override
    public TaskListItem create(Context context, BasicWorkflowItem workflowItem, EPerson ePerson) throws SQLException {
        TaskListItem taskListItem = taskListItemDAO.create(context, new TaskListItem());
        taskListItem.setWorkflowItem(workflowItem);
        taskListItem.setEPerson(ePerson);
        update(context, taskListItem);
        return taskListItem;
    }

    @Override
    public void deleteByWorkflowItem(Context context, BasicWorkflowItem workflowItem) throws SQLException {
        taskListItemDAO.deleteByWorkflowItem(context, workflowItem);
    }

    @Override
    public void update(Context context, TaskListItem taskListItem) throws SQLException {
        taskListItemDAO.save(context, taskListItem);
    }

    @Override
    public List<TaskListItem> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        return taskListItemDAO.findByEPerson(context, ePerson);
    }
}
