/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.dao.ClaimedTaskDAO;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the ClaimedTask object.
 * This class is responsible for all business logic calls for the ClaimedTask
 * object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ClaimedTaskServiceImpl implements ClaimedTaskService {

    @Autowired(required = true)
    protected ClaimedTaskDAO claimedTaskDAO;

    protected ClaimedTaskServiceImpl() {

    }

    @Override
    public ClaimedTask create(Context context) throws SQLException, AuthorizeException {
        return claimedTaskDAO.create(context.getSession(), new ClaimedTask());
    }

    @Override
    public ClaimedTask find(Session session, int id) throws SQLException {
        return claimedTaskDAO.findByID(session, ClaimedTask.class, id);
    }

    @Override
    public List<ClaimedTask> findAll(Session session) throws SQLException {
        return claimedTaskDAO.findAll(session, ClaimedTask.class);
    }

    @Override
    public void update(Context context, ClaimedTask claimedTask) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(claimedTask));
    }

    @Override
    public void update(Context context, List<ClaimedTask> claimedTasks) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(claimedTasks)) {
            for (ClaimedTask claimedTask : claimedTasks) {
                claimedTaskDAO.save(context.getSession(), claimedTask);
            }
        }
    }

    @Override
    public void delete(Context context, ClaimedTask claimedTask) throws SQLException, AuthorizeException {
        claimedTaskDAO.delete(context.getSession(), claimedTask);
    }

    @Override
    public List<ClaimedTask> findByWorkflowItem(Session session, XmlWorkflowItem workflowItem) throws SQLException {
        return claimedTaskDAO.findByWorkflowItem(session, workflowItem);
    }

    @Override
    public ClaimedTask findByWorkflowIdAndEPerson(Session session, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException {
        return claimedTaskDAO.findByWorkflowItemAndEPerson(session, workflowItem, ePerson);
    }

    @Override
    public List<ClaimedTask> findByEperson(Session session, EPerson ePerson) throws SQLException {
        return claimedTaskDAO.findByEperson(session, ePerson);
    }

    @Override
    public List<ClaimedTask> find(Session session, XmlWorkflowItem workflowItem, String stepID) throws SQLException {
        return claimedTaskDAO.findByWorkflowItemAndStepId(session, workflowItem, stepID);
    }

    @Override
    public ClaimedTask find(Session session, EPerson ePerson, XmlWorkflowItem workflowItem, String stepID,
                            String actionID) throws SQLException {
        return claimedTaskDAO
            .findByEPersonAndWorkflowItemAndStepIdAndActionId(session,
                    ePerson, workflowItem, stepID, actionID);
    }

    @Override
    public List<ClaimedTask> find(Session session, XmlWorkflowItem workflowItem, String stepID, String actionID)
        throws SQLException {
        return claimedTaskDAO.findByWorkflowItemAndStepIdAndActionId(session,
                workflowItem, stepID, actionID);
    }

    @Override
    public List<ClaimedTask> find(Session session, XmlWorkflowItem workflowItem) throws SQLException {
        return claimedTaskDAO.findByWorkflowItem(session, workflowItem);
    }

    @Override
    public List<ClaimedTask> findAllInStep(Session session, String stepID) throws SQLException {
        return claimedTaskDAO.findByStep(session, stepID);
    }

    @Override
    public void deleteByWorkflowItem(Context context, XmlWorkflowItem workflowItem)
        throws SQLException, AuthorizeException {
        List<ClaimedTask> claimedTasks = findByWorkflowItem(context.getSession(), workflowItem);
        //Use an iterator to remove the tasks !
        Iterator<ClaimedTask> iterator = claimedTasks.iterator();
        while (iterator.hasNext()) {
            ClaimedTask claimedTask = iterator.next();
            iterator.remove();
            delete(context, claimedTask);
        }
    }
}
