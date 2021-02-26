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

import org.dspace.content.Site;
import org.dspace.content.dao.SiteDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the Site object.
 * This class is responsible for all database calls for the Site object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class SiteDAOImpl extends AbstractHibernateDAO<Site> implements SiteDAO {
    protected SiteDAOImpl() {
        super();
    }

    @Override
    public Site findSite(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Site.class);
        Root<Site> siteRoot = criteriaQuery.from(Site.class);
        criteriaQuery.select(siteRoot);
        return uniqueResult(context, criteriaQuery, true, Site.class, -1, -1);
    }
}
