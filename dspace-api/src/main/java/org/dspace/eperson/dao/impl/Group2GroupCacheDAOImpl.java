/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.Group2GroupCache;
import org.dspace.eperson.Group2GroupCache_;
import org.dspace.eperson.dao.Group2GroupCacheDAO;
import org.hibernate.FlushMode;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the Group2GroupCache object.
 * This class is responsible for all database calls for the Group2GroupCache object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class Group2GroupCacheDAOImpl extends AbstractHibernateDAO<Group2GroupCache> implements Group2GroupCacheDAO {
    protected Group2GroupCacheDAOImpl() {
        super();
    }

    @Override
    public Set<Pair<UUID, UUID>> getCache(Context context) throws SQLException {
        // In Hibernate 7, we need to use a native SQL query to avoid auto-flush issues.
        // This is needed because during group deletion, Group2GroupCache entries
        // may reference deleted (transient) Group entities, which would cause
        // TransientPropertyValueException during auto-flush of HQL queries.
        List<Object[]> results = getHibernateSession(context)
            .createNativeQuery(
                "SELECT CAST(parent_id AS VARCHAR), CAST(child_id AS VARCHAR) FROM group2groupcache",
                Object[].class)
            .getResultList();

        Set<Pair<UUID, UUID>> cache = new HashSet<>();
        for (Object[] row : results) {
            UUID parentId = UUID.fromString((String) row[0]);
            UUID childId = UUID.fromString((String) row[1]);
            cache.add(Pair.of(parentId, childId));
        }
        return cache;
    }

    @Override
    public List<Group2GroupCache> findByParent(Context context, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Group2GroupCache.class);
        Root<Group2GroupCache> group2GroupCacheRoot = criteriaQuery.from(Group2GroupCache.class);
        criteriaQuery.select(group2GroupCacheRoot);
        criteriaQuery.where(criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.parent), group));
        return list(context, criteriaQuery, true, Group2GroupCache.class, -1, -1);
    }

    @Override
    public List<Group2GroupCache> findByChildren(Context context, Iterable<Group> groups) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Group2GroupCache.class);
        Root<Group2GroupCache> group2GroupCacheRoot = criteriaQuery.from(Group2GroupCache.class);
        List<Predicate> eqPredicates = new LinkedList<>();
        for (Group group : groups) {
            eqPredicates.add(criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.child), group));
        }
        Predicate orPredicate = criteriaBuilder.or(eqPredicates.toArray(new Predicate[] {}));
        criteriaQuery.select(group2GroupCacheRoot);
        criteriaQuery.where(orPredicate);
        return list(context, criteriaQuery, true, Group2GroupCache.class, -1, -1);
    }

    @Override
    public Group2GroupCache findByParentAndChild(Context context, Group parent, Group child) throws SQLException {
        // In Hibernate 7, loading Group2GroupCache entities into the session is problematic
        // because rethinkGroupCache uses native SQL to delete/insert rows, leaving loaded
        // entities stale in the session. Use FlushMode.MANUAL to prevent auto-flush issues,
        // and immediately detach the entity to prevent it from causing
        // TransientPropertyValueException during future flushes.
        Session session = getHibernateSession(context);
        FlushMode previousFlushMode = session.getHibernateFlushMode();
        session.setHibernateFlushMode(FlushMode.MANUAL);
        try {
            Query query = createQuery(context,
                "FROM Group2GroupCache g WHERE g.parent = :parentGroup AND g.child = :childGroup");
            query.setParameter("parentGroup", parent);
            query.setParameter("childGroup", child);
            Group2GroupCache result = singleResult(query);
            if (result != null) {
                session.detach(result);
            }
            return result;
        } finally {
            session.setHibernateFlushMode(previousFlushMode);
        }
    }

    @Override
    public Group2GroupCache find(Context context, Group parent, Group child) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Group2GroupCache.class);
        Root<Group2GroupCache> group2GroupCacheRoot = criteriaQuery.from(Group2GroupCache.class);
        criteriaQuery.select(group2GroupCacheRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.parent), parent),
                                criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.child), child)
            )
        );
        return uniqueResult(context, criteriaQuery, true, Group2GroupCache.class);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        Session session = getHibernateSession(context);

        // In Hibernate 7, Group2GroupCache entities have EAGER ManyToOne relationships
        // to Group. During auto-flush, if these entities reference a deleted Group,
        // Hibernate throws TransientPropertyValueException.
        // Strategy: disable auto-flush, load all entries, detach them from the session
        // (so they won't be checked during future flushes), then delete via native SQL.
        FlushMode previousFlushMode = session.getHibernateFlushMode();
        session.setHibernateFlushMode(FlushMode.MANUAL);
        try {
            // Load and detach all managed Group2GroupCache entities from the session
            List<Group2GroupCache> entries = session
                .createQuery("FROM Group2GroupCache", Group2GroupCache.class)
                .list();
            for (Group2GroupCache entry : entries) {
                session.detach(entry);
            }
            // Delete all rows via native SQL
            session.createNativeMutationQuery("DELETE FROM group2groupcache").executeUpdate();
        } finally {
            session.setHibernateFlushMode(previousFlushMode);
        }

        // Clear second-level cache for Group2GroupCache
        session.getSessionFactory().getCache().evictEntityData(Group2GroupCache.class);
    }

    @Override
    public void deleteFromCache(Context context, UUID parent, UUID child) throws SQLException {
        getHibernateSession(context).createNativeMutationQuery(
            "delete from group2groupcache g WHERE g.parent_id = :parent AND g.child_id = :child"
        )
        .setParameter("parent", parent)
        .setParameter("child", child)
        .executeUpdate();
    }

    @Override
    public void addToCache(Context context, UUID parent, UUID child) throws SQLException {
        getHibernateSession(context).createNativeMutationQuery(
            "insert into group2groupcache (parent_id, child_id) VALUES (:parent, :child)"
        )
        .setParameter("parent", parent)
        .setParameter("child", child)
        .executeUpdate();
    }
}
