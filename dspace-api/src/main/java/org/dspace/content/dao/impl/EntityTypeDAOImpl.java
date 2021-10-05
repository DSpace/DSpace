/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.EntityType;
import org.dspace.content.EntityType_;
import org.dspace.content.dao.EntityTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the EntityType object.
 * This class is responsible for all database calls for the EntityType object
 * and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EntityTypeDAOImpl extends AbstractHibernateDAO<EntityType> implements EntityTypeDAO {

    @Override
    public EntityType findByEntityType(Context context, String entityType) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EntityType.class);
        Root<EntityType> entityTypeRoot = criteriaQuery.from(EntityType.class);
        criteriaQuery.select(entityTypeRoot);
        criteriaQuery.where(criteriaBuilder.equal(criteriaBuilder.upper(entityTypeRoot.get(EntityType_.label)),
                                                  entityType.toUpperCase()));
        return uniqueResult(context, criteriaQuery, true, EntityType.class);
    }
}
