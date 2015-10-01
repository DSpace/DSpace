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
import org.dspace.eperson.dao.GroupDAO;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.type.StandardBasicTypes;

import java.sql.SQLException;
import java.util.*;

/**
 * Hibernate implementation of the Database Access Object interface class for the Group object.
 * This class is responsible for all database calls for the Group object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupDAOImpl extends AbstractHibernateDSODAO<Group> implements GroupDAO {

    @Override
    public Group findByMetadataField(Context context, String searchValue, MetadataField metadataField) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();
        String groupTableName = "g";
        queryBuilder.append("SELECT ").append(groupTableName).append(" FROM Group as ").append(groupTableName);

        addMetadataLeftJoin(queryBuilder, groupTableName, Collections.singletonList(metadataField));
        addMetadataValueWhereQuery(queryBuilder, Collections.singletonList(metadataField), "=", null);

        Query query = createQuery(context, queryBuilder.toString());
        query.setParameter(metadataField.toString(), metadataField.getFieldID());
        query.setParameter("queryParam", searchValue);

        return uniqueResult(query);
    }

    @Override
    public List<Group> findAll(Context context, List<MetadataField> sortFields, String sortColumn) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();
        String groupTableName = "g";
        queryBuilder.append("SELECT ").append(groupTableName).append(" FROM Group as ").append(groupTableName);

        addMetadataLeftJoin(queryBuilder, groupTableName, sortFields);
        addMetadataSortQuery(queryBuilder, sortFields, Collections.singletonList(sortColumn));

        Query query = createQuery(context, queryBuilder.toString());
        for (MetadataField sortField : sortFields) {
            query.setParameter(sortField.toString(), sortField.getFieldID());
        }
        return list(query);
    }

    @Override
    public List<Group> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        Query query = createQuery(context, "from Group where (from EPerson e where e.id = :eperson_id) in elements(epeople)");
        query.setParameter("eperson_id", ePerson.getID());
        return list(query);
    }

    @Override
    public List<Group> search(Context context, String query, List<MetadataField> queryFields, int offset, int limit) throws SQLException {
        String groupTableName = "g";
        String queryString = "SELECT " + groupTableName + " FROM Group as " + groupTableName;
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, ListUtils.EMPTY_LIST, null);

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
    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException {
        String groupTableName = "g";
        String queryString = "SELECT count(*) FROM Group as " + groupTableName;
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, ListUtils.EMPTY_LIST, null);

        return count(hibernateQuery);
    }

    protected Query getSearchQuery(Context context, String queryString, String queryParam, List<MetadataField> queryFields, List<MetadataField> sortFields, String sortField) throws SQLException {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(queryString);
        Set<MetadataField> metadataFieldsToJoin = new LinkedHashSet<>();
        metadataFieldsToJoin.addAll(queryFields);
        if(CollectionUtils.isNotEmpty(sortFields))
        {
            metadataFieldsToJoin.addAll(sortFields);
        }

        if(!CollectionUtils.isEmpty(metadataFieldsToJoin)) {
            addMetadataLeftJoin(queryBuilder, "g", metadataFieldsToJoin);
        }
        if(queryParam != null) {
            addMetadataValueWhereQuery(queryBuilder, queryFields, "like", null);
        }
        if(!CollectionUtils.isEmpty(sortFields)) {
            addMetadataSortQuery(queryBuilder, sortFields, Collections.singletonList(sortField));
        }

        Query query = createQuery(context, queryBuilder.toString());
        if(StringUtils.isNotBlank(queryParam)) {
            query.setParameter("queryParam", "%"+queryParam+"%");
        }
        for (MetadataField metadataField : metadataFieldsToJoin) {
            query.setParameter(metadataField.toString(), metadataField.getFieldID());
        }

        return query;
    }

    @Override
    public void delete(Context context, Group group) throws SQLException {
        Query query = getHibernateSession(context).createSQLQuery("DELETE FROM group2group WHERE parent_id=:groupId or child_id=:groupId");
        query.setParameter("groupId", group.getID());
        query.executeUpdate();
        super.delete(context, group);
    }


    @Override
    public List getGroup2GroupResults(Context context, boolean flushQueries) throws SQLException {
        SQLQuery sqlQuery = getHibernateSession(context).createSQLQuery("SELECT parent_id, child_id FROM Group2Group");
        sqlQuery.addScalar("parent_id", StandardBasicTypes.UUID_BINARY);
        sqlQuery.addScalar("child_id", StandardBasicTypes.UUID_BINARY);
        if(flushQueries){
            //Optional, flush queries when executing, could be usefull since you are using native queries & these sometimes require this option.
            sqlQuery.setFlushMode(FlushMode.ALWAYS);
        }
        return sqlQuery.list();
    }

}
