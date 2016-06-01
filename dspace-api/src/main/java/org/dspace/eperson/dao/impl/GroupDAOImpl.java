/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.hibernate.Query;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Hibernate implementation of the Database Access Object interface class for the Group object.
 * This class is responsible for all database calls for the Group object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupDAOImpl extends AbstractHibernateDSODAO<Group> implements GroupDAO
{
    protected GroupDAOImpl()
    {
        super();
    }

    @Override
    public List<Group> findByMetadataField(Context context, String searchValue, MetadataField metadataField) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();
        String groupTableName = "g";
        queryBuilder.append("SELECT ").append(groupTableName).append(" FROM Group as ").append(groupTableName);

        addMetadataLeftJoin(queryBuilder, groupTableName, Collections.singletonList(metadataField));
        addMetadataValueWhereQuery(queryBuilder, Collections.singletonList(metadataField), "=", null);

        Query query = createQuery(context, queryBuilder.toString());
        query.setParameter(metadataField.toString(), metadataField.getID());
        query.setParameter("queryParam", searchValue);

        return list(query);
    }

    @Override
    public List<Group> findAll(Context context, List<MetadataField> sortMetadataFields) throws SQLException
    {
        StringBuilder queryBuilder = new StringBuilder();
        String groupTableName = "g";
        queryBuilder.append("SELECT ").append(groupTableName).append(" FROM Group as ").append(groupTableName);

        addMetadataLeftJoin(queryBuilder, groupTableName, sortMetadataFields);
        addMetadataSortQuery(queryBuilder, sortMetadataFields, null);

        Query query = createQuery(context, queryBuilder.toString());
        for (MetadataField sortField : sortMetadataFields) {
            query.setParameter(sortField.toString(), sortField.getID());
        }
        return list(query);
    }

    @Override
    public List<Group> findAll(Context context) throws SQLException {
        Query query = createQuery(context,
                "SELECT g FROM Group g ORDER BY g.name ASC");
        query.setCacheable(true);

        return list(query);
    }

    @Override
    public List<Group> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        Query query = createQuery(context, "from Group where (from EPerson e where e.id = :eperson_id) in elements(epeople)");
        query.setParameter("eperson_id", ePerson.getID());
        query.setCacheable(true);

        return list(query);
    }

    @Override
    public Group findByName(final Context context, final String name) throws SQLException {
        Query query = createQuery(context,
                "SELECT g from Group g " +
                "where g.name = :name ");

        query.setParameter("name", name);
        query.setCacheable(true);

        return singleResult(query);
    }

    @Override
    public Group findByNameAndMembership(Context context, String groupName, EPerson ePerson) throws SQLException {
        if(groupName == null || ePerson == null) {
            return null;
        } else {
            Query query = createQuery(context,
                    "SELECT DISTINCT g FROM Group g " +
                            "LEFT JOIN g.epeople p " +
                            "WHERE g.name = :name AND " +
                            "(p.id = :eperson_id OR " +
                            "EXISTS ( " +
                                "SELECT 1 FROM Group2GroupCache gc " +
                                "JOIN gc.parent p " +
                                "JOIN gc.child c " +
                                "JOIN c.epeople cp " +
                                "WHERE p.id = g.id AND cp.id = :eperson_id " +
                                ") " +
                            ")");

            query.setParameter("name", groupName);
            query.setParameter("eperson_id", ePerson.getID());
            query.setCacheable(true);

            return singleResult(query);
        }
    }

    @Override
    public List<Group> findByNameLike(final Context context, final String groupName, final int offset, final int limit) throws SQLException {
        Query query = createQuery(context,
                "SELECT g FROM Group g WHERE lower(g.name) LIKE lower(:name)");
        query.setParameter("name", "%" + StringUtils.trimToEmpty(groupName) + "%");

        if(0 <= offset)
        {
            query.setFirstResult(offset);
        }
        if(0 <= limit)
        {
            query.setMaxResults(limit);
        }

        return list(query);
    }

    @Override
    public int countByNameLike(final Context context, final String groupName) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*) FROM Group g WHERE lower(g.name) LIKE lower(:name)");
        query.setParameter("name", "%" + groupName + "%");

        return count(query);
    }

    @Override
    public void delete(Context context, Group group) throws SQLException {
        Query query = getHibernateSession(context).createSQLQuery("DELETE FROM group2group WHERE parent_id=:groupId or child_id=:groupId");
        query.setParameter("groupId", group.getID());
        query.executeUpdate();
        super.delete(context, group);
    }


    @Override
    public List<Pair<UUID, UUID>> getGroup2GroupResults(Context context, boolean flushQueries) throws SQLException {

        Query query = createQuery(context, "SELECT new org.apache.commons.lang3.tuple.ImmutablePair(g.id, c.id) " +
                "FROM Group g " +
                "JOIN g.groups c ");

        @SuppressWarnings("unchecked")
        List<Pair<UUID, UUID>> results = query.list();
        return results;
    }

    @Override
    public List<Group> getEmptyGroups(Context context) throws SQLException {
        return list(createQuery(context, "SELECT g from Group g where g.epeople is EMPTY"));
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Group"));
    }

}
