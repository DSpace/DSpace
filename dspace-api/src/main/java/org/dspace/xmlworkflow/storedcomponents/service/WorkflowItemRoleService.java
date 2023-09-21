/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hibernate.Session;

/**
 * Service interface class for the WorkflowItemRole object.
 * The implementation of this class is responsible for all business logic calls for the WorkflowItemRole object and
 * is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowItemRoleService extends DSpaceCRUDService<WorkflowItemRole> {

    public List<WorkflowItemRole> find(Session session, XmlWorkflowItem workflowItem, String role) throws SQLException;

    public List<WorkflowItemRole> findByWorkflowItem(Session session, XmlWorkflowItem xmlWorkflowItem)
        throws SQLException;

    public void deleteForWorkflowItem(Context context, XmlWorkflowItem wfi) throws SQLException, AuthorizeException;

    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException, AuthorizeException;

    public List<WorkflowItemRole> findByEPerson(Session session, EPerson ePerson) throws SQLException;
}
