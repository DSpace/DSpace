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
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the ClaimedTask object.
 * The implementation of this class is responsible for all database calls for the ClaimedTask object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ClaimedTaskDAO extends GenericDAO<ClaimedTask> {

    public List<ClaimedTask> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public ClaimedTask findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson) throws SQLException;

    public List<ClaimedTask> findByEperson(Context context, EPerson ePerson) throws SQLException;

    public List<ClaimedTask> findByWorkflowItemAndStepId(Context context, XmlWorkflowItem workflowItem, String stepID) throws SQLException;

    public ClaimedTask findByEPersonAndWorkflowItemAndStepIdAndActionId(Context context, EPerson ePerson, XmlWorkflowItem workflowItem, String stepID, String actionID) throws SQLException;

    public List<ClaimedTask> findByWorkflowItemAndStepIdAndActionId(Context c, XmlWorkflowItem workflowItem, String stepID, String actionID) throws SQLException;

    public List<ClaimedTask> findByStep(Context context, String stepID) throws SQLException;
}
