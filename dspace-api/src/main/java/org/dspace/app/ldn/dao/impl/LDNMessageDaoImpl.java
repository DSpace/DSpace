/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.LDNMessageEntity_;
import org.dspace.app.ldn.dao.LDNMessageDao;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the LDNMessage object. This class is responsible for all database calls for
 * the LDNMessage object and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageDaoImpl extends AbstractHibernateDAO<LDNMessageEntity> implements LDNMessageDao {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNMessageDaoImpl.class);

    @Override
    public List<LDNMessageEntity> findOldestMessageToProcess(Context context, int max_attempts) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<LDNMessageEntity> criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>(3);
        andPredicates
            .add(criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus), LDNMessageEntity.QUEUE_STATUS_QUEUED));
        andPredicates.add(criteriaBuilder.lessThan(root.get(LDNMessageEntity_.queueAttempts), max_attempts));
        andPredicates.add(criteriaBuilder.lessThan(root.get(LDNMessageEntity_.queueTimeout), new Date()));
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        criteriaQuery.orderBy(orderList);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages found to be processed");
        }
        return result;
    }

    @Override
    public List<LDNMessageEntity> findMessagesToBeReprocessed(Context context) throws SQLException {
        // looking for LDN Messages to be reprocessed message
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<LDNMessageEntity> criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>(1);
        andPredicates
            .add(criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus),
                LDNMessageEntity.QUEUE_STATUS_QUEUED_FOR_RETRY));
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        criteriaQuery.orderBy(orderList);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages found to be processed");
        }
        return result;
    }

    @Override
    public List<LDNMessageEntity> findProcessingTimedoutMessages(Context context, int max_attempts)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<LDNMessageEntity> criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>(3);
        andPredicates.add(
            criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus), LDNMessageEntity.QUEUE_STATUS_PROCESSING));
        andPredicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(LDNMessageEntity_.queueAttempts), max_attempts));
        andPredicates.add(criteriaBuilder.lessThan(root.get(LDNMessageEntity_.queueTimeout), new Date()));
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        criteriaQuery.orderBy(orderList);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages found to be processed");
        }
        return result;
    }

    @Override
    public List<LDNMessageEntity> findAllRelatedMessagesByItem(
        Context context, LDNMessageEntity msg, Item item, String... relatedTypes) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<LDNMessageEntity> criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>();
        Predicate relatedtypePredicate = null;
        andPredicates.add(
            criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus), LDNMessageEntity.QUEUE_STATUS_PROCESSED));
        andPredicates.add(
            criteriaBuilder.isNull(root.get(LDNMessageEntity_.target)));
        andPredicates.add(
            criteriaBuilder.equal(root.get(LDNMessageEntity_.inReplyTo), msg));
        if (relatedTypes != null && relatedTypes.length > 0) {
            relatedtypePredicate = root.get(LDNMessageEntity_.activityStreamType).in(relatedTypes);
            andPredicates.add(relatedtypePredicate);
        }
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        criteriaQuery.orderBy(orderList);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages ACK found to be processed");
        }
        return result;
    }

    @Override
    public List<LDNMessageEntity> findAllMessagesByItem(
        Context context, Item item, String... activities) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<LDNMessageEntity> criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>();
        Predicate activityPredicate = null;
        andPredicates.add(
            criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus), LDNMessageEntity.QUEUE_STATUS_PROCESSED));
        andPredicates.add(
            criteriaBuilder.equal(root.get(LDNMessageEntity_.object), item));
        if (activities != null && activities.length > 0) {
            activityPredicate = root.get(LDNMessageEntity_.activityStreamType).in(activities);
            andPredicates.add(activityPredicate);
        }
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        criteriaQuery.orderBy(orderList);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages found");
        }
        return result;
    }
}
