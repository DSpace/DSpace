/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao.impl;

import static org.dspace.content.EntityType_.label;
import static org.dspace.layout.CrisLayoutBox_.entitytype;

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
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox_;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Database Access Object implementation class for the CrisLayoutBox object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutBoxDAOImpl extends AbstractHibernateDAO<CrisLayoutBox> implements CrisLayoutBoxDAO {

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public List<CrisLayoutBox> findByEntityType(Context context, String entityType,
        Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<CrisLayoutBox> query = cb.createQuery(CrisLayoutBox.class);
        Root<CrisLayoutBox> boxRoot = query.from(CrisLayoutBox.class);
        boxRoot.fetch(entitytype, JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(boxRoot.get(entitytype).get(label), entityType));

        Predicate[] predicateArray = new Predicate[predicates.size()];
        predicates.toArray(predicateArray);

        query.select(boxRoot).where(predicateArray);
        TypedQuery<CrisLayoutBox> exQuery = getHibernateSession(context).createQuery(query);

        if ( limit != null && offset != null ) {
            exQuery.setFirstResult(offset).setMaxResults(limit);
        }

        return exQuery.getResultList();
    }

    @Override
    public List<CrisLayoutBox> findByEntityAndType(Context context, String entity, String type) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<CrisLayoutBox> cq = cb.createQuery(CrisLayoutBox.class);
        Root<CrisLayoutBox> boxRoot = cq.from(CrisLayoutBox.class);
        boxRoot.fetch(entitytype, JoinType.LEFT);
        cq.where(cb.and(cb.equal(boxRoot.get(entitytype).get(label), entity)),
            cb.equal(boxRoot.get(CrisLayoutBox_.type), type));
        TypedQuery<CrisLayoutBox> query = getHibernateSession(context).createQuery(cq);
        return query.getResultList();
    }
}
