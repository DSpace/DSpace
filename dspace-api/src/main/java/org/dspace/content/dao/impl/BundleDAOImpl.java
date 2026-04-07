/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Bundle;
import org.dspace.content.dao.BundleDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the Bundle object.
 * This class is responsible for all database calls for the Bundle object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BundleDAOImpl extends AbstractHibernateDSODAO<Bundle> implements BundleDAO {
    protected BundleDAOImpl() {
        super();
    }


    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from Bundle"));
    }

    @Override
    public void lockForWrite(Context context, Bundle bundle) throws SQLException {
        Session session = getHibernateSession(context);
        // Flush pending changes first so they are visible in the refresh query.
        // Without this, any in-memory associations (e.g. item2bundle) that have not yet been
        // flushed would be absent from the reloaded entity state.
        session.flush();
        session.refresh(bundle, new LockOptions(LockMode.PESSIMISTIC_WRITE));
    }
}
