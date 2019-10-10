/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.RelationshipType_;
import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class RelationshipTypeDAOImpl extends AbstractHibernateDAO<RelationshipType> implements RelationshipTypeDAO {

    @Override
    public RelationshipType findByTypesAndLabels(Context context, EntityType leftType, EntityType rightType,
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
        return uniqueResult(context, criteriaQuery, false, RelationshipType.class, -1, -1);
    }

    @Override
    public List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String type) throws SQLException {
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
        return list(context, criteriaQuery, true, RelationshipType.class, -1, -1);
    }

    @Override
    public List<RelationshipType> findByEntityType(Context context, EntityType entityType) throws SQLException {
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
        return list(context, criteriaQuery, false, RelationshipType.class, -1, -1);
    }

}
