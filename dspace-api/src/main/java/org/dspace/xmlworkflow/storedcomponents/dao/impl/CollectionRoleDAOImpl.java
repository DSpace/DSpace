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
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole_;
import org.dspace.xmlworkflow.storedcomponents.dao.CollectionRoleDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the CollectionRole object.
 * This class is responsible for all database calls for the CollectionRole object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionRoleDAOImpl extends AbstractHibernateDAO<CollectionRole> implements CollectionRoleDAO {
    protected CollectionRoleDAOImpl() {
        super();
    }

    @Override
    public List<CollectionRole> findByCollection(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CollectionRole.class);
        Root<CollectionRole> collectionRoleRoot = criteriaQuery.from(CollectionRole.class);
        criteriaQuery.select(collectionRoleRoot);
        criteriaQuery.where(criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.collection), collection));
        return list(context, criteriaQuery, false, CollectionRole.class, -1, -1);
    }

    @Override
    public List<CollectionRole> findByGroup(Context context, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CollectionRole.class);
        Root<CollectionRole> collectionRoleRoot = criteriaQuery.from(CollectionRole.class);
        criteriaQuery.select(collectionRoleRoot);
        criteriaQuery.where(criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.group), group));
        return list(context, criteriaQuery, false, CollectionRole.class, -1, -1);
    }

    @Override
    public CollectionRole findByCollectionAndRole(Context context, Collection collection, String role)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, CollectionRole.class);
        Root<CollectionRole> collectionRoleRoot = criteriaQuery.from(CollectionRole.class);
        criteriaQuery.select(collectionRoleRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.collection), collection),
                                criteriaBuilder.equal(collectionRoleRoot.get(CollectionRole_.roleId), role)
            )
        );
        return uniqueResult(context, criteriaQuery, false, CollectionRole.class, -1, -1);

    }

    @Override
    public void deleteByCollection(Context context, Collection collection) throws SQLException {
        String hql = "delete from CollectionRole WHERE collection=:collection";
        Query query = createQuery(context, hql);
        query.setParameter("collection", collection);
        query.executeUpdate();
    }
}
