/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.ResourcePolicy_;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObject_;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Hibernate implementation of the Database Access Object interface class for the ResourcePolicy object.
 * This class is responsible for all database calls for the ResourcePolicy object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ResourcePolicyDAOImpl extends AbstractHibernateDAO<ResourcePolicy> implements ResourcePolicyDAO {

    protected ResourcePolicyDAOImpl() {
        super();
    }

    @Override
    public List<ResourcePolicy> findByDso(Context context, DSpaceObject dso) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso));
        return list(context, criteriaQuery, false, ResourcePolicy.class, -1, -1);
    }

    @Override
    public List<ResourcePolicy> findByDsoAndType(Context context, DSpaceObject dso, String type) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso),
                                       criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.rptype), type)
                   )
        );
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(resourcePolicyRoot.get(ResourcePolicy_.id)));
        criteriaQuery.orderBy(orderList);
        return list(context, criteriaQuery, false, ResourcePolicy.class, -1, -1);
    }

    @Override
    public List<ResourcePolicy> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.eperson), ePerson));
        return list(context, criteriaQuery, false, ResourcePolicy.class, -1, -1);
    }

    @Override
    public List<ResourcePolicy> findByGroup(Context context, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.epersonGroup), group));
        return list(context, criteriaQuery, false, ResourcePolicy.class, -1, -1);
    }

    @Override
    public List<ResourcePolicy> findByDSoAndAction(Context context, DSpaceObject dso, int actionId)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso),
                                       criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), actionId)
                   )
        );
        return list(context, criteriaQuery, false, ResourcePolicy.class, -1, -1);
    }

    @Override
    public List<ResourcePolicy> findByTypeGroupAction(Context context, DSpaceObject dso, Group group, int action)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso),
                                       criteriaBuilder
                                           .equal(resourcePolicyRoot.get(ResourcePolicy_.epersonGroup), group),
                                       criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), action)
                   )
        );
        return list(context, criteriaQuery, false, ResourcePolicy.class, 1, -1);
    }

    @Override
    public List<ResourcePolicy> findByTypeGroupActionExceptId(Context context, DSpaceObject dso, Group group,
                                                              int action, int notPolicyID) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso),
                                       criteriaBuilder
                                           .equal(resourcePolicyRoot.get(ResourcePolicy_.epersonGroup), group),
                                       criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), action),
                                       criteriaBuilder.notEqual(resourcePolicyRoot.get(ResourcePolicy_.id), notPolicyID)
                   )
        );
        return list(context, criteriaQuery, false, ResourcePolicy.class, 1, -1);
    }

    public List<ResourcePolicy> findByEPersonGroupTypeIdAction(Context context, EPerson e, List<Group> groups,
                                                               int action, int type_id) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.resourceTypeId), type_id),
                                criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), action),
                                criteriaBuilder
                                    .or(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.eperson), e),
                                        (resourcePolicyRoot.get(ResourcePolicy_.epersonGroup).in(groups)))
            )
        );
        return list(context, criteriaQuery, false, ResourcePolicy.class, 1, -1);
    }

    @Override
    public void deleteByDso(Context context, DSpaceObject dso) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject= :dSpaceObject";
        Query query = createQuery(context, queryString);
        query.setParameter("dSpaceObject", dso);
        query.executeUpdate();
    }

    @Override
    public void deleteByDsoAndAction(Context context, DSpaceObject dso, int actionId) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject= :dSpaceObject AND actionId= :actionId";
        Query query = createQuery(context, queryString);
        query.setParameter("dSpaceObject", dso);
        query.setParameter("actionId", actionId);
        query.executeUpdate();
    }

    @Override
    public void deleteByDsoAndType(Context context, DSpaceObject dso, String type) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject.id = :dsoId AND rptype = :rptype";
        Query query = createQuery(context, queryString);
        query.setParameter("dsoId", dso.getID());
        query.setParameter("rptype", type);
        query.executeUpdate();
    }

    @Override
    public void deleteByGroup(Context context, Group group) throws SQLException {
        String queryString = "delete from ResourcePolicy where epersonGroup= :epersonGroup";
        Query query = createQuery(context, queryString);
        query.setParameter("epersonGroup", group);
        query.executeUpdate();
    }

    @Override
    public void deleteByDsoGroupPolicies(Context context, DSpaceObject dso, Group group) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject = :dso AND epersonGroup= :epersonGroup";
        Query query = createQuery(context, queryString);
        query.setParameter("dso", dso);
        query.setParameter("epersonGroup", group);
        query.executeUpdate();

    }

    @Override
    public void deleteByDsoEPersonPolicies(Context context, DSpaceObject dso, EPerson ePerson) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject= :dso AND eperson= :eperson";
        Query query = createQuery(context, queryString);
        query.setParameter("dso", dso);
        query.setParameter("eperson", ePerson);
        query.executeUpdate();

    }

    @Override
    public void deleteByEPerson(Context context, EPerson ePerson) throws SQLException {
        String queryString = "delete from ResourcePolicy where eperson= :eperson";
        Query query = createQuery(context, queryString);
        query.setParameter("eperson", ePerson);
        query.executeUpdate();

    }

    @Override
    public void deleteByDsoAndTypeNotEqualsTo(Context context, DSpaceObject dso, String type) throws SQLException {

        String queryString = "delete from ResourcePolicy where dSpaceObject=:dso AND rptype <> :rptype";
        Query query = createQuery(context, queryString);
        query.setParameter("dso", dso);
        query.setParameter("rptype", type);
        query.executeUpdate();
    }

    @Override
    public List<ResourcePolicy> findByDSoAndActionExceptRpType(Context context, DSpaceObject dso, int action,
                                                               String rpType) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);

        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        if (rpType != null) {
            criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso),
                                    criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), action),
                                    criteriaBuilder.or(
                                            criteriaBuilder.notEqual(resourcePolicyRoot.get(ResourcePolicy_.rptype),
                                                    rpType),
                                            criteriaBuilder.isNull(resourcePolicyRoot.get(ResourcePolicy_.rptype))
                                    )
                )
            );
        } else {
            criteriaQuery.where(
                    criteriaBuilder.and(
                            criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.dSpaceObject), dso),
                            criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), action),
                            criteriaBuilder.isNotNull(resourcePolicyRoot.get(ResourcePolicy_.rptype))
                    )
            );
        }
        return list(context, criteriaQuery, false, ResourcePolicy.class, 1, 1);
    }

    @Override
    public List<ResourcePolicy> findByEPerson(Context context, EPerson ePerson, int offset, int limit)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.eperson), ePerson));
        return list(context, criteriaQuery, false, ResourcePolicy.class, limit, offset);
    }

    @Override
    public int countByEPerson(Context context, EPerson ePerson) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(*) FROM " + ResourcePolicy.class.getSimpleName() + " WHERE eperson_id = (:epersonUuid) ");
        query.setParameter("epersonUuid", ePerson.getID());
        return count(query);
    }

    @Override
    public List<ResourcePolicy> findByEPersonAndResourceUuid(Context context, EPerson ePerson, UUID resourceUuid,
            int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        Join<ResourcePolicy, DSpaceObject> join = resourcePolicyRoot.join(ResourcePolicy_.dSpaceObject);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.eperson), ePerson),
                        criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceUuid)));
        return list(context, criteriaQuery, false, ResourcePolicy.class, limit, offset);
    }

    @Override
    public int countByEPersonAndResourceUuid(Context context, EPerson eperson, UUID resourceUuid) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM " + ResourcePolicy.class.getSimpleName()
                + " WHERE eperson_id = (:epersonUuid) AND dspace_object = (:resourceUuid) ");
        query.setParameter("resourceUuid", resourceUuid);
        query.setParameter("epersonUuid", eperson.getID());
        return count(query);
    }

    @Override
    public List<ResourcePolicy> findByResouceUuidAndActionId(Context context, UUID resourceUuid, int actionId,
            int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        Join<ResourcePolicy, DSpaceObject> join = resourcePolicyRoot.join(ResourcePolicy_.dSpaceObject);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.actionId), actionId),
                        criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceUuid)));
        return list(context, criteriaQuery, false, ResourcePolicy.class, limit, offset);
    }

    @Override
    public int countByResouceUuidAndActionId(Context context, UUID resourceUuid, int actionId) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM " + ResourcePolicy.class.getSimpleName()
                + " WHERE dspace_object = (:resourceUuid) AND action_id = (:actionId) ");
        query.setParameter("resourceUuid", resourceUuid);
        query.setParameter("actionId", actionId);
        return count(query);
    }

    @Override
    public List<ResourcePolicy> findByResouceUuid(Context context, UUID resourceUuid, int offset, int limit)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        Join<ResourcePolicy, DSpaceObject> join = resourcePolicyRoot.join(ResourcePolicy_.dSpaceObject);
        criteriaQuery.where(criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceUuid));
        return list(context, criteriaQuery, false, ResourcePolicy.class, limit, offset);
    }

    @Override
    public int countByResourceUuid(Context context, UUID resourceUuid) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM " + ResourcePolicy.class.getSimpleName()
                + " WHERE dspace_object = (:resourceUuid) ");
        query.setParameter("resourceUuid", resourceUuid);
        return count(query);
    }

    @Override
    public List<ResourcePolicy> findByGroup(Context context, Group group, int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.epersonGroup), group));
        return list(context, criteriaQuery, false, ResourcePolicy.class, limit, offset);
    }

    @Override
    public int countResourcePolicyByGroup(Context context, Group group) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) " + "FROM " + ResourcePolicy.class.getSimpleName()
                + " WHERE epersongroup_id = (:groupUuid) ");
        query.setParameter("groupUuid", group.getID());
        return count(query);
    }

    @Override
    public List<ResourcePolicy> findByGroupAndResourceUuid(Context context, Group group, UUID resourceUuid,
            int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        Join<ResourcePolicy, DSpaceObject> join = resourcePolicyRoot.join(ResourcePolicy_.dSpaceObject);
        criteriaQuery.where(
                criteriaBuilder.and(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.epersonGroup), group),
                        criteriaBuilder.equal(join.get(DSpaceObject_.id), resourceUuid)));
        return list(context, criteriaQuery, false, ResourcePolicy.class, limit, offset);
    }

    @Override
    public int countByGroupAndResourceUuid(Context context, Group group, UUID resourceUuid) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM " + ResourcePolicy.class.getSimpleName()
                + " WHERE dspace_object = (:resourceUuid) AND epersongroup_id = (:groupUuid) ");
        query.setParameter("resourceUuid", resourceUuid);
        query.setParameter("groupUuid", group.getID());
        return count(query);
    }

    @Override
    public ResourcePolicy findOneById(Context context, Integer id) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ResourcePolicy.class);
        Root<ResourcePolicy> resourcePolicyRoot = criteriaQuery.from(ResourcePolicy.class);
        criteriaQuery.select(resourcePolicyRoot);
        criteriaQuery.where(criteriaBuilder.equal(resourcePolicyRoot.get(ResourcePolicy_.id), id));
        return singleResult(context, criteriaQuery);
    }
}
