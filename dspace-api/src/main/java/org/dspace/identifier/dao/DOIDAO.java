/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.GenericDAO;
import org.dspace.identifier.DOI;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the DOI object.
 * The implementation of this class is responsible for all database calls for the DOI object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface DOIDAO extends GenericDAO<DOI> {
    public DOI findByDoi(Session session, String doi) throws SQLException;

    public DOI findDOIByDSpaceObject(Session session, DSpaceObject dso, List<Integer> statusToExclude)
        throws SQLException;

    public List<DOI> findSimilarNotInState(Session session, String doi, List<Integer> statuses, boolean dsoNotNull)
        throws SQLException;

    public List<DOI> findByStatus(Session session, List<Integer> statuses) throws SQLException;

    public DOI findDOIByDSpaceObject(Session session, DSpaceObject dso) throws SQLException;
}
