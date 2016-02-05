/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.dao.impl;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.dao.BasicWorkflowItemDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the BasicWorkflowItem object.
 * This class is responsible for all database calls for the BasicWorkflowItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BasicWorkflowItemDAOImpl extends AbstractHibernateDAO<BasicWorkflowItem> implements BasicWorkflowItemDAO
{
    protected BasicWorkflowItemDAOImpl()
    {
        super();
    }


    @Override
    public BasicWorkflowItem findByItem(Context context, Item i) throws SQLException {
        Criteria criteria = createCriteria(context, BasicWorkflowItem.class);
        criteria.add(Restrictions.eq("item", i));
        // Look for the unique WorkflowItem entry where 'item_id' references this item
        return uniqueResult(criteria);
    }

    @Override
    public List<BasicWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException
    {
        Criteria criteria = createCriteria(context, BasicWorkflowItem.class);
        criteria.createAlias("item", "i");
        criteria.add(Restrictions.eq("i.submitter", ep));
        criteria.addOrder(Order.asc("workflowitemId"));
        return list(criteria);

    }

    @Override
    public List<BasicWorkflowItem> findByCollection(Context context, Collection c) throws SQLException
    {
        Criteria criteria = createCriteria(context, BasicWorkflowItem.class);
        criteria.add(Restrictions.eq("collection", c));
        return list(criteria);
    }

    @Override
    public List<BasicWorkflowItem> findByPooledTasks(Context context, EPerson ePerson) throws SQLException
    {
        String queryString = "select wf from TaskListItem as tli join tli.workflowItem wf where tli.ePerson = :eperson ORDER BY wf.workflowitemId";
        Query query = createQuery(context, queryString);
        query.setParameter("eperson", ePerson);
        return list(query);
    }

    @Override
    public List<BasicWorkflowItem> findByOwner(Context context, EPerson ePerson) throws SQLException {
        Criteria criteria = createCriteria(context, BasicWorkflowItem.class);
        criteria.add(Restrictions.eq("owner", ePerson));
        return list(criteria);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM BasicWorkflowItem"));
    }
}
