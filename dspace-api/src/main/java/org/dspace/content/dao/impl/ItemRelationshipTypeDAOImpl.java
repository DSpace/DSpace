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

import org.dspace.content.ItemRelationshipsType;
import org.dspace.content.ItemRelationshipsType_;
import org.dspace.content.dao.ItemRelationshipTypeDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class ItemRelationshipTypeDAOImpl extends AbstractHibernateDAO<ItemRelationshipsType> implements
                                                                                             ItemRelationshipTypeDAO {

    public ItemRelationshipsType findByEntityType(Context context, String entityType) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ItemRelationshipsType.class);
        Root<ItemRelationshipsType> entityTypeRoot = criteriaQuery.from(ItemRelationshipsType.class);
        criteriaQuery.select(entityTypeRoot);
        criteriaQuery
            .where(criteriaBuilder.equal(criteriaBuilder.upper(entityTypeRoot.get(ItemRelationshipsType_.label)),
                                         entityType.toUpperCase()));
        return uniqueResult(context, criteriaQuery, true, ItemRelationshipsType.class, -1, -1);
    }
}
