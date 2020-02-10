/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.dao.impl;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.statistics.export.OpenURLTracker;
import org.dspace.statistics.export.dao.OpenURLTrackerDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the OpenURLTracker object.
 * This class is responsible for all database calls for the OpenURLTracker object and is autowired by spring
 * This class should never be accessed directly.
 *
 */
public class OpenURLTrackerDAOImpl extends AbstractHibernateDAO<OpenURLTracker> implements OpenURLTrackerDAO {

    protected OpenURLTrackerDAOImpl() {
        super();
    }

}
