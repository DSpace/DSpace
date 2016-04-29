/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the WorkflowItemRole object.
 * The implementation of this class is responsible for all database calls for the WorkflowItemRole object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowItemRoleDAO extends GenericDAO<WorkflowItemRole> {

    public List<WorkflowItemRole> findByWorkflowItemAndRole(Context context, XmlWorkflowItem workflowItem, String role) throws SQLException;

    public List<WorkflowItemRole> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public List<WorkflowItemRole> findByEPerson(Context context, EPerson ePerson) throws SQLException;
}
