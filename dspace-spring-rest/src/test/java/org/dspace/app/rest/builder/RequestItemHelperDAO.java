/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.builder;

import java.sql.SQLException;
import javax.persistence.Query;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supply missing "delete" operation on RequestItem, to support testing.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemHelperDAO extends AbstractHibernateDAO<RequestItem> {
    Logger LOG = LoggerFactory.getLogger(RequestItemHelperDAO.class);

    void delete(Context context, String token)
            throws SQLException {
        LOG.debug("delete request with token {}", token);

        Query delete = createQuery(context, "DELETE FROM "
                + RequestItem.class.getSimpleName()
                + " WHERE token = :token");
        delete.setParameter("token", token);
        int howmany = delete.executeUpdate();

        LOG.debug("Deleted {} requests", howmany);
    }
}
