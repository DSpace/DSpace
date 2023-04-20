/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.type.StandardBasicTypes;

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
        super();
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived) throws SQLException {
        Query query = createQuery(context, "FROM Item WHERE inArchive=:in_archive ORDER BY id");
        query.setParameter("in_archive", archived);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived, int limit, int offset) throws SQLException {
        Query query = createQuery(context, "FROM Item WHERE inArchive=:in_archive ORDER BY id");
        query.setParameter("in_archive", archived);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return iterate(query);
    }


    @Override
    public Iterator<Item> findAll(Context context, boolean archived, boolean withdrawn) throws SQLException {
        Query query = createQuery(context,
                "FROM Item WHERE inArchive=:in_archive or withdrawn=:withdrawn ORDER BY id");
        query.setParameter("in_archive", archived);
        query.setParameter("withdrawn", withdrawn);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findAllRegularItems(Context context) throws SQLException {
        // NOTE: This query includes archived items, withdrawn items and older versions of items.
        //       It does not include workspace, workflow or template items.
        Query query = createQuery(
            context,
            "SELECT i FROM Item as i " +
            "LEFT JOIN Version as v ON i = v.item " +
            "WHERE i.inArchive=true or i.withdrawn=true or (i.inArchive=false and v.id IS NOT NULL) " +
            "ORDER BY i.id"
        );
        return iterate(query);
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived,
                                  boolean withdrawn, boolean discoverable, Date lastModified)
        throws SQLException {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append("SELECT i FROM Item i");
        queryStr.append(" WHERE (inArchive = :in_archive OR withdrawn = :withdrawn)");
        queryStr.append(" AND discoverable = :discoverable");

        if (lastModified != null) {
            queryStr.append(" AND last_modified > :last_modified");
        }
        queryStr.append(" ORDER BY i.id");

        Query query = createQuery(context, queryStr.toString());
        query.setParameter("in_archive", archived);
        query.setParameter("withdrawn", withdrawn);
        query.setParameter("discoverable", discoverable);
        if (lastModified != null) {
            query.setParameter("last_modified", lastModified, TemporalType.TIMESTAMP);
        }
        return iterate(query);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson) throws SQLException {
        Query query = createQuery(context,
                "FROM Item WHERE inArchive=:in_archive and submitter=:submitter ORDER BY id");
        query.setParameter("in_archive", true);
        query.setParameter("submitter", eperson);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, boolean retrieveAllItems)
        throws SQLException {
        if (!retrieveAllItems) {
            return findBySubmitter(context, eperson);
        }
        Query query = createQuery(context, "FROM Item WHERE submitter=:submitter ORDER BY id");
        query.setParameter("submitter", eperson);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, MetadataField metadataField, int limit)
        throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT item FROM Item as item ");
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
        return iterate(hibernateQuery);
    }

    @Override
    public Iterator<Item> findByMetadataField(Context context, MetadataField metadataField, String value,
                                              boolean inArchive) throws SQLException {
        String hqlQueryString = "SELECT item FROM Item as item join item.metadata metadatavalue " +
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
        return iterate(query);
    }

    enum OP {
        equals {
            public Criterion buildPredicate(String val, String regexClause) {
                return Property.forName("mv.value").eq(val);
            }
        },
        not_equals {
            public Criterion buildPredicate(String val, String regexClause) {
                return OP.equals.buildPredicate(val, regexClause);
            }
        },
        like {
            public Criterion buildPredicate(String val, String regexClause) {
                return Property.forName("mv.value").like(val);
            }
        },
        not_like {
            public Criterion buildPredicate(String val, String regexClause) {
                return OP.like.buildPredicate(val, regexClause);
            }
        },
        contains {
            public Criterion buildPredicate(String val, String regexClause) {
                return Property.forName("mv.value").like("%" + val + "%");
            }
        },
        doesnt_contain {
            public Criterion buildPredicate(String val, String regexClause) {
                return OP.contains.buildPredicate(val, regexClause);
            }
        },
        exists {
            public Criterion buildPredicate(String val, String regexClause) {
                return Property.forName("mv.value").isNotNull();
            }
        },
        doesnt_exist {
            public Criterion buildPredicate(String val, String regexClause) {
                return OP.exists.buildPredicate(val, regexClause);
            }
        },
        matches {
            public Criterion buildPredicate(String val, String regexClause) {
                return Restrictions.sqlRestriction(regexClause, val, StandardBasicTypes.STRING);
            }
        },
        doesnt_match {
            public Criterion buildPredicate(String val, String regexClause) {
                return OP.matches.buildPredicate(val, regexClause);
            }

        };
        public abstract Criterion buildPredicate(String val, String regexClause);
    }

    @Override
    @Deprecated
    public Iterator<Item> findByMetadataQuery(Context context, List<List<MetadataField>> listFieldList,
                                              List<String> query_op, List<String> query_val, List<UUID> collectionUuids,
                                              String regexClause, int offset, int limit) throws SQLException {

        Criteria criteria = getHibernateSession(context).createCriteria(Item.class, "item");
        criteria.setFirstResult(offset);
        criteria.setMaxResults(limit);

        if (!collectionUuids.isEmpty()) {
            DetachedCriteria dcollCriteria = DetachedCriteria.forClass(Collection.class, "coll");
            dcollCriteria.setProjection(Projections.property("coll.id"));
            dcollCriteria.add(Restrictions.eqProperty("coll.id", "item.owningCollection"));
            dcollCriteria.add(Restrictions.in("coll.id", collectionUuids));
            criteria.add(Subqueries.exists(dcollCriteria));
        }

        int index = Math.min(listFieldList.size(), Math.min(query_op.size(), query_val.size()));
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < index; i++) {
            OP op = OP.valueOf(query_op.get(i));
            if (op == null) {
                log.warn("Skipping Invalid Operator: " + query_op.get(i));
                continue;
            }

            if (op == OP.matches || op == OP.doesnt_match) {
                if (regexClause.isEmpty()) {
                    log.warn("Skipping Unsupported Regex Operator: " + query_op.get(i));
                    continue;
                }
            }

            DetachedCriteria subcriteria = DetachedCriteria.forClass(MetadataValue.class, "mv");
            subcriteria.add(Property.forName("mv.dSpaceObject").eqProperty("item.id"));
            subcriteria.setProjection(Projections.property("mv.dSpaceObject"));

            if (!listFieldList.get(i).isEmpty()) {
                subcriteria.add(Restrictions.in("metadataField", listFieldList.get(i)));
            }

            subcriteria.add(op.buildPredicate(query_val.get(i), regexClause));

            if (op == OP.exists || op == OP.equals || op == OP.like || op == OP.contains || op == OP.matches) {
                criteria.add(Subqueries.exists(subcriteria));
            } else {
                criteria.add(Subqueries.notExists(subcriteria));
            }
        }
        criteria.addOrder(Order.asc("item.id"));

        log.debug(String.format("Running custom query with %d filters", index));

        return ((List<Item>) criteria.list()).iterator();

    }

    @Override
    public Iterator<Item> findByAuthorityValue(Context context, MetadataField metadataField, String authority,
                                               boolean inArchive) throws SQLException {
        Query query = createQuery(context,
                  "SELECT item FROM Item as item join item.metadata metadatavalue " +
                  "WHERE item.inArchive=:in_archive AND metadatavalue.metadataField = :metadata_field AND " +
                      "metadatavalue.authority = :authority ORDER BY item.id");
        query.setParameter("in_archive", inArchive);
        query.setParameter("metadata_field", metadataField);
        query.setParameter("authority", authority);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findArchivedByCollection(Context context, Collection collection, Integer limit,
                                                   Integer offset) throws SQLException {
        Query query = createQuery(context,
              "select i from Item i join i.collections c " +
              "WHERE :collection IN c AND i.inArchive=:in_archive ORDER BY i.id");
        query.setParameter("collection", collection);
        query.setParameter("in_archive", true);
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return iterate(query);
    }

    @Override
    public Iterator<Item> findArchivedByCollectionExcludingOwning(Context context, Collection collection, Integer limit,
                                                                  Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Item.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.notEqual(itemRoot.get(Item_.owningCollection), collection),
                criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections)),
                criteriaBuilder.isTrue(itemRoot.get(Item_.inArchive))));
        criteriaQuery.orderBy(criteriaBuilder.asc(itemRoot.get(Item_.id)));
        criteriaQuery.groupBy(itemRoot.get(Item_.id));
        return list(context, criteriaQuery, false, Item.class, limit, offset).iterator();
    }

    @Override
    public int countArchivedByCollectionExcludingOwning(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Item.class);
        Root<Item> itemRoot = criteriaQuery.from(Item.class);
        criteriaQuery.select(itemRoot);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.notEqual(itemRoot.get(Item_.owningCollection), collection),
                criteriaBuilder.isMember(collection, itemRoot.get(Item_.collections)),
                criteriaBuilder.isTrue(itemRoot.get(Item_.inArchive))));
        return count(context, criteriaQuery, criteriaBuilder, itemRoot);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException {
        Query query = createQuery(context,
                "select i from Item i join i.collections c WHERE :collection IN c ORDER BY i.id");
        query.setParameter("collection", collection);

        return iterate(query);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection, Integer limit, Integer offset)
        throws SQLException {
        Query query = createQuery(context,
                "select i from Item i join i.collections c WHERE :collection IN c ORDER BY i.id");
        query.setParameter("collection", collection);

        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }

        return iterate(query);
    }

    @Override
    public int countItems(Context context, Collection collection, boolean includeArchived, boolean includeWithdrawn)
        throws SQLException {
        Query query = createQuery(context,
              "select count(i) from Item i join i.collections c " +
              "WHERE :collection IN c AND i.inArchive=:in_archive AND i.withdrawn=:withdrawn");
        query.setParameter("collection", collection);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);

        return count(query);
    }

    @Override
    public int countItems(Context context, List<Collection> collections, boolean includeArchived,
                          boolean includeWithdrawn) throws SQLException {
        if (collections.size() == 0) {
            return 0;
        }
        Query query = createQuery(context, "select count(distinct i) from Item i " +
            "join i.collections collection " +
            "WHERE collection IN (:collections) AND i.inArchive=:in_archive AND i.withdrawn=:withdrawn");
        query.setParameter("collections", collections);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);

        return count(query);
    }

    @Override
    public Iterator<Item> findByLastModifiedSince(Context context, Date since)
        throws SQLException {
        Query query = createQuery(context,
                "SELECT i FROM Item i WHERE last_modified > :last_modified ORDER BY id");
        query.setParameter("last_modified", since, TemporalType.TIMESTAMP);
        return iterate(query);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Item"));
    }

    @Override
    public int countItems(Context context, boolean includeArchived, boolean includeWithdrawn) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*) FROM Item i " +
                "WHERE i.inArchive=:in_archive AND i.withdrawn=:withdrawn");
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);
        return count(query);
    }

    @Override
    public int countItems(Context context, EPerson submitter, boolean includeArchived, boolean includeWithdrawn)
        throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*) FROM Item i join i.submitter submitter " +
                "WHERE i.inArchive=:in_archive AND i.withdrawn=:withdrawn AND submitter = :submitter");
        query.setParameter("submitter", submitter);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);
        return count(query);

    }
}
