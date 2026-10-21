/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao.impl;

import static org.dspace.content.EntityType_.label;
import static org.dspace.layout.DynamicLayoutBox_.entitytype;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBox_;
import org.dspace.layout.dao.DynamicLayoutBoxDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Database Access Object implementation class for the DynamicLayoutBox object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutBoxDAOImpl extends AbstractHibernateDAO<DynamicLayoutBox> implements DynamicLayoutBoxDAO {

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public List<DynamicLayoutBox> findByEntityType(Context context, String entityType,
        Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<DynamicLayoutBox> query = cb.createQuery(DynamicLayoutBox.class);
        Root<DynamicLayoutBox> boxRoot = query.from(DynamicLayoutBox.class);
        boxRoot.fetch(entitytype, JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(boxRoot.get(entitytype).get(label), entityType));

        Predicate[] predicateArray = new Predicate[predicates.size()];
        predicates.toArray(predicateArray);

        query.select(boxRoot).where(predicateArray);
        TypedQuery<DynamicLayoutBox> exQuery = getHibernateSession(context).createQuery(query);

        if ( limit != null && offset != null ) {
            exQuery.setFirstResult(offset).setMaxResults(limit);
        }

        return exQuery.getResultList();
    }

    @Override
    public List<DynamicLayoutBox> findByEntityAndType(Context context, String entity, String type) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<DynamicLayoutBox> cq = cb.createQuery(DynamicLayoutBox.class);
        Root<DynamicLayoutBox> boxRoot = cq.from(DynamicLayoutBox.class);
        boxRoot.fetch(entitytype, JoinType.LEFT);
        cq.where(cb.and(cb.equal(boxRoot.get(entitytype).get(label), entity)),
            cb.equal(boxRoot.get(DynamicLayoutBox_.type), type));
        TypedQuery<DynamicLayoutBox> query = getHibernateSession(context).createQuery(cq);
        return query.getResultList();
    }
}
