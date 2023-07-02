/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.content.DSpaceObject;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.SubscriptionParameter_;
import org.dspace.eperson.Subscription_;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the Subscription object.
 * This class is responsible for all database calls for the Subscription object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class SubscriptionDAOImpl extends AbstractHibernateDAO<Subscription> implements SubscriptionDAO {

    protected SubscriptionDAOImpl() {
        super();
    }

    @Override
    public List<Subscription> findByEPerson(Session session, EPerson eperson, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        javax.persistence.criteria.CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Subscription.class);
        Root<Subscription> subscriptionRoot = criteriaQuery.from(Subscription.class);
        criteriaQuery.select(subscriptionRoot);
        criteriaQuery.where(criteriaBuilder.equal(subscriptionRoot.get(Subscription_.ePerson), eperson));
        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(subscriptionRoot.get(Subscription_.dSpaceObject)));
        criteriaQuery.orderBy(orderList);
        return list(session, criteriaQuery, false, Subscription.class, limit, offset);
    }

    @Override
    public List<Subscription> findByEPersonAndDso(Session session, EPerson eperson,
                                                  DSpaceObject dSpaceObject,
                                                  Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        javax.persistence.criteria.CriteriaQuery criteriaQuery =
                getCriteriaQuery(criteriaBuilder, Subscription.class);
        Root<Subscription> subscriptionRoot = criteriaQuery.from(Subscription.class);
        criteriaQuery.select(subscriptionRoot);
        criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(
                            subscriptionRoot.get(Subscription_.ePerson), eperson),
                criteriaBuilder.equal(subscriptionRoot.get(Subscription_.dSpaceObject), dSpaceObject)
        ));
        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(subscriptionRoot.get(Subscription_.dSpaceObject)));
        criteriaQuery.orderBy(orderList);
        return list(session, criteriaQuery, false, Subscription.class, limit, offset);
    }

    @Override
    public void deleteByDspaceObject(Session session, DSpaceObject dSpaceObject) throws SQLException {
        String hqlQuery = "delete from Subscription where dSpaceObject=:dSpaceObject";
        Query query = createQuery(session, hqlQuery);
        query.setParameter("dSpaceObject", dSpaceObject);
        query.executeUpdate();
    }

    @Override
    public void deleteByEPerson(Session session, EPerson eperson) throws SQLException {
        String hqlQuery = "delete from Subscription where ePerson=:ePerson";
        Query query = createQuery(session, hqlQuery);
        query.setParameter("ePerson", eperson);
        query.executeUpdate();
    }

    @Override
    public void deleteByDSOAndEPerson(Session session, DSpaceObject dSpaceObject, EPerson eperson)
            throws SQLException {
        String hqlQuery = "delete from Subscription where dSpaceObject=:dSpaceObject AND ePerson=:ePerson";
        Query query = createQuery(session, hqlQuery);
        query.setParameter("dSpaceObject", dSpaceObject);
        query.setParameter("ePerson", eperson);
        query.executeUpdate();
    }

    @Override
    public List<Subscription> findAllOrderedByIDAndResourceType(Session session, String resourceType,
                                                                Integer limit, Integer offset) throws SQLException {
        String hqlQuery = "select s from Subscription s join %s dso " +
                "ON dso.id = s.dSpaceObject ORDER BY subscription_id";
        if (resourceType != null) {
            hqlQuery = String.format(hqlQuery, resourceType);
        }
        Query query = createQuery(session, hqlQuery);
        if (limit != -1) {
            query.setMaxResults(limit);
        }
        if (offset != -1) {
            query.setFirstResult(offset);
        }
        query.setHint("org.hibernate.cacheable", false);
        return query.getResultList();
    }

    @Override
    public List<Subscription> findAllOrderedByDSO(Session session, Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Subscription.class);
        Root<Subscription> subscriptionRoot = criteriaQuery.from(Subscription.class);
        criteriaQuery.select(subscriptionRoot);
        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(subscriptionRoot.get(Subscription_.dSpaceObject)));
        criteriaQuery.orderBy(orderList);
        return list(session, criteriaQuery, false, Subscription.class, limit, offset);
    }

    @Override
    public List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Session session,
                        String subscriptionType, String frequencyValue) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Subscription.class);
        Root<Subscription> subscriptionRoot = criteriaQuery.from(Subscription.class);
        criteriaQuery.select(subscriptionRoot);
        Join<Subscription, SubscriptionParameter> childJoin = subscriptionRoot.join("subscriptionParameterList");
        criteriaQuery.where(
                criteriaBuilder.and(
                criteriaBuilder.equal(subscriptionRoot.get(Subscription_.SUBSCRIPTION_TYPE), subscriptionType),
                criteriaBuilder.equal(childJoin.get(SubscriptionParameter_.name), "frequency"),
                criteriaBuilder.equal(childJoin.get(SubscriptionParameter_.value), frequencyValue)
                ));
        List<javax.persistence.criteria.Order> orderList = new ArrayList<>(1);
        orderList.add(criteriaBuilder.asc(subscriptionRoot.get(Subscription_.ePerson)));
        orderList.add(criteriaBuilder.asc(subscriptionRoot.get(Subscription_.id)));
        criteriaQuery.orderBy(orderList);
        return list(session, criteriaQuery, false, Subscription.class, 10000, -1);
    }

    @Override
    public Long countAll(Session session) throws SQLException {
        CriteriaBuilder qb = getCriteriaBuilder(session);
        CriteriaQuery<Long> cq = qb.createQuery(Long.class);
        cq.select(qb.count(cq.from(Subscription.class)));
        Query query = session.createQuery(cq);
        return (Long) query.getSingleResult();
    }

    @Override
    public Long countAllByEPerson(Session session, EPerson ePerson) throws SQLException {
        CriteriaBuilder qb = getCriteriaBuilder(session);
        CriteriaQuery<Long> cq = qb.createQuery(Long.class);
        Root<Subscription> subscriptionRoot = cq.from(Subscription.class);
        cq.select(qb.count(subscriptionRoot));
        cq.where(qb.equal(subscriptionRoot.get(Subscription_.ePerson), ePerson));
        Query query = session.createQuery(cq);
        return (Long) query.getSingleResult();
    }

    @Override
    public Long countAllByEPersonAndDso(Session session,
          EPerson ePerson, DSpaceObject dSpaceObject) throws SQLException {
        CriteriaBuilder qb = getCriteriaBuilder(session);
        CriteriaQuery<Long> cq = qb.createQuery(Long.class);
        Root<Subscription> subscriptionRoot = cq.from(Subscription.class);
        cq.select(qb.count(subscriptionRoot));
        cq.where(qb.and(qb.equal(subscriptionRoot.get(Subscription_.ePerson)
                , ePerson), qb.equal(subscriptionRoot.get(Subscription_.dSpaceObject), dSpaceObject)));
        Query query = session.createQuery(cq);
        return (Long) query.getSingleResult();
    }
}