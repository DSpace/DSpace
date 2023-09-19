/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

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
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.storedcomponents.dao.XmlWorkflowItemDAO;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the XmlWorkflowItem object.
 * This class is responsible for all business logic calls for the XmlWorkflowItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class XmlWorkflowItemServiceImpl implements XmlWorkflowItemService {

    @Autowired(required = true)
    protected XmlWorkflowItemDAO xmlWorkflowItemDAO;


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
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger();


    protected XmlWorkflowItemServiceImpl() {

    }

    @Override
    public XmlWorkflowItem create(Context context, Item item, Collection collection)
        throws SQLException, AuthorizeException {
        XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemDAO.create(context.getSession(), new XmlWorkflowItem());
        xmlWorkflowItem.setItem(item);
        xmlWorkflowItem.setCollection(collection);
        return xmlWorkflowItem;
    }

    @Override
    public XmlWorkflowItem find(Session session, int id) throws SQLException {
        XmlWorkflowItem workflowItem = xmlWorkflowItemDAO.findByID(session, XmlWorkflowItem.class, id);

        if (workflowItem == null) {
            log.debug("find_workflow_item not_found,workflowitem_id={}", id);
        } else {
            log.debug("find_workflow_item workflowitem_id={}", id);
        }
        return workflowItem;
    }

    @Override
    public List<XmlWorkflowItem> findAll(Session session) throws SQLException {
        return xmlWorkflowItemDAO.findAll(session, XmlWorkflowItem.class);
    }

    @Override
    public List<XmlWorkflowItem> findAll(Session session, Integer page, Integer pagesize) throws SQLException {
        return findAllInCollection(session, page, pagesize, null);
    }

    @Override
    public List<XmlWorkflowItem> findAllInCollection(Session session, Integer page, Integer pagesize,
                                                     Collection collection) throws SQLException {
        Integer offset = null;
        if (page != null && pagesize != null) {
            offset = page * pagesize;
        }
        return xmlWorkflowItemDAO.findAllInCollection(session, offset, pagesize, collection);
    }

    @Override
    public int countAll(Context context) throws SQLException {
        return xmlWorkflowItemDAO.countAll(context.getSession());
    }

    @Override
    public int countAllInCollection(Context context, Collection collection) throws SQLException {
        return xmlWorkflowItemDAO.countAllInCollection(context.getSession(), collection);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Session session, EPerson ep) throws SQLException {
        return xmlWorkflowItemDAO.findBySubmitter(session, ep);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Session session, EPerson ep, Integer pageNumber, Integer pageSize)
            throws SQLException {
        Integer offset = null;
        if (pageNumber != null && pageSize != null) {
            offset = pageNumber * pageSize;
        }
        return xmlWorkflowItemDAO.findBySubmitter(session, ep, pageNumber, pageSize);
    }

    @Override
    public int countBySubmitter(Context context, EPerson ep) throws SQLException {
        return xmlWorkflowItemDAO.countBySubmitter(context.getSession(), ep);
    }

    @Override
    public void deleteByCollection(Context context, Collection collection)
        throws SQLException, IOException, AuthorizeException {
        List<XmlWorkflowItem> xmlWorkflowItems = findByCollection(context.getSession(), collection);
        Iterator<XmlWorkflowItem> iterator = xmlWorkflowItems.iterator();
        while (iterator.hasNext()) {
            XmlWorkflowItem workflowItem = iterator.next();
            iterator.remove();
            delete(context, workflowItem);
        }
    }

    @Override
    public void delete(Context context, XmlWorkflowItem workflowItem)
        throws SQLException, AuthorizeException, IOException {
        Item item = workflowItem.getItem();
        // Need to delete the workspaceitem row first since it refers
        // to item ID
        deleteWrapper(context, workflowItem);

        // Delete item
        itemService.delete(context, item);
    }

    @Override
    public List<XmlWorkflowItem> findByCollection(Session session, Collection collection) throws SQLException {
        return xmlWorkflowItemDAO.findByCollection(session, collection);
    }

    @Override
    public XmlWorkflowItem findByItem(Session session, Item item) throws SQLException {
        return xmlWorkflowItemDAO.findByItem(session, item);
    }

    @Override
    public void update(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException {
        // FIXME check auth
        log.info(LogHelper.getHeader(context, "update_workflow_item",
                                      "workflowitem_id=" + workflowItem.getID()));

        // Update the item
        itemService.update(context, workflowItem.getItem());

        xmlWorkflowItemDAO.save(context.getSession(), workflowItem);
    }

    @Override
    public void deleteWrapper(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException {
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
        xmlWorkflowItemDAO.delete(context.getSession(), workflowItem);
    }

    @Override
    public void move(Context context, XmlWorkflowItem inProgressSubmission, Collection fromCollection,
                     Collection toCollection) {
        // TODO not implemented yet
    }
}
