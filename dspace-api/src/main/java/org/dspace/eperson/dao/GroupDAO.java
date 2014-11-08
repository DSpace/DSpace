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
import java.util.List;

/**
 * Database Access Object interface class for the Group object.
 * The implementation of this class is responsible for all database calls for the Group object and is autowired by spring
 * This class should only be accessed from a single service & should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface GroupDAO extends DSpaceObjectDAO<Group>, DSpaceObjectLegacySupportDAO<Group> {

    public Group findByMetadataField(Context context, String searchValue, MetadataField metadataField) throws SQLException;

    public List<Group> search(Context context, String query, List<MetadataField> queryFields, int offset, int limit) throws SQLException;

    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException;

    public List<Group> findAll(Context context, List<MetadataField> metadataFields, String sortColumn) throws SQLException;

    public List<Group> findByEPerson(Context context, EPerson ePerson) throws SQLException;

    public List getGroup2GroupResults(Context context, boolean flushQueries) throws SQLException;
}
