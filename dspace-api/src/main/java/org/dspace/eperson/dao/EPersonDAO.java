/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import org.dspace.content.MetadataField;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Database Access Object interface class for the EPerson object.
 * The implementation of this class is responsible for all database calls for the EPerson object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface EPersonDAO extends DSpaceObjectDAO<EPerson>, DSpaceObjectLegacySupportDAO<EPerson> {

    public EPerson findByEmail(Context context, String email) throws SQLException;

    public EPerson findByNetid(Context context, String netid) throws SQLException;

    public List<EPerson> search(Context context, String query, List<MetadataField> queryFields, List<MetadataField> sortFields, int offset, int limit) throws SQLException;

    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException;

    public List<EPerson> findByGroups(Context context, Set<Group> groups) throws SQLException;

    public List<EPerson> findWithPasswordWithoutDigestAlgorithm(Context context) throws SQLException;

    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException;

    public List<EPerson> findAll(Context context, MetadataField metadataFieldSort, String sortColumn, int pageSize, int offset) throws SQLException;

    public List<EPerson> findAllSubscribers(Context context) throws SQLException;

    int countRows(Context context) throws SQLException;
}
