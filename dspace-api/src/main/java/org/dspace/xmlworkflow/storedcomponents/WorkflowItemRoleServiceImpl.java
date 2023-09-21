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
import org.dspace.xmlworkflow.storedcomponents.dao.WorkflowItemRoleDAO;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the WorkflowItemRole object.
 * This class is responsible for all business logic calls for the WorkflowItemRole object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkflowItemRoleServiceImpl implements WorkflowItemRoleService {

    @Autowired(required = true)
    private WorkflowItemRoleDAO workflowItemRoleDAO;

    protected WorkflowItemRoleServiceImpl() {

    }

    @Override
    public List<WorkflowItemRole> find(Session session, XmlWorkflowItem workflowItem, String role) throws SQLException {
        return workflowItemRoleDAO.findByWorkflowItemAndRole(session, workflowItem, role);
    }

    @Override
    public List<WorkflowItemRole> findByWorkflowItem(Session session, XmlWorkflowItem xmlWorkflowItem)
        throws SQLException {
        return workflowItemRoleDAO.findByWorkflowItem(session, xmlWorkflowItem);
    }

    @Override
    public void deleteForWorkflowItem(Context context, XmlWorkflowItem xmlWorkflowItem)
        throws SQLException, AuthorizeException {
        Iterator<WorkflowItemRole> workflowItemRoles
                = findByWorkflowItem(context.getSession(), xmlWorkflowItem).iterator();
        while (workflowItemRoles.hasNext()) {
            WorkflowItemRole workflowItemRole = workflowItemRoles.next();
            workflowItemRoles.remove();
            delete(context, workflowItemRole);
        }
    }

    @Override
    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException, AuthorizeException {
        Iterator<WorkflowItemRole> workflowItemRoles = findByEPerson(context.getSession(), ePerson).iterator();
        while (workflowItemRoles.hasNext()) {
            WorkflowItemRole workflowItemRole = workflowItemRoles.next();
            workflowItemRoles.remove();
            delete(context, workflowItemRole);
        }
    }

    @Override
    public List<WorkflowItemRole> findByEPerson(Session session, EPerson ePerson) throws SQLException {
        return workflowItemRoleDAO.findByEPerson(session, ePerson);
    }

    @Override
    public WorkflowItemRole create(Context context) throws SQLException, AuthorizeException {
        return workflowItemRoleDAO.create(context.getSession(), new WorkflowItemRole());
    }

    @Override
    public WorkflowItemRole find(Session session, int id) throws SQLException {
        return workflowItemRoleDAO.findByID(session, WorkflowItemRole.class, id);
    }

    @Override
    public void update(Context context, WorkflowItemRole workflowItemRole) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(workflowItemRole));
    }

    @Override
    public void update(Context context, List<WorkflowItemRole> workflowItemRoles)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(workflowItemRoles)) {
            for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
                workflowItemRoleDAO.save(context.getSession(), workflowItemRole);
            }
        }
    }

    @Override
    public void delete(Context context, WorkflowItemRole workflowItemRole) throws SQLException, AuthorizeException {
        workflowItemRoleDAO.delete(context.getSession(), workflowItemRole);
    }
}
