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
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Database Access Object interface class for the ResourcePolicy object.
 * The implementation of this class is responsible for all database calls for the ResourcePolicy object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ResourcePolicyDAO extends GenericDAO<ResourcePolicy> {

    public List<ResourcePolicy> findByDso(Context context, DSpaceObject dso) throws SQLException;

    public List<ResourcePolicy> findByDsoAndType(Context context, DSpaceObject dSpaceObject, String type)
        throws SQLException;

    public List<ResourcePolicy> findByGroup(Context context, Group group) throws SQLException;

    public List<ResourcePolicy> findByDSoAndAction(Context context, DSpaceObject dso, int actionId) throws SQLException;

    public List<ResourcePolicy> findByTypeGroupAction(Context context, DSpaceObject dso, Group group, int action)
        throws SQLException;

    /**
     * Look for ResourcePolicies by DSpaceObject, Group, and action, ignoring IDs with a specific PolicyID.
     * This method can be used to detect duplicate ResourcePolicies.
     *
     * @param notPolicyID ResourcePolicies with this ID will be ignored while looking out for equal ResourcePolicies.
     * @return List of resource policies for the same DSpaceObject, group and action but other policyID.
     * @throws SQLException
     */
    public List<ResourcePolicy> findByTypeGroupActionExceptId(Context context, DSpaceObject dso, Group group,
                                                              int action, int notPolicyID) throws SQLException;

    public List<ResourcePolicy> findByEPersonGroupTypeIdAction(Context context, EPerson e, List<Group> groups,
                                                               int action, int type_id) throws SQLException;

    public void deleteByDso(Context context, DSpaceObject dso) throws SQLException;

    public void deleteByDsoAndAction(Context context, DSpaceObject dso, int actionId) throws SQLException;

    public void deleteByDsoAndType(Context context, DSpaceObject dSpaceObject, String type) throws SQLException;

    public void deleteByGroup(Context context, Group group) throws SQLException;

    public void deleteByDsoGroupPolicies(Context context, DSpaceObject dso, Group group) throws SQLException;

    public void deleteByDsoEPersonPolicies(Context context, DSpaceObject dso, EPerson ePerson) throws SQLException;

    public void deleteByDsoAndTypeNotEqualsTo(Context c, DSpaceObject o, String type) throws SQLException;

    /**
     * Return a list of policies for an object that match the action except the record labeled with the rpType
     *
     * @param c        context
     * @param o        DSpaceObject policies relate to
     * @param actionID action (defined in class Constants)
     * @param rpType   the resource policy type
     * @return list of resource policies
     * @throws SQLException if there's a database problem
     */
    public List<ResourcePolicy> findByDSoAndActionExceptRpType(Context c, DSpaceObject o, int actionID,
            String rpType) throws SQLException;

    /**
     * Return a paginated list of policies that belong to an EPerson
     * 
     * @param context       DSpace context object
     * @param ePerson       ePerson whose policies want to find
     * @param offset        the position of the first result to return
     * @param limit         paging limit
     * @throws SQLException if database error
     */
    public List<ResourcePolicy> findByEPerson(Context context, EPerson ePerson, int offset, int limit)
        throws SQLException;

    /**
     * Count all the resource policies of the ePerson
     * 
     * @param context        DSpace context object
     * @param ePerson        ePerson whose policies want to count
     * @return               total resource policies of the ePerson
     * @throws SQLException  if database error
     */
    public int countByEPerson(Context context, EPerson eperson) throws SQLException;

    /**
     * Return a paginated list of policies related to a resourceUuid belong to an ePerson
     * 
     * @param context        DSpace context object
     * @param ePerson        ePerson whose policies want to find
     * @param resourceUuid   the uuid of an DSpace resource
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByEPersonAndResourceUuid(Context context, EPerson ePerson, UUID resourceUuid,
        int offset, int limit) throws SQLException;

    /**
     * Count all the policies related to a resourceUuid belong to an ePerson
     * 
     * @param context        DSpace context object
     * @param resourceUuid   the uuid of an DSpace resource
     * @param ePerson        ePerson whose policies want to find
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByEPersonAndResourceUuid(Context context, EPerson ePerson, UUID resourceUuid)
        throws SQLException;

    /**
     * Return a paginated list of policies related to a DSpace resource filter by actionId
     * 
     * @param context        DSpace context object
     * @param resourceUuid   the uuid of an DSpace resource
     * @param actionId       id relative to action as READ, WRITE, DELITE etc.
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByResouceUuidAndActionId(Context context, UUID resourceUuid, int actionId,
        int offset, int limit) throws SQLException;

    /**
     * Count all the policies related to a resourceUuid and actionId
     * 
     * @param context        DSpace context object
     * @param resourceUuid   the uuid of an DSpace resource
     * @param actionId       id relative to action as READ, WRITE, DELITE etc.
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByResouceUuidAndActionId(Context context, UUID resourceUuid, int actionId)
        throws SQLException;

    /**
     * Return a paginated list of policies related to a DSpace resource
     * 
     * @param context        DSpace context object
     * @param resourceUuid   the uuid of an DSpace resource
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByResouceUuid(Context context, UUID resourceUuid, int offset, int limit)
        throws SQLException;

    /**
     * Count all the policies by resourceUuid
     * 
     * @param context        DSpace context object
     * @param resourceUuid   the uuid of an DSpace resource
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByResourceUuid(Context context, UUID resourceUuid) throws SQLException;

    /**
     * Return a paginated list of policies related to a group
     * 
     * @param context        DSpace context object
     * @param group          DSpace group
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByGroup(Context context, Group group, int offset, int limit)
        throws SQLException;

    /**
     * Count all the resource policies of the group
     * 
     * @param context        DSpace context object
     * @param group          DSpace group
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countResourcePolicyByGroup(Context context, Group group) throws SQLException;

    /**
     * Return a paginated list of policies related to a group and related to a resourceUuid
     * 
     * @param context        DSpace context object
     * @param group          DSpace group
     * @param resourceUuid   the uuid of an DSpace resource
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return               list of resource policies
     * @throws SQLException  if database error
     */
    public List<ResourcePolicy> findByGroupAndResourceUuid(Context context, Group group, UUID resourceUuid,
        int offset, int limit) throws SQLException;

    /**
     * Count all the resource policies of the group and of the resourceUuid
     * 
     * @param context        DSpace context object
     * @param group          DSpace group
     * @param resourceUuid   the uuid of an DSpace resource
     * @return               total policies
     * @throws SQLException  if database error
     */
    public int countByGroupAndResourceUuid(Context context, Group group, UUID resourceUuid) throws SQLException;

    public ResourcePolicy findOneById(Context context, Integer id) throws SQLException;


}
