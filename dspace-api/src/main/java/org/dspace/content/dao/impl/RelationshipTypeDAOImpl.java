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
import jakarta.persistence.criteria.Root;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.RelationshipType_;
import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the RelationshipType object.
 * This class is responsible for all database calls for the RelationshipType
 * object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RelationshipTypeDAOImpl extends AbstractHibernateDAO<RelationshipType> implements RelationshipTypeDAO {

    @Override
    public RelationshipType findbyTypesAndTypeName(Context context, EntityType leftType, EntityType rightType,
                                                 String leftwardType, String rightwardType)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RelationshipType.class);
        Root<RelationshipType> relationshipTypeRoot = criteriaQuery.from(RelationshipType.class);
        criteriaQuery.select(relationshipTypeRoot);
        criteriaQuery.where(
            criteriaBuilder.and(
                criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.leftType), leftType),
                criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.rightType), rightType),
                criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.leftwardType), leftwardType),
                criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.rightwardType), rightwardType)));
        return uniqueResult(context, criteriaQuery, false, RelationshipType.class);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String type) throws SQLException {

        return findByLeftwardOrRightwardTypeName(context, type, -1, -1);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String type, Integer limit,
                                                                    Integer offset)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RelationshipType.class);
        Root<RelationshipType> relationshipTypeRoot = criteriaQuery.from(RelationshipType.class);
        criteriaQuery.select(relationshipTypeRoot);
        criteriaQuery.where(
            criteriaBuilder.or(
                criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.leftwardType), type),
                criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.rightwardType), type)
            )
        );
        return list(context, criteriaQuery, true, RelationshipType.class, limit, offset);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType) throws SQLException {
        return findByEntityType(context, entityType, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType,
                                                   Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RelationshipType.class);
        Root<RelationshipType> relationshipTypeRoot = criteriaQuery.from(RelationshipType.class);
        criteriaQuery.select(relationshipTypeRoot);
        criteriaQuery.where(
            criteriaBuilder.or(criteriaBuilder.
                                    equal(relationshipTypeRoot.get(RelationshipType_.leftType), entityType),
                               criteriaBuilder
                                   .equal(relationshipTypeRoot.get(RelationshipType_.rightType), entityType)
            )
        );
        List<jakarta.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(relationshipTypeRoot.get(RelationshipType_.ID)));
        criteriaQuery.orderBy(orderList);
        return list(context, criteriaQuery, false, RelationshipType.class, limit, offset);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType, Boolean isLeft)
            throws SQLException {
        return findByEntityType(context, entityType, isLeft, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType, Boolean isLeft,
                                                   Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RelationshipType.class);
        Root<RelationshipType> relationshipTypeRoot = criteriaQuery.from(RelationshipType.class);
        criteriaQuery.select(relationshipTypeRoot);
        if (isLeft) {
            criteriaQuery.where(
                    criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.leftType), entityType)
            );
        } else {
            criteriaQuery.where(
                    criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.rightType), entityType)
            );
        }
        return list(context, criteriaQuery, false, RelationshipType.class, limit, offset);
    }

    @Override
    public int countByEntityType(Context context, EntityType entityType) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<RelationshipType> relationshipTypeRoot = criteriaQuery.from(RelationshipType.class);
        criteriaQuery.select(criteriaBuilder.count(relationshipTypeRoot));
        criteriaQuery.where(criteriaBuilder.or(
                            criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.leftType), entityType),
                            criteriaBuilder.equal(relationshipTypeRoot.get(RelationshipType_.rightType), entityType)
                            ));
        return count(context, criteriaQuery, criteriaBuilder, relationshipTypeRoot);
    }

}
