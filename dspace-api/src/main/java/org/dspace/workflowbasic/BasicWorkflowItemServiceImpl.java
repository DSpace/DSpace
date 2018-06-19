/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.dao.BasicWorkflowItemDAO;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.TaskListItemService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Service implementation for the BasicWorkflowItem object.
 * This class is responsible for all business logic calls for the BasicWorkflowItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BasicWorkflowItemServiceImpl implements BasicWorkflowItemService {

    /** log4j category */
    protected static Logger log = Logger.getLogger(BasicWorkflowItem.class);

    @Autowired(required = true)
    protected BasicWorkflowItemDAO workflowItemDAO;

    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected TaskListItemService taskListItemService;


    protected BasicWorkflowItemServiceImpl()
    {

    }

    @Override
    public BasicWorkflowItem create(Context context, Item item, Collection collection) throws SQLException, AuthorizeException {
        if(findByItem(context, item) != null){
            throw new IllegalArgumentException("Unable to create a workflow item for an item that already has a workflow item.");
        }
        BasicWorkflowItem workflowItem = workflowItemDAO.create(context, new BasicWorkflowItem());
        workflowItem.setItem(item);
        workflowItem.setCollection(collection);
        update(context, workflowItem);
        return workflowItem;
    }

    @Override
    public BasicWorkflowItem find(Context context, int id) throws SQLException {
        BasicWorkflowItem workflowItem = workflowItemDAO.findByID(context, BasicWorkflowItem.class, id);

        if (workflowItem == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                        "not_found,workflow_id=" + id));
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                        "workflow_id=" + id));
            }
        }
        return workflowItem;
    }

    @Override
    public List<BasicWorkflowItem> findAll(Context context) throws SQLException {
        return workflowItemDAO.findAll(context, BasicWorkflowItem.class);
    }

    @Override
    public List<BasicWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {
        return workflowItemDAO.findBySubmitter(context, ep);
    }

    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException, IOException, AuthorizeException {
        List<BasicWorkflowItem> workflowItems = findByCollection(context, collection);
        Iterator<BasicWorkflowItem> iterator = workflowItems.iterator();
        while (iterator.hasNext()) {
            BasicWorkflowItem workflowItem = iterator.next();
            iterator.remove();
            delete(context, workflowItem);
        }
    }

    @Override
    public void delete(Context context, BasicWorkflowItem workflowItem) throws SQLException, AuthorizeException, IOException {
        Item item = workflowItem.getItem();
        deleteWrapper(context, workflowItem);
        itemService.delete(context, item);
    }

    @Override
    public List<BasicWorkflowItem> findByCollection(Context context, Collection collection) throws SQLException {
        return workflowItemDAO.findByCollection(context, collection);
    }

    @Override
    public BasicWorkflowItem findByItem(Context context, Item item) throws SQLException {
        return workflowItemDAO.findByItem(context, item);
    }

    @Override
    public void deleteWrapper(Context context, BasicWorkflowItem workflowItem) throws SQLException, AuthorizeException {
        // delete any pending tasks
        taskListItemService.deleteByWorkflowItem(context, workflowItem);

        // FIXME - auth?
        workflowItemDAO.delete(context, workflowItem);
    }

    @Override
    public void update(Context context, BasicWorkflowItem workflowItem) throws SQLException, AuthorizeException {
                // FIXME check auth
        log.info(LogManager.getHeader(context, "update_workflow_item",
                "workflow_item_id=" + workflowItem.getID()));


        // Update the item
        itemService.update(context, workflowItem.getItem());

        // Update ourselves
        workflowItemDAO.save(context, workflowItem);
    }

    @Override
    public List<BasicWorkflowItem> findPooledTasks(Context context, EPerson ePerson) throws SQLException {
        return workflowItemDAO.findByPooledTasks(context, ePerson);
    }

    @Override
    public List<BasicWorkflowItem> findByOwner(Context context, EPerson ePerson) throws SQLException {
        return workflowItemDAO.findByOwner(context, ePerson);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return workflowItemDAO.countRows(context);
    }
}
