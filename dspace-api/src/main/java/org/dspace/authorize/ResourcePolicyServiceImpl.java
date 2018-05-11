/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation for the ResourcePolicy object.
 * This class is responsible for all business logic calls for the ResourcePolicy object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ResourcePolicyServiceImpl implements ResourcePolicyService
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ResourcePolicyServiceImpl.class);

    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;

    @Autowired(required = true)
    protected ResourcePolicyDAO resourcePolicyDAO;

    protected ResourcePolicyServiceImpl()
    {
    }

    /**
     * Get an ResourcePolicy from the database.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the ResourcePolicy
     *
     * @return the ResourcePolicy format, or null if the ID is invalid.
     * @throws SQLException if database error
     */
    @Override
    public ResourcePolicy find(Context context, int id) throws SQLException
    {
        return resourcePolicyDAO.findByID(context, ResourcePolicy.class, id);
    }

    /**
     * Create a new ResourcePolicy
     *
     * @param context
     *            DSpace context object
     * @return ResourcePolicy
     * @throws SQLException if database error
     */
    @Override
    public ResourcePolicy create(Context context) throws SQLException
    {
        // FIXME: Check authorisation
        // Create a table row
        ResourcePolicy resourcePolicy = resourcePolicyDAO.create(context, new ResourcePolicy());
        return resourcePolicy;
    }

    @Override
    public List<ResourcePolicy> find(Context c, DSpaceObject o) throws SQLException
    {
        return resourcePolicyDAO.findByDso(c, o);
    }


    @Override
    public List<ResourcePolicy> find(Context c, DSpaceObject o, String type) throws SQLException
    {
        return resourcePolicyDAO.findByDsoAndType(c, o, type);
    }

    @Override
    public List<ResourcePolicy> find(Context context, Group group) throws SQLException {
        return resourcePolicyDAO.findByGroup(context, group);
    }

    @Override
    public List<ResourcePolicy> find(Context c, DSpaceObject o, int actionId) throws SQLException
    {
        return resourcePolicyDAO.findByDSoAndAction(c, o, actionId);
    }

    @Override
    public List<ResourcePolicy> find(Context c, DSpaceObject dso, Group group, int action) throws SQLException {
        return resourcePolicyDAO.findByTypeGroupAction(c, dso, group, action);
    }
    
    @Override
    public List<ResourcePolicy> find(Context c, EPerson e, List<Group> groups, int action, int type_id) throws SQLException{
        return resourcePolicyDAO.findByEPersonGroupTypeIdAction(c, e, groups, action, type_id);
    }
    
    @Override
    public List<ResourcePolicy> findByTypeGroupActionExceptId(Context context, DSpaceObject dso, Group group, int action, int notPolicyID)
            throws SQLException
    {
        return resourcePolicyDAO.findByTypeGroupActionExceptId(context, dso, group, action, notPolicyID);
    }
            

    /**
     * Delete an ResourcePolicy
     *
     * @param context context
     * @param resourcePolicy resource policy
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void delete(Context context, ResourcePolicy resourcePolicy) throws SQLException, AuthorizeException {
        // FIXME: authorizations
        // Remove ourself
        resourcePolicyDAO.delete(context, resourcePolicy);
        
        context.turnOffAuthorisationSystem();
        if(resourcePolicy.getdSpaceObject() != null)
        {
            //A policy for a DSpace Object has been modified, fire a modify event on the DSpace object
            contentServiceFactory.getDSpaceObjectService(resourcePolicy.getdSpaceObject()).updateLastModified(context, resourcePolicy.getdSpaceObject());
        }
        context.restoreAuthSystemState();
    }


    /**
     * @param resourcePolicy resource policy
     * @return action text or 'null' if action row empty
     */
    @Override
    public String getActionText(ResourcePolicy resourcePolicy)
    {
        int myAction = resourcePolicy.getAction();

        if (myAction == -1)
        {
            return "...";
        }
        else
        {
            return Constants.actionText[myAction];
        }
    }
    /**
     * figures out if the date is valid for the policy
     *
     * @param resourcePolicy resource policy
     * @return true if policy has begun and hasn't expired yet (or no dates are
     *         set)
     */
    @Override
    public boolean isDateValid(ResourcePolicy resourcePolicy)
    {
        Date sd = resourcePolicy.getStartDate();
        Date ed = resourcePolicy.getEndDate();

        // if no dates set, return true (most common case)
        if ((sd == null) && (ed == null))
        {
            return true;
        }

        // one is set, now need to do some date math
        Date now = new Date();

        // check start date first
        if (sd != null && now.before(sd))
        {
            // start date is set, return false if we're before it
            return false;
        }

        // now expiration date
        if (ed != null && now.after(ed))
        {
            // end date is set, return false if we're after it
            return false;
        }

        // if we made it this far, start < now < end
        return true; // date must be okay
    }

    @Override
    public ResourcePolicy clone(Context context, ResourcePolicy resourcePolicy) throws SQLException, AuthorizeException {
        ResourcePolicy clone = create(context);
        clone.setGroup(resourcePolicy.getGroup());
        clone.setEPerson(resourcePolicy.getEPerson());
        clone.setStartDate((Date) ObjectUtils.clone(resourcePolicy.getStartDate()));
        clone.setEndDate((Date) ObjectUtils.clone(resourcePolicy.getEndDate()));
        clone.setRpType((String) ObjectUtils.clone(resourcePolicy.getRpType()));
        clone.setRpDescription((String) ObjectUtils.clone(resourcePolicy.getRpDescription()));
        update(context, clone);
        return clone;
    }

    @Override
    public void removeAllPolicies(Context c, DSpaceObject o) throws SQLException, AuthorizeException {
        resourcePolicyDAO.deleteByDso(c, o);
        c.turnOffAuthorisationSystem();
        contentServiceFactory.getDSpaceObjectService(o).updateLastModified(c, o);
        c.restoreAuthSystemState();
    }

    @Override
    public void removePolicies(Context c, DSpaceObject o, String type) throws SQLException, AuthorizeException {
        resourcePolicyDAO.deleteByDsoAndType(c, o, type);
        c.turnOffAuthorisationSystem();
        contentServiceFactory.getDSpaceObjectService(o).updateLastModified(c, o);
        c.restoreAuthSystemState();
    }

    @Override
    public void removeDsoGroupPolicies(Context context, DSpaceObject dso, Group group) throws SQLException, AuthorizeException {
        resourcePolicyDAO.deleteByDsoGroupPolicies(context, dso, group);
        context.turnOffAuthorisationSystem();
        contentServiceFactory.getDSpaceObjectService(dso).updateLastModified(context, dso);
        context.restoreAuthSystemState();
    }

    @Override
    public void removeDsoEPersonPolicies(Context context, DSpaceObject dso, EPerson ePerson) throws SQLException, AuthorizeException {
        resourcePolicyDAO.deleteByDsoEPersonPolicies(context, dso, ePerson);
        context.turnOffAuthorisationSystem();
        contentServiceFactory.getDSpaceObjectService(dso).updateLastModified(context, dso);
        context.restoreAuthSystemState();

    }

    @Override
    public void removeGroupPolicies(Context c, Group group) throws SQLException {
        resourcePolicyDAO.deleteByGroup(c, group);
    }

    @Override
    public void removePolicies(Context c, DSpaceObject o, int actionId) throws SQLException, AuthorizeException {
        if (actionId == -1)
        {
            removeAllPolicies(c, o);
        }else{
            resourcePolicyDAO.deleteByDsoAndAction(c, o, actionId);
            c.turnOffAuthorisationSystem();
            contentServiceFactory.getDSpaceObjectService(o).updateLastModified(c, o);
            c.restoreAuthSystemState();
        }
    }

    @Override
    public void removeDsoAndTypeNotEqualsToPolicies(Context c, DSpaceObject o, String type) throws SQLException, AuthorizeException {
        resourcePolicyDAO.deleteByDsoAndTypeNotEqualsTo(c, o, type);
        c.turnOffAuthorisationSystem();
        contentServiceFactory.getDSpaceObjectService(o).updateLastModified(c, o);
        c.restoreAuthSystemState();
    }


    /**
     * Update the ResourcePolicy
     * @param context context
     * @param resourcePolicy resource policy
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void update(Context context, ResourcePolicy resourcePolicy) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(resourcePolicy));
    }

    /**
     * Update the ResourcePolicies
     */
    @Override
    public void update(Context context, List<ResourcePolicy> resourcePolicies) throws SQLException, AuthorizeException {
        if(CollectionUtils.isNotEmpty(resourcePolicies)) {
            Set<DSpaceObject> relatedDSpaceObjects = new HashSet<>();

            for (ResourcePolicy resourcePolicy : resourcePolicies) {
                if (resourcePolicy.getdSpaceObject() != null) {
                    relatedDSpaceObjects.add(resourcePolicy.getdSpaceObject());
                }

                // FIXME: Check authorisation
                resourcePolicyDAO.save(context, resourcePolicy);
            }

            //Update the last modified timestamp of all related DSpace Objects
            context.turnOffAuthorisationSystem();
            for (DSpaceObject dSpaceObject : relatedDSpaceObjects) {
                //A policy for a DSpace Object has been modified, fire a modify event on the DSpace object
            	contentServiceFactory.getDSpaceObjectService(dSpaceObject).updateLastModified(context, dSpaceObject);
            }
            context.restoreAuthSystemState();
        }
    }
}
