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
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hibernate.Session;

/**
 * Service interface class for the ClaimedTask object.
 * The implementation of this class is responsible for all business logic calls for the ClaimedTask object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ClaimedTaskService extends DSpaceCRUDService<ClaimedTask> {

    public List<ClaimedTask> findByWorkflowItem(Session session, XmlWorkflowItem workflowItem) throws SQLException;

    /**
     * Find the single task for a given workflow item claimed by a given EPerson.
     * @param session the current request's database context.
     * @param workflowItem find task for this item.
     * @param ePerson find task claimed by this EPerson.
     * @return the single matching task, or null if none.
     * @throws SQLException passed through.
     */
    public ClaimedTask findByWorkflowIdAndEPerson(Session session, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException;

    public List<ClaimedTask> findByEperson(Session session, EPerson ePerson) throws SQLException;

    public List<ClaimedTask> find(Session session, XmlWorkflowItem workflowItem, String stepID) throws SQLException;

    public ClaimedTask find(Session session, EPerson ePerson, XmlWorkflowItem workflowItem, String stepID,
                            String actionID) throws SQLException;

    public List<ClaimedTask> find(Session session, XmlWorkflowItem workflowItem, String stepID, String actionID)
        throws SQLException;

    /**
     * Find all claimed tasks for a given workflow item.
     *
     * @param session current request's database context.
     * @param workflowItem the given workflow item.
     * @return all claimed tasks for that item.
     * @throws SQLException passed through.
     */
    public List<ClaimedTask> find(Session session, XmlWorkflowItem workflowItem) throws SQLException;

    public List<ClaimedTask> findAllInStep(Session session, String stepID) throws SQLException;

    public void deleteByWorkflowItem(Context context, XmlWorkflowItem workflowItem)
        throws SQLException, AuthorizeException;

    List<ClaimedTask> findAll(Session session) throws SQLException;
}
