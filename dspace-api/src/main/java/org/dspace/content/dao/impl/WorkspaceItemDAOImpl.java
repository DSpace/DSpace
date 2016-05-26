/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.BasicTransformerAdapter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation of the Database Access Object interface class for the WorkspaceItem object.
 * This class is responsible for all database calls for the WorkspaceItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemDAOImpl extends AbstractHibernateDAO<WorkspaceItem> implements WorkspaceItemDAO
{
    protected WorkspaceItemDAOImpl()
    {
        super();
    }


    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep) throws SQLException
    {
        Query query = createQuery(context, "from WorkspaceItem ws where ws.item.submitter = :submitter order by workspaceItemId");
        query.setParameter("submitter", ep);
        return list(query);
    }

    @Override
    public List<WorkspaceItem> findByCollection(Context context, Collection c) throws SQLException
    {
        Criteria criteria = createCriteria(context, WorkspaceItem.class);
        criteria.add(Restrictions.eq("collection", c));
        return list(criteria);
    }

    @Override
    public WorkspaceItem findByItem(Context context, Item i) throws SQLException
    {
        Criteria criteria = createCriteria(context, WorkspaceItem.class);
        criteria.add(Restrictions.eq("item", i));
        // Look for the unique workspaceitem entry where 'item_id' references this item
        return uniqueResult(criteria);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context) throws SQLException
    {
        Criteria criteria = createCriteria(context, WorkspaceItem.class);
        criteria.addOrder(Order.asc("item"));
        return list(criteria);
    }

    @Override
    public List<WorkspaceItem> findWithSupervisedGroup(Context context) throws SQLException {
        Criteria criteria = createCriteria(context, WorkspaceItem.class);
        criteria.add(Restrictions.isNotEmpty("supervisorGroups"));
        criteria.addOrder(Order.asc("workspaceItemId"));
        return list(criteria);
    }

    @Override
    public List<WorkspaceItem> findBySupervisedGroupMember(Context context, EPerson ePerson) throws SQLException {
        Criteria criteria = createCriteria(context, WorkspaceItem.class);
        criteria.createAlias("supervisorGroups", "supervisorGroup");
        criteria.createAlias("supervisorGroup.epeople", "person");
        criteria.add(Restrictions.eq("person.id", ePerson.getID()));
        return list(criteria);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from WorkspaceItem"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException {
        Query query = createQuery(context,"SELECT wi.stageReached as stage_reached, count(*) as cnt from WorkspaceItem wi" +
                " group by wi.stageReached order by wi.stageReached");
        query.setResultTransformer(new BasicTransformerAdapter() {
            @Override
            public Object transformTuple(Object[] tuple, String[] aliases) {
                return new java.util.AbstractMap.SimpleImmutableEntry((Integer) tuple[0], (Long) tuple[1]);
            }
        });
        return (List<Map.Entry<Integer, Long>>)query.list();
    }

}
