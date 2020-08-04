/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.Relationship_;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.RelationshipTypeService;
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
    public int countByItem(Context context, Item item)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
                .where(criteriaBuilder.or(criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item),
                        criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item)));
        return count(context, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int findNextLeftPlaceByLeftItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery.where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item));
        List<Relationship> list = list(context, criteriaQuery, false, Relationship.class, -1, -1);
        list.sort((o1, o2) -> o2.getLeftPlace() - o1.getLeftPlace());
        if (!list.isEmpty()) {
            return list.get(0).getLeftPlace() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public int findNextRightPlaceByRightItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery.where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item));
        List<Relationship> list = list(context, criteriaQuery, false, Relationship.class, -1, -1);
        list.sort((o1, o2) -> o2.getRightPlace() - o1.getRightPlace());
        if (!list.isEmpty()) {
            return list.get(0).getRightPlace() + 1;
        } else {
            return 0;
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

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, Integer limit,
                                                            Integer offset)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
                .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType),
                        relationshipType), criteriaBuilder.or
                        (criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item),
                         criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item)));
        return list(context, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, boolean isLeft,
                                                            Integer limit, Integer offset)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        if (isLeft) {
            criteriaQuery
                    .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType),
                            relationshipType),
                           criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item));
            criteriaQuery.orderBy(criteriaBuilder.asc(relationshipRoot.get(Relationship_.leftPlace)));
        } else {
            criteriaQuery
                    .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType),
                            relationshipType),
                            criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item));
            criteriaQuery.orderBy(criteriaBuilder.asc(relationshipRoot.get(Relationship_.rightPlace)));
        }
        return list(context, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public List<Relationship> findByTypeName(Context context, String typeName)
            throws SQLException {
        return this.findByTypeName(context, typeName, -1, -1);
    }

    @Override
    public List<Relationship> findByTypeName(Context context, String typeName, Integer limit, Integer offset)
            throws SQLException {
        RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                .getRelationshipTypeService();
        List<RelationshipType> relTypes = relationshipTypeService.findByLeftwardOrRightwardTypeName(context, typeName);
        List<Integer> ids = new ArrayList<>();
        for ( RelationshipType relationshipType : relTypes) {
            ids.add(relationshipType.getID());
        }
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.where(relationshipRoot.get(Relationship_.relationshipType).in(ids));
        return list(context, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public int countByRelationshipType(Context context, RelationshipType relationshipType) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
                .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType));
        return count(context, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        return count(context, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int countByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
                .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType),
                        relationshipType), criteriaBuilder.or
                        (criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item),
                        criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item)));
        return count(context, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int countByTypeName(Context context, String typeName)
            throws SQLException {
        RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                .getRelationshipTypeService();
        List<RelationshipType> relTypes = relationshipTypeService.findByLeftwardOrRightwardTypeName(context, typeName);
        List<Integer> ids = new ArrayList<>();
        for ( RelationshipType relationshipType : relTypes) {
            ids.add(relationshipType.getID());
        }
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.where(relationshipRoot.get(Relationship_.relationshipType).in(ids));
        return count(context, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

}
