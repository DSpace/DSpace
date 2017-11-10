/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.Site;
import org.dspace.content.dao.SiteDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.identifier.DOI;
import org.hibernate.Criteria;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.SQLException;

/**
 * Hibernate implementation of the Database Access Object interface class for the Site object.
 * This class is responsible for all database calls for the Site object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class SiteDAOImpl extends AbstractHibernateDAO<Site> implements SiteDAO
{
    protected SiteDAOImpl()
    {
        super();
    }

    @Override
    public Site findSite(Context context) throws SQLException {
//        Criteria criteria = createCriteria(context, Site.class);
//        criteria.setCacheable(true);
//        return uniqueResult(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Site.class);
        Root<Site> siteRoot = criteriaQuery.from(Site.class);
        criteriaQuery.select(siteRoot);
//        criteriaQuery.where(criteriaBuilder.equal(root.get("doi"), doi));
        return uniqueResult(context, criteriaQuery, true, Site.class, -1, -1);
    }
}
