/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao.impl;

import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.dao.PoolTaskDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the PoolTask object.
 * This class is responsible for all database calls for the PoolTask object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class PoolTaskDAOImpl extends AbstractHibernateDAO<PoolTask> implements PoolTaskDAO
{
    protected PoolTaskDAOImpl()
    {
        super();
    }

    @Override
    public List<PoolTask> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        Criteria criteria = createCriteria(context, PoolTask.class);
        criteria.add(Restrictions.eq("ePerson", ePerson));

        return list(criteria);
    }

    @Override
    public List<PoolTask> findByGroup(Context context, Group group) throws SQLException {
        Criteria criteria = createCriteria(context, PoolTask.class);
        criteria.add(Restrictions.eq("group", group));

        return list(criteria);
    }

    @Override
    public List<PoolTask> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        Criteria criteria = createCriteria(context, PoolTask.class);
        criteria.add(Restrictions.eq("workflowItem", workflowItem));

        return list(criteria);
    }

    @Override
    public PoolTask findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson) throws SQLException {
        Criteria criteria = createCriteria(context, PoolTask.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("workflowItem", workflowItem),
                        Restrictions.eq("ePerson", ePerson)
                )
        );

        return uniqueResult(criteria);
    }

    @Override
    public PoolTask findByWorkflowItemAndGroup(Context context, Group group, XmlWorkflowItem workflowItem) throws SQLException {
        Criteria criteria = createCriteria(context, PoolTask.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("workflowItem", workflowItem),
                        Restrictions.eq("group", group)
                )
        );

        return uniqueResult(criteria);
    }
}
