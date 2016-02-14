/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.dao.impl;

import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.TaskListItem;
import org.dspace.workflowbasic.dao.TaskListItemDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the TaskListItem object.
 * This class is responsible for all database calls for the TaskListItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class TaskListItemDAOImpl extends AbstractHibernateDAO<TaskListItem> implements TaskListItemDAO
{
    protected TaskListItemDAOImpl()
    {
        super();
    }

    @Override
    public void deleteByWorkflowItem(Context context, BasicWorkflowItem workflowItem) throws SQLException {
        String queryString = "delete from TaskListItem where workflowItem = :workflowItem";
        Query query = createQuery(context, queryString);
        query.setParameter("workflowItem", workflowItem);
        query.executeUpdate();
    }

    @Override
    public List<TaskListItem> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        Criteria criteria = createCriteria(context, TaskListItem.class);
        criteria.add(Restrictions.eq("ePerson", ePerson));
        return list(criteria);
    }
}
