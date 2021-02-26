/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.ResourcePolicy_;
import org.dspace.content.Community;
import org.dspace.content.Community_;
import org.dspace.content.MetadataField;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Hibernate implementation of the Database Access Object interface class for the Community object.
 * This class is responsible for all database calls for the Community object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CommunityDAOImpl extends AbstractHibernateDSODAO<Community> implements CommunityDAO {
    protected CommunityDAOImpl() {
        super();
    }

    /**
     * Get a list of all communities in the system. These are alphabetically
     * sorted by community name.
     *
     * @param context DSpace context object
     * @param sortField sort field
     *
     * @return the communities in the system
     * @throws SQLException if database error
     */
    @Override
    public List<Community> findAll(Context context, MetadataField sortField) throws SQLException {
        return findAll(context, sortField, null, null);
    }

    @Override
    public List<Community> findAll(Context context, MetadataField sortField, Integer limit, Integer offset)
        throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();

        // The query has to be rather complex because we want to sort the retrieval of Communities based on the title
        // We'll join the Communities with the metadata fields on the sortfield specified in the parameters
        // then we'll sort on this metadata field (this is usually the title). We're also making sure that the place
        // is the lowest place in the metadata fields list so that we avoid the duplication bug
        queryBuilder.append("SELECT c" +
                                " FROM Community c" +
                                " left join c.metadata title on title.metadataField = :sortField and" +
                                " title.dSpaceObject = c.id and" +
                                " title.place = (select min(internal.place) " +
                                                "from c.metadata internal " +
                                                "where internal.metadataField = :sortField and" +
                                                    " internal.dSpaceObject = c.id)" +
                                " ORDER BY LOWER(title.value)");
        Query query = createQuery(context, queryBuilder.toString());
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        query.setParameter("sortField", sortField);

        return list(query);
    }

    @Override
    public Community findByAdminGroup(Context context, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Community.class);
        Root<Community> communityRoot = criteriaQuery.from(Community.class);
        criteriaQuery.select(communityRoot);
        criteriaQuery.where(criteriaBuilder.equal(communityRoot.get(Community_.admins), group));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<Community> findAllNoParent(Context context, MetadataField sortField) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();

        // The query has to be rather complex because we want to sort the retrieval of Communities based on the title
        // We'll join the Communities with the metadata fields on the sortfield specified in the parameters
        // then we'll sort on this metadata field (this is usually the title). We're also making sure that the place
        // is the lowest place in the metadata fields list so that we avoid the duplication bug
        // This query has the added where clause to enforce that the community cannot have any parent communities
        queryBuilder.append("SELECT c" +
                                " FROM Community c" +
                                " left join c.metadata title on title.metadataField = :sortField and" +
                                " title.dSpaceObject = c.id and" +
                                " title.place = (select min(internal.place) " +
                                                "from c.metadata internal " +
                                                "where internal.metadataField = :sortField and" +
                                                        " internal.dSpaceObject = c.id)" +
                                " WHERE c.parentCommunities IS EMPTY " +
                                " ORDER BY LOWER(title.value)");
        Query query = createQuery(context, queryBuilder.toString());
        query.setParameter("sortField", sortField);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return findMany(context, query);
    }

    @Override
    public List<Community> findAuthorized(Context context, EPerson ePerson, List<Integer> actions) throws SQLException {

        /*TableRowIterator tri = DatabaseManager.query(context,
                "SELECT \n" +
                        "  * \n" +
                        "FROM \n" +
                        "  public.eperson, \n" +
                        "  public.epersongroup2eperson, \n" +
                        "  public.epersongroup, \n" +
                        "  public.community, \n" +
                        "  public.resourcepolicy\n" +
                        "WHERE \n" +
                        "  epersongroup2eperson.eperson_id = eperson.eperson_id AND\n" +
                        "  epersongroup.eperson_group_id = epersongroup2eperson.eperson_group_id AND\n" +
                        "  resourcepolicy.epersongroup_id = epersongroup.eperson_group_id AND\n" +
                        "  resourcepolicy.resource_id = community.community_id AND\n" +
                        " ( resourcepolicy.action_id = 3 OR \n" +
                        "  resourcepolicy.action_id = 11) AND \n" +
                        "  resourcepolicy.resource_type_id = 4 AND eperson.eperson_id = ?", context.getCurrentUser()
                        .getID());
        */

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Community.class);
        Root<Community> communityRoot = criteriaQuery.from(Community.class);
        Join<Community, ResourcePolicy> join = communityRoot.join("resourcePolicies");
        List<Predicate> orPredicates = new LinkedList<Predicate>();
        for (Integer action : actions) {
            orPredicates.add(criteriaBuilder.equal(join.get(ResourcePolicy_.actionId), action));
        }
        Predicate orPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[] {}));
        criteriaQuery.select(communityRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(join.get(ResourcePolicy_.resourceTypeId), Constants.COMMUNITY),
                                criteriaBuilder.equal(join.get(ResourcePolicy_.eperson), ePerson),
                                orPredicate
            )
        );
        return list(context, criteriaQuery, true, Community.class, -1, -1);
    }

    @Override
    public List<Community> findAuthorizedByGroup(Context context, EPerson ePerson, List<Integer> actions)
        throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("select c from Community c join c.resourcePolicies rp join rp.epersonGroup rpGroup WHERE ");
        for (int i = 0; i < actions.size(); i++) {
            Integer action = actions.get(i);
            if (i != 0) {
                query.append(" AND ");
            }
            query.append("rp.actionId=").append(action);
        }
        query.append(" AND rp.resourceTypeId=").append(Constants.COMMUNITY);
        query.append(
            " AND rp.epersonGroup.id IN (select g.id from Group g where (from EPerson e where e.id = :eperson_id) in " +
                "elements(epeople))");
        Query persistenceQuery = createQuery(context, query.toString());
        persistenceQuery.setParameter("eperson_id", ePerson.getID());

        persistenceQuery.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(persistenceQuery);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Community"));
    }
}
