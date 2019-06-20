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

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.Relationship_;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class RelationshipDAOImpl extends AbstractHibernateDAO<Relationship> implements RelationshipDAO {

    @Override
    public List<Relationship> findByItem(Context context, Item item) throws SQLException {

        return findByItem(context, item, -1, -1);
    }

    @Override
    public List<Relationship> findByItem(Context context, Item item, Integer limit, Integer offset)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
            .where(criteriaBuilder.or(criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item),
                                      criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item)));
        return list(context, criteriaQuery, false, Relationship.class, limit, offset);
    }

    @Override
    public int findLeftPlaceByLeftItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery.where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item));
        List<Relationship> list = list(context, criteriaQuery, false, Relationship.class, -1, -1);
        list.sort((o1, o2) -> o2.getLeftPlace() - o1.getLeftPlace());
        if (!list.isEmpty()) {
            return list.get(0).getLeftPlace();
        } else {
            return 1;
        }
    }

    @Override
    public int findRightPlaceByRightItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery.where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item));
        List<Relationship> list = list(context, criteriaQuery, false, Relationship.class, -1, -1);
        list.sort((o1, o2) -> o2.getLeftPlace() - o1.getLeftPlace());
        if (!list.isEmpty()) {
            return list.get(0).getLeftPlace();
        } else {
            return 1;
        }
    }

    @Override
    public List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType)
        throws SQLException {

        return findByRelationshipType(context, relationshipType, -1, -1);
    }

    @Override
    public List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType,
                                                     Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
            .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType));
        return list(context, criteriaQuery, true, Relationship.class, limit, offset);
    }


}
