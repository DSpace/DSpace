/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.dspace.content.EntityType;
import org.dspace.content.EntityType_;
import org.dspace.content.dao.EntityTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the EntityType object.
 * This class is responsible for all database calls for the EntityType object
 * and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EntityTypeDAOImpl extends AbstractHibernateDAO<EntityType> implements EntityTypeDAO {

    @Override
    public EntityType findByEntityType(Context context, String entityType) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EntityType.class);
        Root<EntityType> entityTypeRoot = criteriaQuery.from(EntityType.class);
        criteriaQuery.select(entityTypeRoot);
        criteriaQuery.where(criteriaBuilder.equal(criteriaBuilder.upper(entityTypeRoot.get(EntityType_.label)),
                                                  entityType.toUpperCase()));
        return uniqueResult(context, criteriaQuery, true, EntityType.class);
    }

    @Override
    public List<EntityType> getEntityTypesByNames(Context context, List<String> names, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EntityType.class);
        Root<EntityType> entityTypeRoot = criteriaQuery.from(EntityType.class);
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(entityTypeRoot.get(EntityType_.label)));
        criteriaQuery.select(entityTypeRoot).orderBy(orderList);
        criteriaQuery.where(entityTypeRoot.get(EntityType_.LABEL).in(names));
        return list(context, criteriaQuery, false, EntityType.class, limit, offset);
    }

    @Override
    public int countEntityTypesByNames(Context context, List<String> names) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<EntityType> entityTypeRoot = criteriaQuery.from(EntityType.class);
        criteriaQuery.select(criteriaBuilder.count(entityTypeRoot));
        criteriaQuery.where(entityTypeRoot.get(EntityType_.LABEL).in(names));
        return count(context, criteriaQuery, criteriaBuilder, entityTypeRoot);
    }

}
