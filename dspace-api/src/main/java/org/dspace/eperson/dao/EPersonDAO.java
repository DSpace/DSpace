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
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Database Access Object interface class for the EPerson object.
 * The implementation of this class is responsible for all database calls for the EPerson object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface EPersonDAO extends DSpaceObjectDAO<EPerson>, DSpaceObjectLegacySupportDAO<EPerson> {

    public EPerson findByEmail(Context context, String email) throws SQLException;

    public EPerson findByNetid(Context context, String netid) throws SQLException;

    /**
     * Search all EPersons by the given MetadataField objects, sorting by the given sort fields.
     * <P>
     * NOTE: As long as a query is specified, the EPerson's email address is included in the search alongside any given
     * metadata fields.
     *
     * @param context DSpace context
     * @param query the text to search EPersons for
     * @param queryFields the metadata fields to search within (email is also included automatically)
     * @param sortFields the metadata field(s) to sort the results by
     * @param offset the position of the first result to return
     * @param limit how many results return
     * @return List of matching EPerson objects
     * @throws SQLException if an error occurs
     */
    public List<EPerson> search(Context context, String query, List<MetadataField> queryFields,
                                List<MetadataField> sortFields, int offset, int limit) throws SQLException;

    /**
     * Count number of EPersons who match a search on the given metadata fields. This returns the count of total
     * results for the same query using the 'search()', and therefore can be used to provide pagination.
     *
     * @param context DSpace context
     * @param query the text to search EPersons for
     * @param queryFields the metadata fields to search within (email is also included automatically)
     * @return total number of EPersons who match the query
     * @throws SQLException if an error occurs
     */
    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException;

    /**
     * Search all EPersons via their firstname, lastname, email (fuzzy match), limited to those EPersons which are NOT
     * a member of the given group. This may be used to search across EPersons which are valid to add as members to the
     * given group.
     *
     * @param context The DSpace context
     * @param query the text to search EPersons for
     * @param queryFields the metadata fields to search within (email is also included automatically)
     * @param excludeGroup Group to exclude results from. Members of this group will never be returned.
     * @param offset the position of the first result to return
     * @param limit how many results return
     * @return EPersons matching the query (which are not members of the given group)
     * @throws SQLException if database error
     */
    List<EPerson> searchNotMember(Context context, String query, List<MetadataField> queryFields, Group excludeGroup,
                                  List<MetadataField> sortFields, int offset, int limit) throws SQLException;

    /**
     * Count number of EPersons that match a given search (fuzzy match) across firstname, lastname and email. This
     * search is limited to those EPersons which are NOT a member of the given group. This may be used
     * (with searchNotMember()) to perform a paginated search across EPersons which are valid to add to the given group.
     *
     * @param context The DSpace context
     * @param query querystring to fuzzy match against.
     * @param queryFields the metadata fields to search within (email is also included automatically)
     * @param excludeGroup Group to exclude results from. Members of this group will never be returned.
     * @return Groups matching the query (which are not members of the given parent)
     * @throws SQLException if database error
     */
    int searchNotMemberCount(Context context, String query, List<MetadataField> queryFields, Group excludeGroup)
        throws SQLException;

    /**
     * Find all EPersons who are a member of one or more of the listed groups in a paginated fashion. This returns
     * EPersons ordered by UUID.
     *
     * @param context current Context
     * @param groups Set of group(s) to check membership in
     * @param pageSize number of EPerson objects to load at one time. Set to <=0 to disable pagination
     * @param offset number of page to load (starting with 1). Set to <=0 to disable pagination
     * @return List of all EPersons who are a member of one or more groups.
     * @throws SQLException
     */
    List<EPerson> findByGroups(Context context, Set<Group> groups, int pageSize, int offset) throws SQLException;

    /**
     * Count total number of EPersons who are a member of one or more of the listed groups. This provides the total
     * number of results to expect from corresponding findByGroups() for pagination purposes.
     *
     * @param context current Context
     * @param groups Set of group(s) to check membership in
     * @return total number of (unique) EPersons who are a member of one or more groups.
     * @throws SQLException
     */
    int countByGroups(Context context, Set<Group> groups) throws SQLException;

    public List<EPerson> findWithPasswordWithoutDigestAlgorithm(Context context) throws SQLException;

    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException;

    public List<EPerson> findAll(Context context, MetadataField metadataFieldSort, String sortColumn, int pageSize,
                                 int offset) throws SQLException;

    public List<EPerson> findAllSubscribers(Context context) throws SQLException;

    int countRows(Context context) throws SQLException;
}
