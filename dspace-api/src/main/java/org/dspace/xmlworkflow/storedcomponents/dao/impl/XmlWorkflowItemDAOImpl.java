/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao.impl;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.dao.XmlWorkflowItemDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the XmlWorkflowItem object.
 * This class is responsible for all database calls for the XmlWorkflowItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class XmlWorkflowItemDAOImpl extends AbstractHibernateDAO<XmlWorkflowItem> implements XmlWorkflowItemDAO
{

    protected XmlWorkflowItemDAOImpl()
    {
        super();
    }

    @Override
    public List<XmlWorkflowItem> findAllInCollection(Context context, Integer offset, Integer limit, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, XmlWorkflowItem.class);
        if(collection != null)
        {
            criteria.add(Restrictions.eq("collection", collection));
        }

        if(offset != null)
        {
            criteria.setFirstResult(offset);
        }
        if(limit != null)
        {
            criteria.setMaxResults(limit);
        }

        return list(criteria);
    }

    @Override
    public int countAll(Context context) throws SQLException {
        return countAllInCollection(context, null);
    }

    @Override
    public int countAllInCollection(Context context, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, XmlWorkflowItem.class);
        if(collection != null)
        {
            criteria.add(Restrictions.eq("collection", collection));
        }
        return count(criteria);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {
        Criteria criteria = createCriteria(context, XmlWorkflowItem.class);
        criteria.createAlias("item", "i");
        criteria.add(Restrictions.eq("i.submitter", ep));

        return list(criteria);
    }

    @Override
    public List<XmlWorkflowItem> findByCollection(Context context, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, XmlWorkflowItem.class);
        criteria.add(Restrictions.eq("collection", collection));

        return list(criteria);
    }

    @Override
    public XmlWorkflowItem findByItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, XmlWorkflowItem.class);
        criteria.add(Restrictions.eq("item", item));

        return uniqueResult(criteria);
    }
}
