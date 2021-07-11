/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Database Access Object interface class for the ClaimedTask object.
 * The implementation of this class is responsible for all database calls for the ClaimedTask object and is autowired
 * by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ClaimedTaskDAO extends GenericDAO<ClaimedTask> {

    /**
     * Find all claimed tasks for a given workflow item.
     *
     * @param context current DSpace session.
     * @param workflowItem the interesting workflow item.
     * @return all claimed tasks for that item.
     * @throws SQLException passed through.
     */
    public List<ClaimedTask> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    /**
     * Find the single task for a given workflow item which is claimed by a given EPerson.
     *
     * @param context
     * @param workflowItem find task for this item.
     * @param ePerson find task claimed by this EPerson.
     * @return the matching task, or null if none.
     * @throws SQLException if query cannot be created or fails.
     */
    public ClaimedTask findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException;

    public List<ClaimedTask> findByEperson(Context context, EPerson ePerson) throws SQLException;

    public List<ClaimedTask> findByWorkflowItemAndStepId(Context context, XmlWorkflowItem workflowItem, String stepID)
        throws SQLException;

    public ClaimedTask findByEPersonAndWorkflowItemAndStepIdAndActionId(Context context, EPerson ePerson,
                                                                        XmlWorkflowItem workflowItem, String stepID,
                                                                        String actionID) throws SQLException;

    public List<ClaimedTask> findByWorkflowItemAndStepIdAndActionId(Context c, XmlWorkflowItem workflowItem,
                                                                    String stepID, String actionID) throws SQLException;

    public List<ClaimedTask> findByStep(Context context, String stepID) throws SQLException;
}
