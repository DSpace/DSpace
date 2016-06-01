/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.dao.ClaimedTaskDAO;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Service implementation for the ClaimedTask object.
 * This class is responsible for all business logic calls for the ClaimedTask object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ClaimedTaskServiceImpl implements ClaimedTaskService
{

    @Autowired(required = true)
    protected ClaimedTaskDAO claimedTaskDAO;

    protected ClaimedTaskServiceImpl()
    {

    }

    @Override
    public ClaimedTask create(Context context) throws SQLException, AuthorizeException {
        return claimedTaskDAO.create(context, new ClaimedTask());
    }

    @Override
    public ClaimedTask find(Context context, int id) throws SQLException {
        return claimedTaskDAO.findByID(context, ClaimedTask.class, id);
    }

    @Override
    public void update(Context context, ClaimedTask claimedTask) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(claimedTask));
    }

    @Override
    public void update(Context context, List<ClaimedTask> claimedTasks) throws SQLException, AuthorizeException {
        if(CollectionUtils.isNotEmpty(claimedTasks)) {
            for (ClaimedTask claimedTask : claimedTasks) {
                claimedTaskDAO.save(context, claimedTask);
            }
        }
    }

    @Override
    public void delete(Context context, ClaimedTask claimedTask) throws SQLException, AuthorizeException {
        claimedTaskDAO.delete(context, claimedTask);
    }

    @Override
    public List<ClaimedTask> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return claimedTaskDAO.findByWorkflowItem(context, workflowItem);
    }

    @Override
    public ClaimedTask findByWorkflowIdAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson) throws SQLException {
        return claimedTaskDAO.findByWorkflowItemAndEPerson(context, workflowItem, ePerson);
    }

    @Override
    public List<ClaimedTask> findByEperson(Context context, EPerson ePerson) throws SQLException {
        return claimedTaskDAO.findByEperson(context, ePerson);
    }

    @Override
    public List<ClaimedTask> find(Context context, XmlWorkflowItem workflowItem, String stepID) throws SQLException {
        return claimedTaskDAO.findByWorkflowItemAndStepId(context, workflowItem, stepID);
    }

    @Override
    public ClaimedTask find(Context context, EPerson ePerson, XmlWorkflowItem workflowItem, String stepID, String actionID) throws SQLException {
        return claimedTaskDAO.findByEPersonAndWorkflowItemAndStepIdAndActionId(context, ePerson,workflowItem,stepID,actionID);
    }

    @Override
    public List<ClaimedTask> find(Context context, XmlWorkflowItem workflowItem, String stepID, String actionID) throws SQLException {
        return claimedTaskDAO.findByWorkflowItemAndStepIdAndActionId(context, workflowItem,stepID, actionID);
    }

    @Override
    public List<ClaimedTask> find(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return claimedTaskDAO.findByWorkflowItem(context, workflowItem);
    }

    @Override
    public List<ClaimedTask> findAllInStep(Context context, String stepID) throws SQLException {
        return claimedTaskDAO.findByStep(context, stepID);
    }

    @Override
    public void deleteByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException {
        List<ClaimedTask> claimedTasks = findByWorkflowItem(context, workflowItem);
        //Use an iterator to remove the tasks !
        Iterator<ClaimedTask> iterator = claimedTasks.iterator();
        while (iterator.hasNext()) {
            ClaimedTask claimedTask = iterator.next();
            iterator.remove();
            delete(context, claimedTask);
        }
    }
}
