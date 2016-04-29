/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the XmlWorkflowItem object.
 * The implementation of this class is responsible for all database calls for the XmlWorkflowItem object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface XmlWorkflowItemDAO extends GenericDAO<XmlWorkflowItem> {

    public List<XmlWorkflowItem> findAllInCollection(Context context, Integer offset, Integer limit, Collection collection) throws SQLException;

    public int countAll(Context context) throws SQLException;

    public int countAllInCollection(Context context, Collection collection) throws SQLException;

    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException;

    public List<XmlWorkflowItem> findByCollection(Context context, Collection collection) throws SQLException;

    public XmlWorkflowItem findByItem(Context context, Item item) throws SQLException;
}
