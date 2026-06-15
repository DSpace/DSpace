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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataField_;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the MetadataValue object.
 * This class is responsible for all database calls for the MetadataValue object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataValueDAOImpl extends AbstractHibernateDAO<MetadataValue> implements MetadataValueDAO {

    private static final int MAX_IN_CLAUSE_SIZE = 1000;

    protected MetadataValueDAOImpl() {
        super();
    }


    @Override
    public List<MetadataValue> findByField(Context context, MetadataField metadataField) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, MetadataValue.class);
        Root<MetadataValue> metadataValueRoot = criteriaQuery.from(MetadataValue.class);
        Join<MetadataValue, MetadataField> join = metadataValueRoot.join("metadataField");
        criteriaQuery.select(metadataValueRoot);
        criteriaQuery.where(criteriaBuilder.equal(join.get(MetadataField_.id), metadataField.getID()));

        return list(context, criteriaQuery, false, MetadataValue.class, -1, -1);
    }

    @Override
    public Iterator<MetadataValue> findItemValuesByFieldAndValue(Context context,
                                                                 MetadataField metadataField, String value)
            throws SQLException {
        String queryString = "SELECT m from MetadataValue m " +
                "join Item i on m.dSpaceObject = i where m.metadataField.id = :metadata_field_id " +
                "and m.value = :text_value";
        Query query = createQuery(context, queryString);
        query.setParameter("metadata_field_id", metadataField.getID());
        query.setParameter("text_value", value);
        return iterate(query);
    }

    @Override
    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException {
        String queryString = "SELECT m FROM MetadataValue m JOIN m.metadataField f " +
            "WHERE m.value like concat('%', concat(:searchString,'%')) ORDER BY m.id ASC";

        Query query = createQuery(context, queryString);
        query.setParameter("searchString", value);

        return iterate(query);
    }

    @Override
    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException {
        String queryString = "delete from MetadataValue where metadataField= :metadataField";
        Query query = createQuery(context, queryString);
        query.setParameter("metadataField", metadataField);
        query.executeUpdate();
    }

    @Override
    public MetadataValue getMinimum(Context context, int metadataFieldId)
        throws SQLException {
        String queryString = "SELECT m FROM MetadataValue m JOIN FETCH m.metadataField WHERE m.metadataField.id = " +
            ":metadata_field_id ORDER BY value";
        Query query = createQuery(context, queryString);
        query.setParameter("metadata_field_id", metadataFieldId);
        query.setMaxResults(1);
        return (MetadataValue) query.getSingleResult();
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM MetadataValue"));
    }

    @Override
    public long countByField(Context context, MetadataField metadataField) throws SQLException {
        Query query = createQuery(context,
            "SELECT count(mv) FROM MetadataValue mv WHERE mv.metadataField = :field");
        query.setParameter("field", metadataField);
        return (Long) query.getSingleResult();
    }

    @Override
    public List<UUID> findObjectIdsByField(Context context, MetadataField metadataField, UUID afterUuid, int limit)
        throws SQLException {
        StringBuilder hql = new StringBuilder(
            "SELECT DISTINCT mv.dSpaceObject.id FROM MetadataValue mv WHERE mv.metadataField = :field");
        if (afterUuid != null) {
            hql.append(" AND mv.dSpaceObject.id > :after");
        }
        hql.append(" ORDER BY mv.dSpaceObject.id");

        Query query = createQuery(context, hql.toString());
        query.setParameter("field", metadataField);
        if (afterUuid != null) {
            query.setParameter("after", afterUuid);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }

        @SuppressWarnings("unchecked")
        List<UUID> result = query.getResultList();
        return result;
    }

    @Override
    public List<MetadataValue> findByFieldAndObjects(Context context, MetadataField metadataField,
                                                     List<UUID> objectIds) throws SQLException {
        if (objectIds == null || objectIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (objectIds.size() <= MAX_IN_CLAUSE_SIZE) {
            Query query = createQuery(context,
                "SELECT mv FROM MetadataValue mv WHERE mv.metadataField = :field "
                    + "AND mv.dSpaceObject.id IN (:ids) ORDER BY mv.dSpaceObject.id, mv.place");
            query.setParameter("field", metadataField);
            query.setParameter("ids", objectIds);

            @SuppressWarnings("unchecked")
            List<MetadataValue> result = query.getResultList();
            return result;
        }

        // Chunk into sublists to avoid exceeding database IN-clause limits
        List<MetadataValue> combined = new ArrayList<>();
        for (int start = 0; start < objectIds.size(); start += MAX_IN_CLAUSE_SIZE) {
            int end = Math.min(start + MAX_IN_CLAUSE_SIZE, objectIds.size());
            List<UUID> chunk = objectIds.subList(start, end);

            Query query = createQuery(context,
                "SELECT mv FROM MetadataValue mv WHERE mv.metadataField = :field "
                    + "AND mv.dSpaceObject.id IN (:ids) ORDER BY mv.dSpaceObject.id, mv.place");
            query.setParameter("field", metadataField);
            query.setParameter("ids", chunk);

            @SuppressWarnings("unchecked")
            List<MetadataValue> chunkResult = query.getResultList();
            combined.addAll(chunkResult);
        }
        return combined;
    }

    @Override
    public int deleteByIds(Context context, List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // N.B. Bulk HQL DELETE bypasses the persistence context — callers must
        // evict affected entities from the session to avoid stale references.
        if (ids.size() <= MAX_IN_CLAUSE_SIZE) {
            Query query = createQuery(context, "DELETE FROM MetadataValue mv WHERE mv.id IN (:ids)");
            query.setParameter("ids", ids);
            return query.executeUpdate();
        }

        // Chunk into sublists to avoid exceeding database IN-clause limits
        int totalDeleted = 0;
        for (int start = 0; start < ids.size(); start += MAX_IN_CLAUSE_SIZE) {
            int end = Math.min(start + MAX_IN_CLAUSE_SIZE, ids.size());
            List<Integer> chunk = ids.subList(start, end);

            Query query = createQuery(context, "DELETE FROM MetadataValue mv WHERE mv.id IN (:ids)");
            query.setParameter("ids", chunk);
            totalDeleted += query.executeUpdate();
        }
        return totalDeleted;
    }

}
