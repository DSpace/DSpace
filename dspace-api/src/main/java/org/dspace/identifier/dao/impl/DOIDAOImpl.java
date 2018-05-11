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
import org.dspace.identifier.dao.DOIDAO;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
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
        Criteria criteria = createCriteria(context, DOI.class);
        criteria.add(Restrictions.eq("doi", doi));
        return uniqueResult(criteria);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso, List<Integer> statusToExclude) throws SQLException {
        //SELECT * FROM Doi WHERE resource_type_id = ? AND resource_id = ? AND resource_id = ? AND ((status != ? AND status != ?) OR status IS NULL)
        Criteria criteria = createCriteria(context, DOI.class);
        Disjunction statusQuery = Restrictions.or();
        Conjunction statusConjunctionAnd = Restrictions.and();
        for (Integer status : statusToExclude) {
            statusConjunctionAnd.add(Restrictions.not(Restrictions.eq("status", status)));
        }
        statusQuery.add(statusConjunctionAnd);
        statusQuery.add(Restrictions.isNull("status"));
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("dSpaceObject", dso),
                        statusQuery

                )
        );
        return singleResult(criteria);
    }

    @Override
    public List<DOI> findByStatus(Context context, List<Integer> statuses) throws SQLException {
        Criteria criteria = createCriteria(context, DOI.class);
        Disjunction statusQuery = Restrictions.or();
        for (Integer status : statuses) {
            statusQuery.add(Restrictions.eq("status", status));
        }
        criteria.add(statusQuery);
        return list(criteria);
    }
    
    @Override
    public List<DOI> findSimilarNotInState(Context context, String doi, List<Integer> excludedStatuses, boolean dsoNotNull)
            throws SQLException
    {
        // SELECT * FROM Doi WHERE doi LIKE ? AND resource_type_id = ? AND resource_id IS NOT NULL AND status != ? AND status != ?
        Criteria criteria = createCriteria(context, DOI.class);
        Conjunction conjunctionAnd = Restrictions.and();
        Disjunction statusQuery = Restrictions.or();
        for (Integer status : excludedStatuses) {
            statusQuery.add(Restrictions.ne("status", status));
        }
        conjunctionAnd.add(Restrictions.like("doi", doi));
        conjunctionAnd.add(statusQuery);
        if (dsoNotNull)
        {
            conjunctionAnd.add(Restrictions.isNotNull("dSpaceObject"));
        }
        criteria.add(conjunctionAnd);
        return list(criteria);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
        Criteria criteria = createCriteria(context, DOI.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("dSpaceObject", dso)
                )
        );
        return singleResult(criteria);
    }
}
