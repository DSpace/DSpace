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

    public List<ResourcePolicy> findbyEPerson(Context c, EPerson ePerson, int offset, int limit) throws SQLException;

    public int searchCountEPerson(Context context, EPerson eperson) throws SQLException;

    public List<ResourcePolicy> searchByEPersonAndResourceUuid(Context context, EPerson ePerson, UUID resourceUuid,
            int offset, int limit) throws SQLException;

    public int searchCountByResourceUuid(Context context, UUID resourceUuid, EPerson eperson) throws SQLException;

    public List<ResourcePolicy> searchByResouceUuidAndActionId(Context context, UUID resourceUuid, int actionId,
            int offset, int limit) throws SQLException;

    public int searchCountByResouceAndAction(Context context, UUID resourceUuid, int actionId) throws SQLException;

    public List<ResourcePolicy> searchByResouceUuid(Context context, UUID resourceUuid, int offset, int limit)
            throws SQLException;

    public int searchCountByResourceUuid(Context context, UUID resourceUuid) throws SQLException;

    public List<ResourcePolicy> searchByGroup(Context context, Group group, int offset, int limit) throws SQLException;

    public int searchCountResourcePolicyOfGroup(Context context, Group group) throws SQLException;

    public List<ResourcePolicy> searchByGroupAndResourceUuid(Context context, Group group, UUID resourceUuid,
            int offset, int limit) throws SQLException;

    public int searchCountByGroupAndResourceUuid(Context context, Group group, UUID resourceUuid) throws SQLException;

}
