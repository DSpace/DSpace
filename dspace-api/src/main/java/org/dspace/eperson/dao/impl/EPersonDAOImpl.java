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
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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
 * This class is responsible for all database calls for the EPerson object and is autowired by spring
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
        return uniqueResult(context, criteriaQuery, true, EPerson.class, -1, -1);
    }


    @Override
    public EPerson findByNetid(Context context, String netid) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, EPerson.class);
        Root<EPerson> ePersonRoot = criteriaQuery.from(EPerson.class);
        criteriaQuery.select(ePersonRoot);
        criteriaQuery.where((criteriaBuilder.equal(ePersonRoot.get(EPerson_.netid), netid)));
        return uniqueResult(context, criteriaQuery, true, EPerson.class, -1, -1);
    }

    @Override
    public List<EPerson> search(Context context, String query, List<MetadataField> queryFields,
                                List<MetadataField> sortFields, int offset, int limit) throws SQLException {
        String queryString = "SELECT " + EPerson.class.getSimpleName()
                                                      .toLowerCase() + " FROM EPerson as " + EPerson.class
            .getSimpleName().toLowerCase() + " ";
        if (query != null) {
            query = "%" + query.toLowerCase() + "%";
        }
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, sortFields, null);

        if (0 <= offset) {
            hibernateQuery.setFirstResult(offset);
        }
        if (0 <= limit) {
            hibernateQuery.setMaxResults(limit);
        }
        return list(hibernateQuery);
    }

    @Override
    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException {
        String queryString = "SELECT count(*) FROM EPerson as " + EPerson.class.getSimpleName().toLowerCase();
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, Collections.EMPTY_LIST, null);

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

        Query query = getSearchQuery(context, queryString, null, ListUtils.EMPTY_LIST, sortFields, sortField, pageSize,
                                     offset);
        return list(query);

    }

    @Override
    public List<EPerson> findByGroups(Context context, Set<Group> groups) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT DISTINCT e FROM EPerson e " +
                                      "JOIN e.groups g " +
                                      "WHERE g.id IN (:idList) ");

        List<UUID> idList = new ArrayList<>(groups.size());
        for (Group group : groups) {
            idList.add(group.getID());
        }

        query.setParameter("idList", idList);

        return list(query);
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
        return getSearchQuery(context, queryString, queryParam, queryFields, sortFields, sortField, -1, -1);
    }

    protected Query getSearchQuery(Context context, String queryString, String queryParam,
                                   List<MetadataField> queryFields, List<MetadataField> sortFields, String sortField,
                                   int pageSize, int offset) throws SQLException {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(queryString);
        Set<MetadataField> metadataFieldsToJoin = new LinkedHashSet<>();
        metadataFieldsToJoin.addAll(queryFields);
        metadataFieldsToJoin.addAll(sortFields);

        if (!CollectionUtils.isEmpty(metadataFieldsToJoin)) {
            addMetadataLeftJoin(queryBuilder, EPerson.class.getSimpleName().toLowerCase(), metadataFieldsToJoin);
        }
        if (queryParam != null) {
            addMetadataValueWhereQuery(queryBuilder, queryFields, "like",
                                       EPerson.class.getSimpleName().toLowerCase() + ".email like :queryParam");
        }
        if (!CollectionUtils.isEmpty(sortFields) || StringUtils.isNotBlank(sortField)) {
            addMetadataSortQuery(queryBuilder, sortFields, Collections.singletonList(sortField));
        }

        Query query = createQuery(context, queryBuilder.toString());
        if (pageSize > 0) {
            query.setMaxResults(pageSize);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        if (StringUtils.isNotBlank(queryParam)) {
            query.setParameter("queryParam", "%" + queryParam.toLowerCase() + "%");
        }
        for (MetadataField metadataField : metadataFieldsToJoin) {
            query.setParameter(metadataField.toString(), metadataField.getID());
        }

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
