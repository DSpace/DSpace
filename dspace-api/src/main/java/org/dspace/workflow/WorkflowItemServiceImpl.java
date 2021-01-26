/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.dao.WorkflowItemDAO;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the WorkflowItem object.
 * This class is responsible for all business logic calls for the WorkflowItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkflowItemServiceImpl implements WorkflowItemService {

    @Autowired(required = true)
    protected WorkflowItemDAO workflowItemDAO;


    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected PoolTaskService poolTaskService;
    @Autowired(required = true)
    protected WorkflowRequirementsService workflowRequirementsService;
    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;

    /*
     * The current step in the workflow system in which this workflow item is present
     */
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(WorkflowItemServiceImpl.class);


    protected WorkflowItemServiceImpl() {

    }

    @Override
    public WorkflowItem create(Context context, Item item, Collection collection)
        throws SQLException, AuthorizeException {
        WorkflowItem workflowItem = workflowItemDAO.create(context, new WorkflowItem());
        workflowItem.setItem(item);
        workflowItem.setCollection(collection);
        return workflowItem;
    }

    @Override
    public WorkflowItem find(Context context, int id) throws SQLException {
        WorkflowItem workflowItem = workflowItemDAO.findByID(context, WorkflowItem.class, id);

        if (workflowItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                                               "not_found,workflowitem_id=" + id));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                                               "workflowitem_id=" + id));
            }
        }
        return workflowItem;
    }

    @Override
    public List<WorkflowItem> findAll(Context context) throws SQLException {
        return workflowItemDAO.findAll(context, WorkflowItem.class);
    }

    @Override
    public List<WorkflowItem> findAll(Context context, Integer page, Integer pagesize) throws SQLException {
        return findAllInCollection(context, page, pagesize, null);
    }

    @Override
    public List<WorkflowItem> findAllInCollection(Context context, Integer page, Integer pagesize,
                                                  Collection collection) throws SQLException {
        Integer offset = null;
        if (page != null && pagesize != null) {
            offset = page * pagesize;
        }
        return workflowItemDAO.findAllInCollection(context, offset, pagesize, collection);
    }

    @Override
    public int countAll(Context context) throws SQLException {
        return workflowItemDAO.countAll(context);
    }

    @Override
    public int countAllInCollection(Context context, Collection collection) throws SQLException {
        return workflowItemDAO.countAllInCollection(context, collection);
    }

    @Override
    public List<WorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {
        return workflowItemDAO.findBySubmitter(context, ep);
    }

    @Override
    public List<WorkflowItem> findBySubmitter(Context context, EPerson ep, Integer pageNumber, Integer pageSize)
            throws SQLException {
        Integer offset = null;
        if (pageNumber != null && pageSize != null) {
            offset = pageNumber * pageSize;
        }
        return workflowItemDAO.findBySubmitter(context, ep, pageNumber, pageSize);
    }

    @Override
    public int countBySubmitter(Context context, EPerson ep) throws SQLException {
        return workflowItemDAO.countBySubmitter(context, ep);
    }

    @Override
    public void deleteByCollection(Context context, Collection collection)
        throws SQLException, IOException, AuthorizeException {
        List<WorkflowItem> workflowItems = findByCollection(context, collection);
        Iterator<WorkflowItem> iterator = workflowItems.iterator();
        while (iterator.hasNext()) {
            WorkflowItem workflowItem = iterator.next();
            iterator.remove();
            delete(context, workflowItem);
        }
    }

    @Override
    public void delete(Context context, WorkflowItem workflowItem)
        throws SQLException, AuthorizeException, IOException {
        Item item = workflowItem.getItem();
        // Need to delete the workspaceitem row first since it refers
        // to item ID
        deleteWrapper(context, workflowItem);

        // Delete item
        itemService.delete(context, item);
    }

    @Override
    public List<WorkflowItem> findByCollection(Context context, Collection collection) throws SQLException {
        return workflowItemDAO.findByCollection(context, collection);
    }

    @Override
    public WorkflowItem findByItem(Context context, Item item) throws SQLException {
        return workflowItemDAO.findByItem(context, item);
    }

    @Override
    public void update(Context context, WorkflowItem workflowItem) throws SQLException, AuthorizeException {
        // FIXME check auth
        log.info(LogManager.getHeader(context, "update_workflow_item",
                                      "workflowitem_id=" + workflowItem.getID()));

        // Update the item
        itemService.update(context, workflowItem.getItem());

        workflowItemDAO.save(context, workflowItem);
    }

    @Override
    public void deleteWrapper(Context context, WorkflowItem workflowItem) throws SQLException, AuthorizeException {
        List<WorkflowItemRole> roles = workflowItemRoleService.findByWorkflowItem(context, workflowItem);
        Iterator<WorkflowItemRole> workflowItemRoleIterator = roles.iterator();
        while (workflowItemRoleIterator.hasNext()) {
            WorkflowItemRole workflowItemRole = workflowItemRoleIterator.next();
            workflowItemRoleIterator.remove();
            workflowItemRoleService.delete(context, workflowItemRole);
        }

        poolTaskService.deleteByWorkflowItem(context, workflowItem);
        workflowRequirementsService.clearInProgressUsers(context, workflowItem);
        claimedTaskService.deleteByWorkflowItem(context, workflowItem);

        // FIXME - auth?
        workflowItemDAO.delete(context, workflowItem);
    }


    @Override
    public void move(Context context, WorkflowItem inProgressSubmission, Collection fromCollection,
                     Collection toCollection) {
        // TODO not implemented yet
    }
}
