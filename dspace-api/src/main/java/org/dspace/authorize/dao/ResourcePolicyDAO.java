/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.dao;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the ResourcePolicy object.
 * The implementation of this class is responsible for all database calls for the ResourcePolicy object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ResourcePolicyDAO extends GenericDAO<ResourcePolicy> {

    public List<ResourcePolicy> findByDso(Context context, DSpaceObject dso) throws SQLException;

    public List<ResourcePolicy> findByDsoAndType(Context context, DSpaceObject dSpaceObject, String type) throws SQLException;

    public List<ResourcePolicy> findByGroup(Context context, Group group) throws SQLException;

    public List<ResourcePolicy> findByDSoAndAction(Context context, DSpaceObject dso, int actionId) throws SQLException;

    public List<ResourcePolicy> findByTypeGroupAction(Context context, DSpaceObject dso, Group group, int action) throws SQLException;
    
    /**
     * Look for ResourcePolicies by DSpaceObject, Group, and action, ignoring IDs with a specific PolicyID.
     * This method can be used to detect duplicate ResourcePolicies.
     * @param notPolicyID ResourcePolicies with this ID will be ignored while looking out for equal ResourcePolicies.
     * @return List of resource policies for the same DSpaceObject, group and action but other policyID.
     * @throws SQLException 
     */
    public List<ResourcePolicy> findByTypeGroupActionExceptId(Context context, DSpaceObject dso, Group group, int action, int notPolicyID) throws SQLException;
    
    public List<ResourcePolicy> findByEPersonGroupTypeIdAction(Context context, EPerson e, List<Group> groups, int action, int type_id) throws SQLException;

    public void deleteByDso(Context context, DSpaceObject dso) throws SQLException;

    public void deleteByDsoAndAction(Context context, DSpaceObject dso, int actionId) throws SQLException;

    public void deleteByDsoAndType(Context context, DSpaceObject dSpaceObject, String type) throws SQLException;

    public void deleteByGroup(Context context, Group group) throws SQLException;

    public void deleteByDsoGroupPolicies(Context context, DSpaceObject dso, Group group) throws SQLException;

    public void deleteByDsoEPersonPolicies(Context context, DSpaceObject dso, EPerson ePerson) throws SQLException;

    public void deleteByDsoAndTypeNotEqualsTo(Context c, DSpaceObject o, String type) throws SQLException;
}
