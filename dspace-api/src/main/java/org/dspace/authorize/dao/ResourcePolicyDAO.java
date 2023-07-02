/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the ResourcePolicy object.
 * The implementation of this class is responsible for all database calls for the ResourcePolicy object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ResourcePolicyDAO extends GenericDAO<ResourcePolicy> {

    public List<ResourcePolicy> findByDso(Session session, DSpaceObject dso) throws SQLException;

    public List<ResourcePolicy> findByDsoAndType(Session session, DSpaceObject dSpaceObject, String type)
        throws SQLException;

    public List<ResourcePolicy> findByEPerson(Session session, EPerson ePerson) throws SQLException;

    public List<ResourcePolicy> findByGroup(Session session, Group group) throws SQLException;

    public List<ResourcePolicy> findByDSoAndAction(Session session, DSpaceObject dso, int actionId) throws SQLException;

    public void deleteByDsoAndTypeAndAction(Session session, DSpaceObject dSpaceObject, String type, int action)
        throws SQLException;

    public List<ResourcePolicy> findByTypeGroupAction(Session session, DSpaceObject dso, Group group, int action)
        throws SQLException;

    /**
     * Look for ResourcePolicies by DSpaceObject, Group, and action, ignoring
     * IDs with a specific PolicyID.  This method can be used to detect
     * duplicate ResourcePolicies.
     *
     * @param session the current request's database context.
     * @param dso the object in question.
     * @param group the group in question.
     * @param action the required action.
     * @param notPolicyID ResourcePolicies with this ID will be ignored while
     *                    looking for equal ResourcePolicies.
     * @return List of resource policies for the same DSpaceObject, group and
     *                    action but other policyID.
     * @throws SQLException
     */
    public List<ResourcePolicy> findByTypeGroupActionExceptId(Session session, DSpaceObject dso, Group group,
                                                              int action, int notPolicyID) throws SQLException;

    public List<ResourcePolicy> findByEPersonGroupTypeIdAction(Session session, EPerson e, List<Group> groups,
                                                               int action, int type_id) throws SQLException;

    public void deleteByDso(Session session, DSpaceObject dso) throws SQLException;

    public void deleteByDsoAndAction(Session session, DSpaceObject dso, int actionId) throws SQLException;

    public void deleteByDsoAndType(Session session, DSpaceObject dSpaceObject, String type) throws SQLException;

    public void deleteByGroup(Session session, Group group) throws SQLException;

    public void deleteByDsoGroupPolicies(Session session, DSpaceObject dso, Group group) throws SQLException;

    public void deleteByDsoEPersonPolicies(Session session, DSpaceObject dso, EPerson ePerson) throws SQLException;

    /**
     * Deletes all policies that belong to an EPerson
     *
     * @param session       current request's database context.
     * @param ePerson       ePerson whose policies to delete
     * @throws SQLException if database error
     */
    public void deleteByEPerson(Session session, EPerson ePerson) throws SQLException;

    public void deleteByDsoAndTypeNotEqualsTo(Session session, DSpaceObject o, String type) throws SQLException;

    /**
     * Return a list of policies for an object that match the action except the record labeled with the rpType
     *
     * @param session  current request's database session.
     * @param o        DSpaceObject policies relate to
     * @param actionID action (defined in class Constants)
     * @param rpType   the resource policy type
     * @return list of resource policies
     * @throws SQLException if there's a database problem
     */
    public List<ResourcePolicy> findByDSoAndActionExceptRpType(Session session, DSpaceObject o, int actionID,
            String rpType) throws SQLException;

    /**
     * Return a paginated list of policies that belong to an EPerson
     *
     * @param session       current request's database context
     * @param ePerson       ePerson whose policies want to find
     * @param offset        the position of the first result to return
     * @param limit         paging limit
     * @return              matching policies
     * @throws SQLException if database error
     */
    public List<ResourcePolicy> findByEPerson(Session session, EPerson ePerson, int offset, int limit)
        throws SQLException;

    /**
     * Count all the resource policies of the ePerson
     *
     * @param session        current request's database context
     * @param ePerson        ePerson whose policies want to count
     * @return               total resource policies of the ePerson
     * @throws SQLException  if database error
     */
    public int countByEPerson(Session session, EPerson ePerson) throws SQLException;

    /**
     * Return a paginated list of policies related to a resourceUuid belong to an ePerson
     *
     * @param session        current request's database context
     * @param ePerson        ePerson whose policies want to find
     * @param resourceUuid   the uuid of an DSpace resource
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByEPersonAndResourceUuid(Session session, EPerson ePerson, UUID resourceUuid,
        int offset, int limit) throws SQLException;

    /**
     * Count all the policies related to a resourceUuid belong to an ePerson
     *
     * @param session        current request's database context
     * @param resourceUuid   the uuid of an DSpace resource
     * @param ePerson        ePerson whose policies want to find
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByEPersonAndResourceUuid(Session session, EPerson ePerson, UUID resourceUuid)
        throws SQLException;

    /**
     * Return a paginated list of policies related to a DSpace resource filter by actionId
     *
     * @param session        current request's database context
     * @param resourceUuid   the uuid of an DSpace resource
     * @param actionId       id relative to action as READ, WRITE, DELITE etc.
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByResouceUuidAndActionId(Session session, UUID resourceUuid, int actionId,
        int offset, int limit) throws SQLException;

    /**
     * Count all the policies related to a resourceUuid and actionId
     *
     * @param session        current request's database context
     * @param resourceUuid   the uuid of an DSpace resource
     * @param actionId       id relative to action as READ, WRITE, DELITE etc.
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByResouceUuidAndActionId(Session session, UUID resourceUuid, int actionId)
        throws SQLException;

    /**
     * Return a paginated list of policies related to a DSpace resource
     *
     * @param session        current request's database context
     * @param resourceUuid   the uuid of an DSpace resource
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByResouceUuid(Session session, UUID resourceUuid, int offset, int limit)
        throws SQLException;

    /**
     * Count all the policies by resourceUuid
     *
     * @param session        current request's database context
     * @param resourceUuid   the uuid of an DSpace resource
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByResourceUuid(Session session, UUID resourceUuid) throws SQLException;

    /**
     * Return a paginated list of policies related to a group
     *
     * @param session        current request's database context
     * @param group          DSpace group
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByGroup(Session session, Group group, int offset, int limit)
        throws SQLException;

    /**
     * Count all the resource policies of the group
     *
     * @param session        current request's database context
     * @param group          DSpace group
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countResourcePolicyByGroup(Session session, Group group) throws SQLException;

    /**
     * Return a paginated list of policies related to a group and related to a resourceUuid
     *
     * @param session        current request's database context
     * @param group          DSpace group
     * @param resourceUuid   the uuid of an DSpace resource
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByGroupAndResourceUuid(Session session, Group group, UUID resourceUuid,
        int offset, int limit) throws SQLException;

    /**
     * Count all the resource policies of the group and of the resourceUuid
     *
     * @param session        current request's database context
     * @param group          DSpace group
     * @param resourceUuid   the uuid of an DSpace resource
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByGroupAndResourceUuid(Session session, Group group, UUID resourceUuid) throws SQLException;

    public ResourcePolicy findOneById(Session session, Integer id) throws SQLException;


}
