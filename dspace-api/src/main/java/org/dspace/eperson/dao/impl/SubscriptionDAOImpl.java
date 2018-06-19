/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the Subscription object.
 * This class is responsible for all database calls for the Subscription object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class SubscriptionDAOImpl extends AbstractHibernateDAO<Subscription> implements SubscriptionDAO
{
    protected SubscriptionDAOImpl()
    {
        super();
    }

    @Override
    public List<Subscription> findByEPerson(Context context, EPerson eperson) throws SQLException {
        Criteria criteria = createCriteria(context, Subscription.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("ePerson", eperson)
                )
        );
        return list(criteria);

    }

    @Override
    public Subscription findByCollectionAndEPerson(Context context, EPerson eperson, Collection collection) throws SQLException {
        Criteria criteria = createCriteria(context, Subscription.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("ePerson", eperson),
                        Restrictions.eq("collection", collection)
                )
        );
        return singleResult(criteria);
    }


    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException {
        String hqlQuery = "delete from Subscription where collection=:collection";
        Query query = createQuery(context, hqlQuery);
        query.setParameter("collection", collection);
        query.executeUpdate();
    }

    @Override
    public void deleteByEPerson(Context context, EPerson eperson) throws SQLException {
        String hqlQuery = "delete from Subscription where ePerson=:ePerson";
        Query query = createQuery(context, hqlQuery);
        query.setParameter("ePerson", eperson);
        query.executeUpdate();
    }

    @Override
    public void deleteByCollectionAndEPerson(Context context, Collection collection, EPerson eperson) throws SQLException {
        String hqlQuery = "delete from Subscription where collection=:collection AND ePerson=:ePerson";
        Query query = createQuery(context, hqlQuery);
        query.setParameter("collection", collection);
        query.setParameter("ePerson", eperson);
        query.executeUpdate();
    }

    @Override
    public List<Subscription> findAllOrderedByEPerson(Context context) throws SQLException {
        Criteria criteria = createCriteria(context, Subscription.class);
        criteria.addOrder(Order.asc("ePerson"));
        return list(criteria);
    }
}