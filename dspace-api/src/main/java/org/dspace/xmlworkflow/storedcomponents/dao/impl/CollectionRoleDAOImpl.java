/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.Collection;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole_;
import org.dspace.xmlworkflow.storedcomponents.dao.CollectionRoleDAO;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the CollectionRole object.
 * This class is responsible for all database calls for the CollectionRole object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionRoleDAOImpl extends AbstractHibernateDAO<CollectionRole> implements CollectionRoleDAO {
    protected CollectionRoleDAOImpl() {
        super();
    }

    @Override
    public List<CollectionRole> findByCollection(Session session, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CollectionRole.class);
        Root<CollectionRole> collectionRoleRoot = criteriaQuery.from(CollectionRole.class);
        criteriaQuery.select(collectionRoleRoot);
        criteriaQuery.where(criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.collection), collection));
        return list(session, criteriaQuery, false, CollectionRole.class, -1, -1);
    }

    @Override
    public List<CollectionRole> findByGroup(Session session, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CollectionRole.class);
        Root<CollectionRole> collectionRoleRoot = criteriaQuery.from(CollectionRole.class);
        criteriaQuery.select(collectionRoleRoot);
        criteriaQuery.where(criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.group), group));
        return list(session, criteriaQuery, false, CollectionRole.class, -1, -1);
    }

    @Override
    public CollectionRole findByCollectionAndRole(Session session, Collection collection, String role)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CollectionRole.class);
        Root<CollectionRole> collectionRoleRoot = criteriaQuery.from(CollectionRole.class);
        criteriaQuery.select(collectionRoleRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.collection), collection),
                                criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.roleId), role)
            )
        );
        return uniqueResult(session, criteriaQuery, false, CollectionRole.class);

    }

    @Override
    public void deleteByCollection(Session session, Collection collection) throws SQLException {
        String hql = "delete from CollectionRole WHERE collection=:collection";
        Query query = createQuery(session, hql);
        query.setParameter("collection", collection);
        query.executeUpdate();
    }
}
