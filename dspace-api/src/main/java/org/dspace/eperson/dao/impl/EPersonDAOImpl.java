/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson_;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.EPersonDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the EPerson object.
 * This class is responsible for all database calls for the EPerson object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EPersonDAOImpl extends AbstractHibernateDSODAO<EPerson> implements EPersonDAO {
    protected EPersonDAOImpl() {
        super();
    }

    @Override
    public EPerson findByEmail(Context context, String email) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EPerson.class);
        Root<EPerson> ePersonRoot = criteriaQuery.from(EPerson.class);
        criteriaQuery.select(ePersonRoot);
        criteriaQuery.where(criteriaBuilder.equal(ePersonRoot.get(EPerson_.email), email.toLowerCase()));
        return uniqueResult(context, criteriaQuery, true, EPerson.class);
    }


    @Override
    public EPerson findByNetid(Context context, String netid) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EPerson.class);
        Root<EPerson> ePersonRoot = criteriaQuery.from(EPerson.class);
        criteriaQuery.select(ePersonRoot);
        criteriaQuery.where((criteriaBuilder.equal(ePersonRoot.get(EPerson_.netid), netid)));
        return uniqueResult(context, criteriaQuery, true, EPerson.class);
    }

    @Override
    public List<EPerson> search(Context context, String query, List<MetadataField> queryFields,
                                List<MetadataField> sortFields, int offset, int limit) throws SQLException {
        String queryString = "SELECT " + EPerson.class.getSimpleName()
                                                      .toLowerCase() + " FROM EPerson as " + EPerson.class
            .getSimpleName().toLowerCase() + " ";

        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, null,
                                              sortFields, null, limit, offset);
        return list(hibernateQuery);
    }

    @Override
    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException {
        String queryString = "SELECT count(*) FROM EPerson as " + EPerson.class.getSimpleName().toLowerCase();
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, Collections.EMPTY_LIST, null);

        return count(hibernateQuery);
    }

    @Override
    public List<EPerson> searchNotMember(Context context, String query, List<MetadataField> queryFields,
                                         Group excludeGroup, List<MetadataField> sortFields,
                                         int offset, int limit) throws SQLException {
        String queryString = "SELECT " + EPerson.class.getSimpleName()
                                                      .toLowerCase() + " FROM EPerson as " + EPerson.class
            .getSimpleName().toLowerCase() + " ";

        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, excludeGroup,
                                              sortFields, null, limit, offset);
        return list(hibernateQuery);
    }

    public int searchNotMemberCount(Context context, String query, List<MetadataField> queryFields,
                                         Group excludeGroup) throws SQLException {
        String queryString = "SELECT count(*) FROM EPerson as " + EPerson.class.getSimpleName().toLowerCase();

        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, excludeGroup,
                                              Collections.EMPTY_LIST, null, -1, -1);
        return count(hibernateQuery);
    }

    @Override
    public List<EPerson> findAll(Context context, MetadataField metadataSortField, String sortField, int pageSize,
                                 int offset) throws SQLException {
        String queryString = "SELECT " + EPerson.class.getSimpleName()
                                                      .toLowerCase() + " FROM EPerson as " + EPerson.class
            .getSimpleName().toLowerCase();

        List<MetadataField> sortFields = Collections.EMPTY_LIST;

        if (metadataSortField != null) {
            sortFields = Collections.singletonList(metadataSortField);
        }

        Query query = getSearchQuery(context, queryString, null, ListUtils.EMPTY_LIST, null,
                                     sortFields, sortField, pageSize, offset);
        return list(query);

    }

    @Override
    public List<EPerson> findByGroups(Context context, Set<Group> groups, int pageSize, int offset)
        throws SQLException {
        Query query = createQuery(context,
                                  "SELECT DISTINCT e FROM EPerson e " +
                                      "JOIN e.groups g " +
                                      "WHERE g.id IN (:idList) ");

        List<UUID> idList = new ArrayList<>(groups.size());
        for (Group group : groups) {
            idList.add(group.getID());
        }
        query.setParameter("idList", idList);

        if (pageSize > 0) {
            query.setMaxResults(pageSize);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }

        return list(query);
    }

    @Override
    public int countByGroups(Context context, Set<Group> groups) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT count(DISTINCT e) FROM EPerson e " +
                                      "JOIN e.groups g " +
                                      "WHERE g.id IN (:idList) ");

        List<UUID> idList = new ArrayList<>(groups.size());
        for (Group group : groups) {
            idList.add(group.getID());
        }

        query.setParameter("idList", idList);

        return count(query);
    }

    @Override
    public List<EPerson> findWithPasswordWithoutDigestAlgorithm(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EPerson.class);
        Root<EPerson> ePersonRoot = criteriaQuery.from(EPerson.class);
        criteriaQuery.select(ePersonRoot);
        criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.isNotNull(ePersonRoot.get(EPerson_.password)),
                                                criteriaBuilder.isNull(ePersonRoot.get(EPerson_.digestAlgorithm))
                            )
        );
        return list(context, criteriaQuery, false, EPerson.class, -1, -1);
    }

    @Override
    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EPerson.class);
        Root<EPerson> ePersonRoot = criteriaQuery.from(EPerson.class);
        criteriaQuery.select(ePersonRoot);
        criteriaQuery.where(criteriaBuilder.lessThanOrEqualTo(ePersonRoot.get(EPerson_.lastActive), date));
        return list(context, criteriaQuery, false, EPerson.class, -1, -1);
    }

    protected Query getSearchQuery(Context context, String queryString, String queryParam,
                                   List<MetadataField> queryFields, List<MetadataField> sortFields, String sortField)
        throws SQLException {
        return getSearchQuery(context, queryString, queryParam, queryFields, null, sortFields, sortField, -1, -1);
    }

    /**
     * Build a search query across EPersons based on the given metadata fields and sorted based on the given metadata
     * field(s) or database column.
     * <P>
     * NOTE: the EPerson's email address is included in the search alongside any given metadata fields.
     *
     * @param context DSpace Context
     * @param queryString String which defines the beginning "SELECT" for the SQL query
     * @param queryParam Actual text being searched for
     * @param queryFields List of metadata fields to search within
     * @param excludeGroup Optional Group which should be excluded from search. Any EPersons who are members
     *                     of this group will not be included in the results.
     * @param sortFields Optional List of metadata fields to sort by (should not be specified if sortField is used)
     * @param sortField Optional database column to sort on (should not be specified if sortFields is used)
     * @param pageSize  how many results return
     * @param offset the position of the first result to return
     * @return built Query object
     * @throws SQLException if error occurs
     */
    protected Query getSearchQuery(Context context, String queryString, String queryParam,
                                   List<MetadataField> queryFields, Group excludeGroup,
                                   List<MetadataField> sortFields, String sortField,
                                    int pageSize, int offset) throws SQLException {
        // Initialize SQL statement using the passed in "queryString"
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(queryString);

        Set<MetadataField> metadataFieldsToJoin = new LinkedHashSet<>();
        metadataFieldsToJoin.addAll(queryFields);
        metadataFieldsToJoin.addAll(sortFields);

        // Append necessary join information for MetadataFields we will search within
        if (!CollectionUtils.isEmpty(metadataFieldsToJoin)) {
            addMetadataLeftJoin(queryBuilder, EPerson.class.getSimpleName().toLowerCase(), metadataFieldsToJoin);
        }
        // Always append a search on EPerson "email" based on query
        if (StringUtils.isNotBlank(queryParam)) {
            addMetadataValueWhereQuery(queryBuilder, queryFields, "like",
                                       EPerson.class.getSimpleName().toLowerCase() + ".email like :queryParam");
        }
        // If excludeGroup is specified, exclude members of that group from results
        // This uses a subquery to find the excluded group & verify that it is not in the EPerson list of "groups"
        if (excludeGroup != null) {
            // If query params exist, then we already have a WHERE clause (see above) and just need to append an AND
            if (StringUtils.isNotBlank(queryParam)) {
                queryBuilder.append(" AND ");
            } else {
                // no WHERE clause yet, so this is the start of the WHERE
                queryBuilder.append(" WHERE ");
            }
            queryBuilder.append("(FROM Group g where g.id = :group_id) NOT IN elements (")
                        .append(EPerson.class.getSimpleName().toLowerCase()).append(".groups)");
        }
        // Add sort/order by info to query, if specified
        if (!CollectionUtils.isEmpty(sortFields) || StringUtils.isNotBlank(sortField)) {
            addMetadataSortQuery(queryBuilder, sortFields, Collections.singletonList(sortField));
        }

        // Create the final SQL SELECT statement (based on included params above)
        Query query = createQuery(context, queryBuilder.toString());
        // Set pagesize & offset for pagination
        if (pageSize > 0) {
            query.setMaxResults(pageSize);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        // Set all parameters to the SQL SELECT statement (based on included params above)
        if (StringUtils.isNotBlank(queryParam)) {
            query.setParameter("queryParam", "%" + queryParam.toLowerCase() + "%");
        }
        for (MetadataField metadataField : metadataFieldsToJoin) {
            query.setParameter(metadataField.toString(), metadataField.getID());
        }
        if (excludeGroup != null) {
            query.setParameter("group_id", excludeGroup.getID());
        }

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return query;
    }

    @Override
    public List<EPerson> findAllSubscribers(Context context) throws SQLException {
        return list(createQuery(context, "SELECT DISTINCT e from Subscription s join s.ePerson e"));
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM EPerson"));
    }
}
