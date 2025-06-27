/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect.dao.impl;

import java.sql.SQLException;
import javax.persistence.Query;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.handleredirect.HandleRedirect;
import org.dspace.handleredirect.dao.HandleRedirectDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the HandleRedirect object.
 * This class is responsible for all database calls for the HandleRedirect object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Ying Jin at rice.edu Updated from HandleDAOImpl.java
 */
public class HandleRedirectDAOImpl extends AbstractHibernateDAO<HandleRedirect> implements HandleRedirectDAO {

    protected HandleRedirectDAOImpl() {
        super();
    }

    @Override
    public HandleRedirect findByHandleRedirect(Context context, String handle) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT h " +
                                      "FROM HandleRedirect h " +
                                          "WHERE h.handle = :handle");

        query.setParameter("handle", handle);

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return singleResult(query);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM HandleRedirect"));
    }

}
