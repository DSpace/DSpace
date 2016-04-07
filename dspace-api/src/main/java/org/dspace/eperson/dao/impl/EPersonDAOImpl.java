/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.EPersonDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.*;

/**
 * Hibernate implementation of the Database Access Object interface class for the EPerson object.
 * This class is responsible for all database calls for the EPerson object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EPersonDAOImpl extends AbstractHibernateDSODAO<EPerson> implements EPersonDAO
{
    protected EPersonDAOImpl()
    {
        super();
    }

    @Override
    public EPerson findByEmail(Context context, String email) throws SQLException
    {
        Query query = createQuery(context,
                "SELECT p " +
                "FROM EPerson p " +
                "WHERE p.email = :email" );

        query.setParameter("email", email.toLowerCase());

        return uniqueResult(query);
    }


    @Override
    public EPerson findByNetid(Context context, String netid) throws SQLException
    {
        Query query = createQuery(context,
                "SELECT p " +
                        "FROM EPerson p " +
                        "WHERE p.netid = :netid" );

        query.setParameter("netid", netid);

        return uniqueResult(query);
    }

    @Override
    public List<EPerson> search(Context context, String query, List<MetadataField> queryFields, List<MetadataField> sortFields, int offset, int limit) throws SQLException
    {
        String queryString = "SELECT " + EPerson.class.getSimpleName().toLowerCase() + " FROM EPerson as " + EPerson.class.getSimpleName().toLowerCase() + " ";

        if(query != null) query= "%"+query.toLowerCase()+"%";
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, sortFields, null);

        if(0 <= offset)
        {
            hibernateQuery.setFirstResult(offset);
        }
        if(0 <= limit)
        {
            hibernateQuery.setMaxResults(limit);
        }
        return list(hibernateQuery);
    }

    @Override
    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException
    {
        String queryString = "SELECT count(*) FROM EPerson as " + EPerson.class.getSimpleName().toLowerCase();
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, ListUtils.EMPTY_LIST, null);

        return count(hibernateQuery);
    }

    @Override
    public List<EPerson> findAll(Context context, MetadataField metadataSortField, String sortField) throws SQLException {
        String queryString = "SELECT " + EPerson.class.getSimpleName().toLowerCase() + " FROM EPerson as " + EPerson.class.getSimpleName().toLowerCase();

        List<MetadataField> sortFields = ListUtils.EMPTY_LIST;

        if(metadataSortField!=null){
            sortFields =  Collections.singletonList(metadataSortField);
        }

        Query query = getSearchQuery(context, queryString, null, ListUtils.EMPTY_LIST, sortFields, sortField);
        return list(query);

    }

    @Override
    public List<EPerson> findByGroups(Context context, Set<Group> groups) throws SQLException {
        Criteria criteria = createCriteria(context, EPerson.class);
        criteria.createAlias("groups", "g");
        Disjunction orRestriction = Restrictions.or();
        for(Group group : groups)
        {
            orRestriction.add(Restrictions.eq("g.id", group.getID()));
        }
        criteria.add(orRestriction);
        return list(criteria);
    }


    @Override
    public List<EPerson> findWithPasswordWithoutDigestAlgorithm(Context context) throws SQLException {
        Query query = createQuery(context,
                "SELECT p " +
                        "FROM EPerson p " +
                        "WHERE p.password IS NOT NULL AND p.digestAlgorithm IS NULL " );

        return list(query);
    }

    @Override
    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException {
        Query query = createQuery(context,
                "SELECT p " +
                        "FROM EPerson p " +
                        "WHERE p.lastActive <= :date " );
        query.setParameter("date", date);

        return list(query);
    }

    protected Query getSearchQuery(Context context, String queryString, String queryParam, List<MetadataField> queryFields, List<MetadataField> sortFields, String sortField) throws SQLException {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(queryString);
        Set<MetadataField> metadataFieldsToJoin = new LinkedHashSet<>();
        metadataFieldsToJoin.addAll(queryFields);
        metadataFieldsToJoin.addAll(sortFields);

        if(!CollectionUtils.isEmpty(metadataFieldsToJoin)) {
            addMetadataLeftJoin(queryBuilder, EPerson.class.getSimpleName().toLowerCase(), metadataFieldsToJoin);
        }
        if(queryParam != null) {
            addMetadataValueWhereQuery(queryBuilder, queryFields, "like", EPerson.class.getSimpleName().toLowerCase() + ".email like :queryParam");
        }
        if(!CollectionUtils.isEmpty(sortFields)) {
            addMetadataSortQuery(queryBuilder, sortFields, Collections.singletonList(sortField));
        }

        Query query = createQuery(context, queryBuilder.toString());
        if(StringUtils.isNotBlank(queryParam)) {
            query.setParameter("queryParam", "%"+queryParam.toLowerCase()+"%");
        }
        for (MetadataField metadataField : metadataFieldsToJoin) {
            query.setParameter(metadataField.toString(), metadataField.getID());
        }

        return query;
    }

    @Override
    public List<EPerson> findAllSubscribers(Context context) throws SQLException {
        return list(createQuery(context, "SELECT DISTINCT e from Subscription s JOIN s.ePerson e "));
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM EPerson"));
    }
}
