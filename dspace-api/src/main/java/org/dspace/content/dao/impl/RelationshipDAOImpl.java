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
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.RelationshipType_;
import org.dspace.content.Relationship_;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.content.dao.pojo.ItemUuidAndRelationshipId;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.Session;

public class RelationshipDAOImpl extends AbstractHibernateDAO<Relationship> implements RelationshipDAO {
    @Inject
    RelationshipTypeDAO rtdao;

    @Override
    public List<Relationship> findByItem(
        Session session, Item item, boolean excludeTilted, boolean excludeNonLatest
    ) throws SQLException {
        return findByItem(session, item, -1, -1, excludeTilted, excludeNonLatest);
    }

    @Override
    public List<Relationship> findByItem(
        Session session, Item item, Integer limit, Integer offset, boolean excludeTilted, boolean excludeNonLatest
    ) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery<Relationship> criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);

        criteriaQuery.where(
            criteriaBuilder.or(
                getLeftItemPredicate(criteriaBuilder, relationshipRoot, item, excludeTilted, excludeNonLatest),
                getRightItemPredicate(criteriaBuilder, relationshipRoot, item, excludeTilted, excludeNonLatest)
            )
        );

        return list(session, criteriaQuery, false, Relationship.class, limit, offset);
    }

    /**
     * Get the predicate for a criteria query that selects relationships by their left item.
     * @param criteriaBuilder   the criteria builder.
     * @param relationshipRoot  the relationship root.
     * @param item              the item that is being searched for.
     * @param excludeTilted     if true, exclude tilted relationships.
     * @param excludeNonLatest  if true, exclude relationships for which the opposite item is not the latest version
     *                          that is relevant.
     * @return a predicate that satisfies the given restrictions.
     */
    protected Predicate getLeftItemPredicate(
        CriteriaBuilder criteriaBuilder, Root<Relationship> relationshipRoot, Item item,
        boolean excludeTilted, boolean excludeNonLatest
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // match relationships based on the left item
        predicates.add(
            criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), item)
        );

        if (excludeTilted) {
            // if this item is the left item,
            // return relationships for types which are NOT tilted right (tilted is either left nor null)
            predicates.add(
                criteriaBuilder.or(
                    criteriaBuilder.isNull(
                        relationshipRoot.get(Relationship_.relationshipType).get(RelationshipType_.tilted)
                    ),
                    criteriaBuilder.notEqual(
                        relationshipRoot.get(Relationship_.relationshipType).get(RelationshipType_.tilted),
                        RelationshipType.Tilted.RIGHT
                    )
                )
            );
        }

        if (excludeNonLatest) {
            // if this item is the left item,
            // return relationships for which the right item is the "latest" version that is relevant.
            predicates.add(
                criteriaBuilder.notEqual(
                    relationshipRoot.get(Relationship_.LATEST_VERSION_STATUS),
                    Relationship.LatestVersionStatus.LEFT_ONLY
                )
            );
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
    }

    /**
     * Get the predicate for a criteria query that selects relationships by their right item.
     * @param criteriaBuilder   the criteria builder.
     * @param relationshipRoot  the relationship root.
     * @param item              the item that is being searched for.
     * @param excludeTilted     if true, exclude tilted relationships.
     * @param excludeNonLatest  if true, exclude relationships for which the opposite item is not the latest version
     *                          that is relevant.
     * @return a predicate that satisfies the given restrictions.
     */
    protected Predicate getRightItemPredicate(
        CriteriaBuilder criteriaBuilder, Root<Relationship> relationshipRoot, Item item,
        boolean excludeTilted, boolean excludeNonLatest
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // match relationships based on the right item
        predicates.add(
            criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), item)
        );

        if (excludeTilted) {
            // if this item is the right item,
            // return relationships for types which are NOT tilted left (tilted is either right nor null)
            predicates.add(
                criteriaBuilder.or(
                    criteriaBuilder.isNull(
                        relationshipRoot.get(Relationship_.relationshipType).get(RelationshipType_.tilted)
                    ),
                    criteriaBuilder.notEqual(
                        relationshipRoot.get(Relationship_.relationshipType).get(RelationshipType_.tilted),
                        RelationshipType.Tilted.LEFT
                    )
                )
            );
        }

        if (excludeNonLatest) {
            // if this item is the right item,
            // return relationships for which the left item is the "latest" version that is relevant.
            predicates.add(
                criteriaBuilder.notEqual(
                    relationshipRoot.get(Relationship_.LATEST_VERSION_STATUS),
                    Relationship.LatestVersionStatus.RIGHT_ONLY
                )
            );
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
    }

    @Override
    public int countByItem(
        Session session, Item item, boolean excludeTilted, boolean excludeNonLatest
    ) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);

        criteriaQuery.where(
            criteriaBuilder.or(
                getLeftItemPredicate(criteriaBuilder, relationshipRoot, item, excludeTilted, excludeNonLatest),
                getRightItemPredicate(criteriaBuilder, relationshipRoot, item, excludeTilted, excludeNonLatest)
            )
        );

        return count(session, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public List<Relationship> findByRelationshipType(Session session, RelationshipType relationshipType)
        throws SQLException {

        return findByRelationshipType(session, relationshipType, -1, -1);
    }

    @Override
    public List<Relationship> findByRelationshipType(Session session, RelationshipType relationshipType,
                                                     Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
            .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType));
        return list(session, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(
        Session session, Item item, RelationshipType relationshipType, Integer limit, Integer offset,
        boolean excludeNonLatest
    ) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);

        criteriaQuery.where(
            criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType),
            criteriaBuilder.or(
                getLeftItemPredicate(criteriaBuilder, relationshipRoot, item, false, excludeNonLatest),
                getRightItemPredicate(criteriaBuilder, relationshipRoot, item, false, excludeNonLatest)
            )
        );

        return list(session, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(
        Session session, Item item, RelationshipType relationshipType, boolean isLeft, Integer limit, Integer offset,
        boolean excludeNonLatest
    ) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);

        if (isLeft) {
            criteriaQuery.where(
                criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType),
                getLeftItemPredicate(criteriaBuilder, relationshipRoot, item, false, excludeNonLatest)
            );
            criteriaQuery.orderBy(criteriaBuilder.asc(relationshipRoot.get(Relationship_.leftPlace)));
        } else {
            criteriaQuery.where(
                criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType),
                getRightItemPredicate(criteriaBuilder, relationshipRoot, item, false, excludeNonLatest)
            );
            criteriaQuery.orderBy(criteriaBuilder.asc(relationshipRoot.get(Relationship_.rightPlace)));
        }

        return list(session, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public List<ItemUuidAndRelationshipId> findByLatestItemAndRelationshipType(
        Session session, Item latestItem, RelationshipType relationshipType, boolean isLeft
    ) throws SQLException {
        final String relationshipIdAlias = "relationshipId";
        final String itemUuidAlias = "itemUuid";

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);

        ArrayList<Predicate> predicates = new ArrayList<>();

        // all relationships should have the specified relationship type
        predicates.add(
            criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType)
        );

        if (isLeft) {
            // match relationships based on the left item
            predicates.add(
                criteriaBuilder.equal(relationshipRoot.get(Relationship_.leftItem), latestItem)
            );

            // the left item has to have "latest status" => accept BOTH and LEFT_ONLY
            predicates.add(
                criteriaBuilder.notEqual(
                    relationshipRoot.get(Relationship_.LATEST_VERSION_STATUS),
                    Relationship.LatestVersionStatus.RIGHT_ONLY
                )
            );

            // return the UUIDs of the right item
            criteriaQuery.multiselect(
                relationshipRoot.get(Relationship_.id).alias(relationshipIdAlias),
                relationshipRoot.get(Relationship_.rightItem).get(Item_.id).alias(itemUuidAlias)
            );
        } else {
            // match relationships based on the right item
            predicates.add(
                criteriaBuilder.equal(relationshipRoot.get(Relationship_.rightItem), latestItem)
            );

            // the right item has to have "latest status" => accept BOTH and RIGHT_ONLY
            predicates.add(
                criteriaBuilder.notEqual(
                    relationshipRoot.get(Relationship_.LATEST_VERSION_STATUS),
                    Relationship.LatestVersionStatus.LEFT_ONLY
                )
            );

            // return the UUIDs of the left item
            criteriaQuery.multiselect(
                relationshipRoot.get(Relationship_.id).alias(relationshipIdAlias),
                relationshipRoot.get(Relationship_.leftItem).get(Item_.id).alias(itemUuidAlias)
            );
        }

        // all predicates are chained with the AND operator
        criteriaQuery.where(predicates.toArray(new Predicate[]{}));

        // deduplicate result
        criteriaQuery.distinct(true);

        // execute query
        Query query = session.createQuery(criteriaQuery);
        query.setHint("org.hibernate.cacheable", true);
        List<?> resultList = query.getResultList();

        // convert types
        return resultList.stream()
            .map(Tuple.class::cast)
            .map(t -> new ItemUuidAndRelationshipId(
                (UUID) t.get(itemUuidAlias),
                (Integer) t.get(relationshipIdAlias)
            ))
            .collect(Collectors.toList());
    }

    @Override
    public List<Relationship> findByTypeName(Session session, String typeName)
            throws SQLException {
        return this.findByTypeName(session, typeName, -1, -1);
    }

    @Override
    public List<Relationship> findByTypeName(Session session, String typeName, Integer limit, Integer offset)
            throws SQLException {
        List<RelationshipType> relTypes = rtdao.findByLeftwardOrRightwardTypeName(session, typeName);
        List<Integer> ids = new ArrayList<>();
        for ( RelationshipType relationshipType : relTypes) {
            ids.add(relationshipType.getID());
        }
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.where(relationshipRoot.get(Relationship_.relationshipType).in(ids));
        return list(session, criteriaQuery, true, Relationship.class, limit, offset);
    }

    @Override
    public int countByRelationshipType(Session session, RelationshipType relationshipType) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        criteriaQuery
                .where(criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType));
        return count(session, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int countRows(Session session) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);
        return count(session, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int countByItemAndRelationshipType(
        Session session, Item item, RelationshipType relationshipType, boolean isLeft, boolean excludeNonLatest
    ) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.select(relationshipRoot);

        if (isLeft) {
            criteriaQuery.where(
                criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType),
                getLeftItemPredicate(criteriaBuilder, relationshipRoot, item, false, excludeNonLatest)
            );
        } else {
            criteriaQuery.where(
                criteriaBuilder.equal(relationshipRoot.get(Relationship_.relationshipType), relationshipType),
                getRightItemPredicate(criteriaBuilder, relationshipRoot, item, false, excludeNonLatest)
            );
        }

        return count(session, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public int countByTypeName(Session session, String typeName)
            throws SQLException {
        List<RelationshipType> relTypes = rtdao.findByLeftwardOrRightwardTypeName(session, typeName);
        List<Integer> ids = new ArrayList<>();
        for ( RelationshipType relationshipType : relTypes) {
            ids.add(relationshipType.getID());
        }
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Relationship.class);
        Root<Relationship> relationshipRoot = criteriaQuery.from(Relationship.class);
        criteriaQuery.where(relationshipRoot.get(Relationship_.relationshipType).in(ids));
        return count(session, criteriaQuery, criteriaBuilder, relationshipRoot);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipTypeAndList(Session session, UUID focusUUID,
            RelationshipType relationshipType, List<UUID> items, boolean isLeft,
            int offset, int limit) throws SQLException {
        String side = isLeft ? "left_id" : "right_id";
        String otherSide = !isLeft ? "left_id" : "right_id";
        Query query = createQuery(session, "FROM " + Relationship.class.getSimpleName() +
                                          " WHERE type_id = (:typeId) " +
                                           "AND " + side + " = (:focusUUID) " +
                                           "AND " + otherSide + " in (:list) " +
                                           "ORDER BY id");
        query.setParameter("typeId", relationshipType.getID());
        query.setParameter("focusUUID", focusUUID);
        query.setParameter("list", items);
        return list(query, limit, offset);
    }

    @Override
    public int countByItemAndRelationshipTypeAndList(Session session, UUID focusUUID, RelationshipType relationshipType,
            List<UUID> items, boolean isLeft) throws SQLException {
        String side = isLeft ? "left_id" : "right_id";
        String otherSide = !isLeft ? "left_id" : "right_id";
        Query query = createQuery(session, "SELECT count(*) " +
                                           "FROM " + Relationship.class.getSimpleName() +
                                          " WHERE type_id = (:typeId) " +
                                           "AND " + side + " = (:focusUUID) " +
                                           "AND " + otherSide + " in (:list)");
        query.setParameter("typeId", relationshipType.getID());
        query.setParameter("focusUUID", focusUUID);
        query.setParameter("list", items);
        return count(query);
    }

}
