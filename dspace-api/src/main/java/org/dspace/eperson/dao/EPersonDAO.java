/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.dspace.content.MetadataField;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the EPerson object.
 * The implementation of this class is responsible for all database calls for the EPerson object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface EPersonDAO extends DSpaceObjectDAO<EPerson>, DSpaceObjectLegacySupportDAO<EPerson> {

    public EPerson findByEmail(Session session, String email) throws SQLException;

    public EPerson findByNetid(Session session, String netid) throws SQLException;

    public List<EPerson> search(Session session, String query, List<MetadataField> queryFields,
                                List<MetadataField> sortFields, int offset, int limit) throws SQLException;

    public int searchResultCount(Session session, String query, List<MetadataField> queryFields) throws SQLException;

    public List<EPerson> findByGroups(Session session, Set<Group> groups) throws SQLException;

    public List<EPerson> findWithPasswordWithoutDigestAlgorithm(Session session) throws SQLException;

    public List<EPerson> findNotActiveSince(Session session, Date date) throws SQLException;

    public List<EPerson> findAll(Session session, MetadataField metadataFieldSort, String sortColumn, int pageSize,
                                 int offset) throws SQLException;

    public List<EPerson> findAllSubscribers(Session session) throws SQLException;

    int countRows(Session session) throws SQLException;
}
