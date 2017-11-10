/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.dao.impl;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOI_;
import org.dspace.identifier.dao.DOIDAO;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the DOI object.
 * This class is responsible for all database calls for the DOI object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class DOIDAOImpl extends AbstractHibernateDAO<DOI> implements DOIDAO
{
    protected DOIDAOImpl()
    {
        super();
    }

    @Override
    public DOI findByDoi(Context context, String doi) throws SQLException {
//        Criteria criteria = createCriteria(context, DOI.class);
//        criteria.add(Restrictions.eq("doi", doi));
//        return uniqueResult(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, DOI.class);
        Root<DOI> doiRoot = criteriaQuery.from(DOI.class);
        criteriaQuery.select(doiRoot);
        criteriaQuery.where(criteriaBuilder.equal(doiRoot.get(DOI_.doi), doi));
        return uniqueResult(context, criteriaQuery, false, DOI.class, -1, -1);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso, List<Integer> statusToExclude) throws SQLException {
        //SELECT * FROM Doi WHERE resource_type_id = ? AND resource_id = ? AND resource_id = ? AND ((status != ? AND status != ?) OR status IS NULL)

        //TODO RAF CHECK
//        Criteria criteria = createCriteria(context, DOI.class);
//        Disjunction statusQuery = Restrictions.or();
//        Conjunction statusConjunctionAnd = Restrictions.and();
//        for (Integer status : statusToExclude) {
//            statusConjunctionAnd.add(Restrictions.not(Restrictions.eq("status", status)));
//        }
//        statusQuery.add(statusConjunctionAnd);
//        statusQuery.add(Restrictions.isNull("status"));
//        criteria.add(
//                Restrictions.and(
//                        Restrictions.eq("dSpaceObject", dso),
//                        statusQuery
//
//                )
//        );
//        return singleResult(criteria);

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, DOI.class);
        Root<DOI> doiRoot = criteriaQuery.from(DOI.class);
        criteriaQuery.select(doiRoot);

        List<Predicate> listToIncludeInOrPredicate = new LinkedList<>();

        for(Integer status : statusToExclude){
            listToIncludeInOrPredicate.add(criteriaBuilder.notEqual(doiRoot.get(DOI_.status), status));
        }
        listToIncludeInOrPredicate.add(criteriaBuilder.isNull(doiRoot.get(DOI_.status)));

        Predicate orPredicate = criteriaBuilder.or(listToIncludeInOrPredicate.toArray(new Predicate[]{}));

        criteriaQuery.where(criteriaBuilder.and(orPredicate,
                                                criteriaBuilder.equal(doiRoot.get(DOI_.dSpaceObject), dso)
                                                )
                            );

        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<DOI> findByStatus(Context context, List<Integer> statuses) throws SQLException {
//        Criteria criteria = createCriteria(context, DOI.class);
//        Disjunction statusQuery = Restrictions.or();
//        for (Integer status : statuses) {
//            statusQuery.add(Restrictions.eq("status", status));
//        }
//        criteria.add(statusQuery);
//        return list(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, DOI.class);
        Root<DOI> doiRoot = criteriaQuery.from(DOI.class);
        criteriaQuery.select(doiRoot);
        List<Predicate> orPredicates = new LinkedList<>();
        for( Integer status : statuses){
            orPredicates.add(criteriaBuilder.equal(doiRoot.get(DOI_.status), status));
        }
        criteriaQuery.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[]{})));
        return list(context, criteriaQuery, false, DOI.class, -1, -1);
    }
    
    @Override
    public List<DOI> findSimilarNotInState(Context context, String doi, List<Integer> excludedStatuses, boolean dsoNotNull)
            throws SQLException
    {
        // SELECT * FROM Doi WHERE doi LIKE ? AND resource_type_id = ? AND resource_id IS NOT NULL AND status != ? AND status != ?

        //TODO RAF CHECK
//        Criteria criteria = createCriteria(context, DOI.class);
//        Conjunction conjunctionAnd = Restrictions.and();
//        Disjunction statusQuery = Restrictions.or();
//        for (Integer status : excludedStatuses) {
//            statusQuery.add(Restrictions.ne("status", status));
//        }
//        conjunctionAnd.add(Restrictions.like("doi", doi));
//        conjunctionAnd.add(statusQuery);
//        if (dsoNotNull)
//        {
//            conjunctionAnd.add(Restrictions.isNotNull("dSpaceObject"));
//        }
//        criteria.add(conjunctionAnd);
//        return list(criteria);

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, DOI.class);
        Root<DOI> doiRoot = criteriaQuery.from(DOI.class);
        criteriaQuery.select(doiRoot);

        List<Predicate> listToIncludeInOrPredicate = new LinkedList<>();

        for(Integer status: excludedStatuses){
            listToIncludeInOrPredicate.add(criteriaBuilder.notEqual(doiRoot.get(DOI_.status), status));
        }

        List<Predicate> listToIncludeInAndPredicate = new LinkedList<>();

        listToIncludeInAndPredicate.add(criteriaBuilder.like(doiRoot.get(DOI_.doi), doi));
        listToIncludeInAndPredicate.add(criteriaBuilder.or(listToIncludeInOrPredicate.toArray(new Predicate[]{})));
        if(dsoNotNull){
            listToIncludeInAndPredicate.add(criteriaBuilder.isNotNull(doiRoot.get(DOI_.dSpaceObject)));
        }
        criteriaQuery.where(listToIncludeInAndPredicate.toArray(new Predicate[]{}));
        return list(context, criteriaQuery, false, DOI.class, -1, -1);



    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
//        Criteria criteria = createCriteria(context, DOI.class);
//        criteria.add(
//                Restrictions.and(
//                        Restrictions.eq("dSpaceObject", dso)
//                )
//        );
//        return singleResult(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, DOI.class);
        Root<DOI> doiRoot = criteriaQuery.from(DOI.class);
        criteriaQuery.select(doiRoot);
        criteriaQuery.where(criteriaBuilder.equal(doiRoot.get(DOI_.dSpaceObject), dso));
        return singleResult(context, criteriaQuery);
    }
}
