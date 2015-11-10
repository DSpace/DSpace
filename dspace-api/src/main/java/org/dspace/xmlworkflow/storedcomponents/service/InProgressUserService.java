/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.xmlworkflow.storedcomponents.InProgressUser;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the InProgressUser object.
 * The implementation of this class is responsible for all business logic calls for the InProgressUser object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface InProgressUserService extends DSpaceCRUDService<InProgressUser>
{
    public InProgressUser findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
            throws SQLException;

    public List<InProgressUser> findByEperson(Context context, EPerson ePerson) throws SQLException;

    public List<InProgressUser> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public int getNumberOfInProgressUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public int getNumberOfFinishedUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException;
}
