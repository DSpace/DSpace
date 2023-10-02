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

import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.InProgressUser;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the InProgressUser object.
 * The implementation of this class is responsible for all database calls for the InProgressUser object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface InProgressUserDAO extends GenericDAO<InProgressUser> {

    public InProgressUser findByWorkflowItemAndEPerson(Session session, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException;

    public List<InProgressUser> findByEperson(Session session, EPerson ePerson) throws SQLException;

    public List<InProgressUser> findByWorkflowItem(Session session, XmlWorkflowItem workflowItem) throws SQLException;

    public int countInProgressUsers(Session session, XmlWorkflowItem workflowItem) throws SQLException;

    public int countFinishedUsers(Session session, XmlWorkflowItem workflowItem) throws SQLException;
}
