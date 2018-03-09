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
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.storedcomponents.dao.PoolTaskDAO;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation for the PoolTask object.
 * This class is responsible for all business logic calls for the PoolTask object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class PoolTaskServiceImpl implements PoolTaskService {

    @Autowired(required = true)
    protected PoolTaskDAO poolTaskDAO;

    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected InProgressUserService inProgressUserService;

    protected PoolTaskServiceImpl()
    {

    }

    @Override
    public List<PoolTask> findByEperson(Context context, EPerson ePerson) throws SQLException, AuthorizeException, IOException {
        List<PoolTask> result = poolTaskDAO.findByEPerson(context, ePerson);
        //Get all PoolTasks for groups of which this eperson is a member
        List<Group> groups = groupService.allMemberGroups(context, ePerson);
        result.addAll(findByGroups(context, ePerson, groups));
        return result;
    }

    protected List<PoolTask> findByGroups(Context context, EPerson ePerson, List<Group> groups) throws SQLException {
        List<PoolTask> result = new ArrayList<PoolTask>();
        for (Group group : groups) {
            List<PoolTask> groupTasks = poolTaskDAO.findByGroup(context, group);
            for (PoolTask poolTask : groupTasks) {
                XmlWorkflowItem workflowItem = poolTask.getWorkflowItem();
                if(inProgressUserService.findByWorkflowItemAndEPerson(context, workflowItem, ePerson) == null){
                    result.add(poolTask);
                }
            }
        }
        return result;
    }


    @Override
    public List<PoolTask> find(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return poolTaskDAO.findByWorkflowItem(context, workflowItem);
    }

    @Override
    public PoolTask findByWorkflowIdAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson) throws SQLException, AuthorizeException, IOException {
        PoolTask poolTask = poolTaskDAO.findByWorkflowItemAndEPerson(context, workflowItem, ePerson);

        //If there is a pooltask for this eperson, return it
        if(poolTask != null)
            return poolTask;
        else{
            //If the user has a is processing or has finished the step for a workflowitem, there is no need to look for pooltasks for one of his
            //groups because the user already has the task claimed
            if(inProgressUserService.findByWorkflowItemAndEPerson(context, workflowItem, ePerson)!=null){
                return null;
            }
            else{
                //If the user does not have a claimedtask yet, see whether one of the groups of the user has pooltasks
                //for this workflow item
                Set<Group> groups = groupService.allMemberGroupsSet(context, ePerson);
                for (Group group : groups) {
                    poolTask = poolTaskDAO.findByWorkflowItemAndGroup(context, group, workflowItem);
                    if(poolTask != null)
                    {
                        return poolTask;
                    }

                }
            }
        }
        return null;
    }

    @Override
    public void deleteByWorkflowItem(Context context, XmlWorkflowItem xmlWorkflowItem) throws SQLException, AuthorizeException {
        List<PoolTask> tasks = find(context, xmlWorkflowItem);
        //Use an iterator to remove the tasks !
        Iterator<PoolTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            PoolTask poolTask = iterator.next();
            iterator.remove();
            delete(context, poolTask);
        }
    }

    @Override
    public List<PoolTask> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        return poolTaskDAO.findByEPerson(context, ePerson);
    }

    @Override
    public PoolTask create(Context context) throws SQLException, AuthorizeException {
        return poolTaskDAO.create(context, new PoolTask());
    }

    @Override
    public PoolTask find(Context context, int id) throws SQLException {
        return poolTaskDAO.findByID(context, PoolTask.class, id);
    }

    @Override
    public void update(Context context, PoolTask poolTask) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(poolTask));
    }

    @Override
    public void update(Context context, List<PoolTask> poolTasks) throws SQLException, AuthorizeException {
        if(CollectionUtils.isNotEmpty(poolTasks)) {
            for (PoolTask poolTask : poolTasks) {
                poolTaskDAO.save(context, poolTask);
            }
        }
    }

    @Override
    public void delete(Context context, PoolTask poolTask) throws SQLException, AuthorizeException {
        poolTaskDAO.delete(context, poolTask);
    }
}
