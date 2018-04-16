/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;

import org.dspace.content.EntityType;
import org.dspace.content.dao.EntityTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class EntityTypeDAOImpl extends AbstractHibernateDAO<EntityType> implements EntityTypeDAO {

    public EntityType findByEntityType(Context context,String entityType) throws SQLException {
        Criteria criteria = createCriteria(context,EntityType.class);
        criteria.add(Restrictions.and(
            Restrictions.eq("label", entityType).ignoreCase()
        ));

        return singleResult(criteria);
    }
}
