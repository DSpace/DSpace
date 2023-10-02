/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;

import org.dspace.content.Site;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Site object.
 * The implementation of this class is responsible for all database calls for the Site object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SiteDAO extends DSpaceObjectDAO<Site> {

    public Site findSite(Session session) throws SQLException;
}
