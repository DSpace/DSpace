/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Helper class to execute Hibernate queries in the DSpace test classes
 */
public class HibernateTestUtil {

    private HibernateTestUtil() {
        //private constructor required for utility classes
    }

    public static Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }

    public static SessionFactory getHibernateSessionFactory(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession()).getSessionFactory();
    }

}
