/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.dao;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.BasicWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the BasicWorkflowItem object.
 * The implementation of this class is responsible for all database calls for the BasicWorkflowItem object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BasicWorkflowItemDAO extends GenericDAO<BasicWorkflowItem> {

    public BasicWorkflowItem findByItem(Context context, Item i) throws SQLException;

    public List<BasicWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException;

    public List<BasicWorkflowItem> findByCollection(Context context, Collection c) throws SQLException;

    public List<BasicWorkflowItem> findByPooledTasks(Context context, EPerson ePerson) throws SQLException;

    public List<BasicWorkflowItem> findByOwner(Context context, EPerson ePerson) throws SQLException;

    int countRows(Context context) throws SQLException;
}
