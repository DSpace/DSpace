/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the ClaimedTask object.
 * The implementation of this class is responsible for all business logic calls for the ClaimedTask object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ClaimedTaskService extends DSpaceCRUDService<ClaimedTask> {

    public List<ClaimedTask> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public ClaimedTask findByWorkflowIdAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
            throws SQLException;

    public List<ClaimedTask> findByEperson(Context context, EPerson ePerson) throws SQLException;

    public List<ClaimedTask> find(Context context, XmlWorkflowItem workflowItem, String stepID) throws SQLException;

    public ClaimedTask find(Context context, EPerson ePerson, XmlWorkflowItem workflowItem, String stepID, String actionID) throws SQLException;

    public List<ClaimedTask> find(Context context, XmlWorkflowItem workflowItem, String stepID, String actionID) throws SQLException;

    public List<ClaimedTask> find(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public List<ClaimedTask> findAllInStep(Context context, String stepID) throws SQLException;

    public void deleteByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException;
}
