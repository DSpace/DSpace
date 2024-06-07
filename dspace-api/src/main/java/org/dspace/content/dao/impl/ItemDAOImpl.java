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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject_;
import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.MetadataValue_;
import org.dspace.content.dao.ItemDAO;
import org.dspace.contentreport.QueryOperator;
import org.dspace.contentreport.QueryPredicate;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.core.UUIDIterator;
import org.dspace.eperson.EPerson;
import org.dspace.util.JpaCriteriaBuilderKit;

/**
 * Hibernate implementation of the Database Access Object interface class for the Item object.
 * This class is responsible for all database calls for the Item object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ItemDAOImpl extends AbstractHibernateDSODAO<Item> implements ItemDAO {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemDAOImpl.class);

    protected ItemDAOImpl() {
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived) throws SQLException {
        Query query = createQuery(context, "SELECT i.id FROM Item i WHERE inArchive=:in_archive ORDER BY id");
        query.setParameter("in_archive", archived);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived, int limit, int offset) throws SQLException {
        Query query = createQuery(context, "SELECT i.id FROM Item i WHERE inArchive=:in_archive ORDER BY id");
        query.setParameter("in_archive", archived);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }


    @Override
    public Iterator<Item> findAll(Context context, boolean archived, boolean withdrawn) throws SQLException {
        Query query = createQuery(context,
                "SELECT i.id FROM Item i WHERE inArchive=:in_archive or withdrawn=:withdrawn ORDER BY id");
        query.setParameter("in_archive", archived);
        query.setParameter("withdrawn", withdrawn);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findAllRegularItems(Context context) throws SQLException {
        // NOTE: This query includes archived items, withdrawn items and older versions of items.
        //       It does not include workspace, workflow or template items.
        Query query = createQuery(
            context,
            "SELECT i.id FROM Item as i " +
            "LEFT JOIN Version as v ON i = v.item " +
            "WHERE i.inArchive=true or i.withdrawn=true or (i.inArchive=false and v.id IS NOT NULL) " +
            "ORDER BY i.id"
        );
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived,
                                  boolean withdrawn, boolean discoverable, Date lastModified)
        throws SQLException {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append("SELECT i.id FROM Item i");
        queryStr.append(" WHERE (inArchive = :in_archive OR withdrawn = :withdrawn)");
        queryStr.append(" AND discoverable = :discoverable");

        if (lastModified != null) {
            queryStr.append(" AND lastModified > :last_modified");
        }
        queryStr.append(" ORDER BY i.id");

        Query query = createQuery(context, queryStr.toString());
        query.setParameter("in_archive", archived);
        query.setParameter("withdrawn", withdrawn);
        query.setParameter("discoverable", discoverable);
        if (lastModified != null) {
            query.setParameter("last_modified", lastModified, TemporalType.TIMESTAMP);
        }
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson) throws SQLException {
        Query query = createQuery(context,
                "SELECT i.id FROM Item i WHERE inArchive=:in_archive and submitter=:submitter ORDER BY id");
        query.setParameter("in_archive", true);
        query.setParameter("submitter", eperson);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, boolean retrieveAllItems)
        throws SQLException {
        if (!retrieveAllItems) {
            return findBySubmitter(context, eperson);
        }
        Query query = createQuery(context, "SELECT i.id FROM Item i WHERE submitter=:submitter ORDER BY id");
        query.setParameter("submitter", eperson);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, MetadataField metadataField, int limit)
        throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT item.id FROM Item as item ");
        addMetadataLeftJoin(query, Item.class.getSimpleName().toLowerCase(), Collections.singletonList(metadataField));
        query.append(" WHERE item.inArchive = :in_archive");
        query.append(" AND item.submitter =:submitter");
        //submissions should sort in reverse by date by default
        addMetadataSortQuery(query, Collections.singletonList(metadataField), null, Collections.singletonList("desc"));

        Query hibernateQuery = createQuery(context, query.toString());
        hibernateQuery.setParameter(metadataField.toString(), metadataField.getID());
        hibernateQuery.setParameter("in_archive", true);
        hibernateQuery.setParameter("submitter", eperson);
        hibernateQuery.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = hibernateQuery.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findByMetadataField(Context context, MetadataField metadataField, String value,
                                              boolean inArchive) throws SQLException {
        String hqlQueryString = "SELECT item.id FROM Item as item join item.metadata metadatavalue " +
                "WHERE item.inArchive=:in_archive AND metadatavalue.metadataField = :metadata_field";
        if (value != null) {
            hqlQueryString += " AND STR(metadatavalue.value) = :text_value";
        }
        Query query = createQuery(context, hqlQueryString + " ORDER BY item.id");

        query.setParameter("in_archive", inArchive);
        query.setParameter("metadata_field", metadataField);
        if (value != null) {
            query.setParameter("text_value", value);
        }
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public List<Item> findByMetadataQuery(Context context, List<QueryPredicate> queryPredicates,
            List<UUID> collectionUuids, String regexClause, long offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Item> criteriaQuery = getCriteriaQuery(criteriaBuilder, Item.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot);
        List<Predicate> predicates = toPredicates(criteriaBuilder, criteriaQuery, itemRoot,
                queryPredicates, collectionUuids, regexClause);
        criteriaQuery.where(criteriaBuilder.and(predicates.stream().toArray(Predicate[]::new)));
        criteriaQuery.orderBy(criteriaBuilder.asc(itemRoot.get(DSpaceObject_.id)));
        try {
            return list(context, criteriaQuery, false, Item.class, limit, (int) offset);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public long countForMetadataQuery(Context context, List<QueryPredicate> queryPredicates,
            List<UUID> collectionUuids, String regexClause) throws SQLException {
        // Build the query infrastructure
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        // Select
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        // Apply the selected predicates
        List<Predicate> predicates = toPredicates(criteriaBuilder, criteriaQuery, itemRoot,
                queryPredicates, collectionUuids, regexClause);
        criteriaQuery.where(criteriaBuilder.and(predicates.stream().toArray(Predicate[]::new)));
        // Execute the query
        return countLong(context, criteriaQuery, criteriaBuilder, itemRoot);
    }

    private <T> List<Predicate> toPredicates(CriteriaBuilder criteriaBuilder, CriteriaQuery<T> query,
            Root<Item> root, List<QueryPredicate> queryPredicates,
            List<UUID> collectionUuids, String regexClause) {
        List<Predicate> predicates = new ArrayList<>();

        if (!collectionUuids.isEmpty()) {
            Subquery<Collection> scollQuery = query.subquery(Collection.class);
            Root<Collection> collRoot = scollQuery.from(Collection.class);
            In<UUID> inColls = criteriaBuilder.in(collRoot.get(DSpaceObject_.ID));
            collectionUuids.forEach(inColls::value);
            scollQuery.select(collRoot.get(DSpaceObject_.ID))
                    .where(criteriaBuilder.and(
                            criteriaBuilder.equal(collRoot.get(DSpaceObject_.ID),
                                    root.get(Item_.OWNING_COLLECTION).get(DSpaceObject_.ID)),
                            collRoot.get(DSpaceObject_.ID).in(collectionUuids)
                    ));
            predicates.add(criteriaBuilder.exists(scollQuery));
        }

        for (int i = 0; i < queryPredicates.size(); i++) {
            QueryPredicate predicate = queryPredicates.get(i);
            QueryOperator op = predicate.getOperator();
            if (op == null) {
                log.warn("Skipping Invalid Operator: null");
                continue;
            }

            if (op.getUsesRegex()) {
                if (regexClause.isEmpty()) {
                    log.warn("Skipping Unsupported Regex Operator: " + op);
                    continue;
                }
            }

            List<Predicate> mvPredicates = new ArrayList<>();
            Subquery<MetadataValue> mvQuery = query.subquery(MetadataValue.class);
            Root<MetadataValue> mvRoot = mvQuery.from(MetadataValue.class);
            mvPredicates.add(criteriaBuilder.equal(
                    mvRoot.get(MetadataValue_.D_SPACE_OBJECT), root));

            if (!predicate.getFields().isEmpty()) {
                In<MetadataField> inFields = criteriaBuilder.in(mvRoot.get(MetadataValue_.METADATA_FIELD));
                predicate.getFields().forEach(inFields::value);
                mvPredicates.add(inFields);
            }

            JpaCriteriaBuilderKit<MetadataValue> jpaKit = new JpaCriteriaBuilderKit<>(criteriaBuilder, mvQuery, mvRoot);
            mvPredicates.add(op.buildJpaPredicate(predicate.getValue(), regexClause, jpaKit));

            mvQuery.select(mvRoot.get(MetadataValue_.D_SPACE_OBJECT))
                    .where(mvPredicates.stream().toArray(Predicate[]::new));

            if (op.getNegate()) {
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(mvQuery)));
            } else {
                predicates.add(criteriaBuilder.exists(mvQuery));
            }
        }

        log.debug(String.format("Running custom query with %d filters", queryPredicates.size()));
        return predicates;
    }

    @Override
    public Iterator<Item> findByAuthorityValue(Context context, MetadataField metadataField, String authority,
                                               boolean inArchive) throws SQLException {
        Query query = createQuery(context,
                  "SELECT item.id FROM Item as item join item.metadata metadatavalue " +
                  "WHERE item.inArchive=:in_archive AND metadatavalue.metadataField = :metadata_field AND " +
                      "metadatavalue.authority = :authority ORDER BY item.id");
        query.setParameter("in_archive", inArchive);
        query.setParameter("metadata_field", metadataField);
        query.setParameter("authority", authority);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findArchivedByCollection(Context context, Collection collection, Integer limit,
                                                   Integer offset) throws SQLException {
        // Select UUID of all items which have this "collection" in their list of collections and are in_archive
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UUID> criteriaQuery = criteriaBuilder.createQuery(UUID.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot.get(Item_.id));
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.isTrue((itemRoot.get(Item_.inArchive))),
            criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections))));
        criteriaQuery.orderBy(criteriaBuilder.asc(itemRoot.get((Item_.id))));

        // Transform into a query object to execute
        Query query = createQuery(context, criteriaQuery);
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findArchivedByCollectionExcludingOwning(Context context, Collection collection, Integer limit,
                                                                  Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Item> criteriaQuery = getCriteriaQuery(criteriaBuilder, Item.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.notEqual(itemRoot.get(Item_.owningCollection), collection),
                criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections)),
                criteriaBuilder.isTrue(itemRoot.get(Item_.inArchive))));
        criteriaQuery.orderBy(criteriaBuilder.asc(itemRoot.get(DSpaceObject_.id)));
        return list(context, criteriaQuery, false, Item.class, limit, offset).iterator();
    }

    @Override
    public int countArchivedByCollectionExcludingOwning(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(criteriaBuilder.count(itemRoot));
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.notEqual(itemRoot.get(Item_.owningCollection), collection),
                criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections)),
                criteriaBuilder.isTrue(itemRoot.get(Item_.inArchive))));
        return count(context, criteriaQuery, criteriaBuilder, itemRoot);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException {
        // Select UUID of all items which have this "collection" in their list of collections
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UUID> criteriaQuery = criteriaBuilder.createQuery(UUID.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot.get(Item_.id));
        criteriaQuery.where(criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections)));
        criteriaQuery.orderBy(criteriaBuilder.asc(itemRoot.get((Item_.id))));

        // Transform into a query object to execute
        Query query = createQuery(context, criteriaQuery);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection, Integer limit, Integer offset)
        throws SQLException {
        // Build Query to select UUID of all items which have this "collection" in their list of collections.
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UUID> criteriaQuery = criteriaBuilder.createQuery(UUID.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot.get(Item_.id));
        criteriaQuery.where(criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections)));
        criteriaQuery.orderBy(criteriaBuilder.asc(itemRoot.get((Item_.id))));

        // Transform into a query object to execute
        Query query = createQuery(context, criteriaQuery);
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public int countItems(Context context, Collection collection, boolean includeArchived, boolean includeWithdrawn,
                          boolean discoverable)
        throws SQLException {
        // Build query to select all Items have this "collection" in their list of collections
        // AND also have the inArchive or isWithdrawn set as specified
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(criteriaBuilder.count(itemRoot));
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(itemRoot.get(Item_.inArchive), includeArchived),
            criteriaBuilder.equal(itemRoot.get(Item_.withdrawn), includeWithdrawn),
            criteriaBuilder.equal(itemRoot.get(Item_.discoverable), discoverable),
            criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections))));
        // Execute and return count
        return count(context, criteriaQuery, criteriaBuilder, itemRoot);
    }

    @Override
    public int countItems(Context context, List<Collection> collections, boolean includeArchived,
                          boolean includeWithdrawn, boolean discoverable) throws SQLException {
        if (collections.size() == 0) {
            return 0;
        }
        Query query = createQuery(context, "select count(distinct i) from Item i " +
            "join i.collections collection " +
            "WHERE collection IN (:collections) AND i.inArchive=:in_archive AND i.withdrawn=:withdrawn AND " +
                "discoverable=:discoverable");
        query.setParameter("collections", collections);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);
        query.setParameter("discoverable", discoverable);

        return count(query);
    }

    @Override
    public Iterator<Item> findByLastModifiedSince(Context context, Date since)
        throws SQLException {
        Query query = createQuery(context,
                "SELECT i.id FROM Item i WHERE last_modified > :last_modified ORDER BY id");
        query.setParameter("last_modified", since, TemporalType.TIMESTAMP);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Item>(context, uuids, Item.class, this);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Item"));
    }

    @Override
    public int countItems(Context context, boolean includeArchived, boolean includeWithdrawn,
                          boolean discoverable) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*) FROM Item i " +
                "WHERE i.inArchive=:in_archive AND i.withdrawn=:withdrawn AND discoverable=:discoverable");
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);
        query.setParameter("discoverable", discoverable);
        return count(query);
    }

    @Override
    public int countItems(Context context, EPerson submitter, boolean includeArchived, boolean includeWithdrawn,
                          boolean discoverable)
        throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*) FROM Item i join i.submitter submitter " +
                "WHERE i.inArchive=:in_archive AND i.withdrawn=:withdrawn AND submitter = :submitter AND " +
                        "discoverable=:discoverable");
        query.setParameter("submitter", submitter);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);
        query.setParameter("discoverable", discoverable);
        return count(query);

    }
}
