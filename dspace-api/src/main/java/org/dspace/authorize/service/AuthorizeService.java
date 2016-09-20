/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * AuthorizeManager handles all authorization checks for DSpace. For better
 * security, DSpace assumes that you do not have the right to do something
 * unless that permission is spelled out somewhere. That "somewhere" is the
 * ResourcePolicy table. The AuthorizeManager is given a user, an object, and an
 * action, and it then does a lookup in the ResourcePolicy table to see if there
 * are any policies giving the user permission to do that action.
 * <p>
 * ResourcePolicies now apply to single objects (such as submit (ADD) permission
 * to a collection.)
 * <p>
 * Note: If an eperson is a member of the administrator group, then they
 * are automatically given permission for all requests another special group is
 * group with name "Anonymous" - all EPeople are members of this group.
 */
public interface AuthorizeService {

    /**
     * Utility method, checks that the current user of the given context can
     * perform all of the specified actions on the given object. An
     * <code>AuthorizeException</code> if all the authorizations fail.
     *
     * @param c context with the current user
     * @param o DSpace object user is attempting to perform action on
     * @param actions array of action IDs from
     *         <code>org.dspace.core.Constants</code>
     * @throws AuthorizeException if any one of the specified actions cannot be 
     *         performed by the current user on the given object.
     * @throws SQLException if database error
     */
    public void authorizeAnyOf(Context c, DSpaceObject o, int[] actions) throws AuthorizeException, SQLException;

    /**
     * Checks that the context's current user can perform the given action on
     * the given object. Throws an exception if the user is not authorized,
     * otherwise the method call does nothing.
     *
     * @param c context
     * @param o a DSpaceObject
     * @param action action to perform from <code>org.dspace.core.Constants</code>
     * @throws AuthorizeException if the user is denied
     * @throws SQLException if database error
     */
    public void authorizeAction(Context c, DSpaceObject o, int action) throws AuthorizeException, SQLException;

    /**
     * Checks that the context's current user can perform the given action on
     * the given object. Throws an exception if the user is not authorized,
     * otherwise the method call does nothing.
     *
     * @param c context
     * @param o a DSpaceObject
     * @param useInheritance
     *         flag to say if ADMIN action on the current object or parent
     *         object can be used
     * @param action action to perform from <code>org.dspace.core.Constants</code>
     * @throws AuthorizeException if the user is denied
     * @throws SQLException if database error
     */
    public void authorizeAction(Context c, DSpaceObject o, int action, boolean useInheritance)
            throws AuthorizeException, SQLException;

    /**
     * Checks that the specified eperson can perform the given action on
     * the given object. Throws an exception if the user is not authorized,
     * otherwise the method call does nothing.
     *
     * @param c context
     * @param e the eperson to use for the authorization check        
     * @param o a DSpaceObject
     * @param useInheritance
     *         flag to say if ADMIN action on the current object or parent
     *         object can be used
     * @param action action to perform from <code>org.dspace.core.Constants</code>
     * @throws AuthorizeException if the user is denied
     * @throws SQLException if database error
     */
    public void authorizeAction(Context c, EPerson e, DSpaceObject o, int action, boolean useInheritance)
            throws AuthorizeException, SQLException;
    
    /**
     * same authorize, returns boolean for those who don't want to deal with
     * catching exceptions.
     *
     * @param c DSpace context, containing current user
     * @param o DSpaceObject
     * @param a action being attempted, from
     *         <code>org.dspace.core.Constants</code>
     * @return {@code true} if the current user in the context is
     *         authorized to perform the given action on the given object
     * @throws SQLException if database error
     */
    public boolean authorizeActionBoolean(Context c, DSpaceObject o, int a) throws SQLException;

    /**
     * same authorize, returns boolean for those who don't want to deal with
     * catching exceptions.
     *
     * @param c DSpace context, containing current user
     * @param o DSpaceObject
     * @param a action being attempted, from
     *         <code>org.dspace.core.Constants</code>
     * @param useInheritance
     *         flag to say if ADMIN action on the current object or parent
     *         object can be used
     * @return {@code true} if the current user in the context is
     *         authorized to perform the given action on the given object
     * @throws SQLException if database error
     */
    public boolean authorizeActionBoolean(Context c, DSpaceObject o, int a, boolean useInheritance) throws SQLException;
    
    /**
     * same authorize with a specif eperson (not the current user), returns boolean for those who don't want to deal with
     * catching exceptions.
     *
     * @param c DSpace context
     * @param e EPerson to use in the check        
     * @param o DSpaceObject
     * @param a action being attempted, from
     *         <code>org.dspace.core.Constants</code>
     * @param useInheritance
     *         flag to say if ADMIN action on the current object or parent
     *         object can be used
     * @return {@code true} if the requested user is
     *         authorized to perform the given action on the given object
     * @throws SQLException if database error
     */
    public boolean authorizeActionBoolean(Context c, EPerson e, DSpaceObject o, int a, boolean useInheritance) throws SQLException;

    ///////////////////////////////////////////////
    // admin check methods
    ///////////////////////////////////////////////

    /**
     * Check to see if the current user is an Administrator of a given object
     * within DSpace. Always return {@code true} if the user is a System
     * Admin
     *
     * @param c current context
     * @param o current DSpace Object, if <code>null</code> the call will be
     *         equivalent to a call to the <code>isAdmin(Context c)</code>
     *         method
     * @return {@code true} if user has administrative privileges on the
     *         given DSpace object
     * @throws SQLException if database error
     */
    public boolean isAdmin(Context c, DSpaceObject o) throws SQLException;


    /**
     * Check to see if the current user is a System Admin. Always return
     * {@code true} if c.ignoreAuthorization is set. Anonymous users
     * can't be Admins (EPerson set to NULL)
     *
     * @param c current context
     * @return {@code true} if user is an admin or ignore authorization
     *         flag set
     * @throws SQLException if database error
     */
    public boolean isAdmin(Context c) throws SQLException;
    
    public boolean isCommunityAdmin(Context c) throws SQLException;
    
    public boolean isCollectionAdmin(Context c) throws SQLException; 

    ///////////////////////////////////////////////
    // policy manipulation methods
    ///////////////////////////////////////////////

    /**
     * Add a policy for an individual eperson
     *
     * @param c context. Current user irrelevant
     * @param o DSpaceObject to add policy to
     * @param actionID ID of action from <code>org.dspace.core.Constants</code>
     * @param e eperson who can perform the action
     * @throws SQLException if database error
     * @throws AuthorizeException if current user in context is not authorized to add policies
     */
    public void addPolicy(Context c, DSpaceObject o, int actionID, EPerson e) throws SQLException, AuthorizeException;


    /**
     * Add a policy for an individual eperson
     *
     * @param c context. Current user irrelevant
     * @param o DSpaceObject to add policy to
     * @param actionID ID of action from <code>org.dspace.core.Constants</code>
     * @param e eperson who can perform the action
     * @param type policy type, deafult types are declared in the ResourcePolicy class
     * @throws SQLException if database error
     * @throws AuthorizeException if current user in context is not authorized to add policies
     */
    public void addPolicy(Context c, DSpaceObject o, int actionID, EPerson e, String type) throws SQLException, AuthorizeException;

    /**
     * Add a policy for a group
     *
     * @param c current context
     * @param o object to add policy for
     * @param actionID ID of action from <code>org.dspace.core.Constants</code>
     * @param g group to add policy for
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if the current user is not authorized to add this policy
     */
    public void addPolicy(Context c, DSpaceObject o, int actionID, Group g) throws SQLException, AuthorizeException;

    /**
     * Add a policy for a group
     *
     * @param c current context
     * @param o object to add policy for
     * @param actionID ID of action from <code>org.dspace.core.Constants</code>
     * @param g group to add policy for
     * @param type policy type, deafult types are declared in the ResourcePolicy class
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if the current user is not authorized to add this policy
     */
    public void addPolicy(Context c, DSpaceObject o, int actionID, Group g, String type) throws SQLException, AuthorizeException;

    /**
     * Return a List of the policies for an object
     *
     * @param c current context
     * @param o object to retrieve policies for
     * @return List of {@code ResourcePolicy} objects
     * @throws SQLException if database error
     */
    public List<ResourcePolicy> getPolicies(Context c, DSpaceObject o) throws SQLException;

    /**
     * Return a List of the policies for an object
     *
     * @param c  current context
     * @param o object to retrieve policies for
     * @param type type
     * @return List of {@code ResourcePolicy} objects
     * @throws SQLException if database error
     */
    public List<ResourcePolicy> findPoliciesByDSOAndType(Context c, DSpaceObject o, String type) throws SQLException;

    /**
     * Return a List of the policies for a group
     *
     * @param c current context
     * @param g group to retrieve policies for
     * @return List of {@code ResourcePolicy} objects
     * @throws SQLException if database error
     */
    public List<ResourcePolicy> getPoliciesForGroup(Context c, Group g) throws SQLException;

    /**
     * Return a list of policies for an object that match the action
     *
     * @param c context
     * @param o DSpaceObject policies relate to
     * @param actionID action (defined in class Constants)
     * @return list of resource policies
     * @throws SQLException if there's a database problem
     */
    public List<ResourcePolicy> getPoliciesActionFilter(Context c, DSpaceObject o, int actionID) throws SQLException;

    /**
     * Add policies to an object to match those from a previous object
     *
     * @param c context
     * @param src source of policies
     * @param dest destination of inherited policies
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if the current user is not authorized to add these policies
     */
    public void inheritPolicies(Context c, DSpaceObject src, DSpaceObject dest) throws SQLException, AuthorizeException;

    /**
     * Copies policies from a list of resource policies to a given DSpaceObject
     *
     * @param c DSpace context
     * @param policies List of ResourcePolicy objects
     * @param dest object to have policies added
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if the current user is not authorized to add these policies
     */
    public void addPolicies(Context c, List<ResourcePolicy> policies, DSpaceObject dest) throws SQLException, AuthorizeException;

    /**
     * removes ALL policies for an object.  FIXME doesn't check authorization
     *
     * @param c DSpace context
     * @param o object to remove policies for
     * @throws SQLException  if there's a database problem
     * @throws AuthorizeException if authorization error
     */
    public void removeAllPolicies(Context c, DSpaceObject o) throws SQLException, AuthorizeException;

    /**
     * removes ALL policies for an object that are not of the input type.
     *
     * @param c DSpace context
     * @param o object to remove policies for
     * @param type type
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if authorization error
     */
    public void removeAllPoliciesByDSOAndTypeNotEqualsTo(Context c, DSpaceObject o, String type) throws SQLException, AuthorizeException;

    /**
     * removes policies
     *
     * @param c DSpace context
     * @param o object to remove policies for
     * @param type policy type
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if authorization error
     */
    public void removeAllPoliciesByDSOAndType(Context c, DSpaceObject o, String type) throws SQLException, AuthorizeException;

    /**
     * Remove all policies from an object that match a given action. FIXME
     * doesn't check authorization
     *
     * @param context current context
     * @param dso object to remove policies from
     * @param actionID ID of action to match from
     *         {@link org.dspace.core.Constants#Constants Constants}, or -1=all
     * @throws SQLException  if there's a database problem
     * @throws AuthorizeException if authorization error
     */
    public void removePoliciesActionFilter(Context context, DSpaceObject dso, int actionID) throws SQLException, AuthorizeException;

    /**
     * Removes all policies relating to a particular group. FIXME doesn't check
     * authorization
     *
     * @param c current context
     * @param group the group
     * @throws SQLException if there's a database problem
     */
    public void removeGroupPolicies(Context c, Group group) throws SQLException;

    /**
     * Removes all policies from a group for a particular object that belong to
     * a Group. FIXME doesn't check authorization
     *
     * @param c current context
     * @param o the object
     * @param g the group
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if authorization error
     */
    public void removeGroupPolicies(Context c, DSpaceObject o, Group g) throws SQLException, AuthorizeException;

    /**
     * Removes all policies from an eperson for a particular object that belong to
     * an EPerson. FIXME doesn't check authorization
     *
     * @param c current context
     * @param o the object
     * @param e the eperson
     * @throws SQLException if there's a database problem
     * @throws AuthorizeException if authorization error
     */
    public void removeEPersonPolicies(Context c, DSpaceObject o, EPerson e) throws SQLException, AuthorizeException;

    /**
     * Returns all groups authorized to perform an action on an object. Returns
     * empty array if no matches.
     *
     * @param c current context
     * @param o object
     * @param actionID ID of action from {@link org.dspace.core.Constants#Constants Constants}
     * @return array of {@link org.dspace.eperson.Group#Group Groups} that can perform the specified
     *         action on the specified object
     * @throws SQLException if there's a database problem
     */
    public List<Group> getAuthorizedGroups(Context c, DSpaceObject o, int actionID) throws java.sql.SQLException;

    public boolean isAnIdenticalPolicyAlreadyInPlace(Context c, DSpaceObject o, ResourcePolicy rp) throws SQLException;


    /**
     * Is a policy with the specified parameters already in place?
     *
     * @param c current context
     * @param o object
     * @param group group
     * @param actionID ID of action from {@link org.dspace.core.Constants#Constants Constants}
     * @param policyID ID of an existing policy. If -1 is specified, this parameter will be ignored
     * @return true if such a policy exists, false otherwise
     * @throws SQLException if there's a database problem
     */
    public boolean isAnIdenticalPolicyAlreadyInPlace(Context c, DSpaceObject o, Group group, int actionID, int policyID) throws SQLException;

    public ResourcePolicy findByTypeIdGroupAction(Context c, DSpaceObject dso, Group group, int action, int policyID) throws SQLException;


    /**
     * Generate Policies policies READ for the date in input adding reason. New policies are assigned automatically at the groups that
     * have right on the collection. E.g., if the anonymous can access the collection policies are assigned to anonymous.
     *
     * @param context current context
     * @param embargoDate date
     * @param reason reason
     * @param dso DSpaceObject
     * @param owningCollection collection
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void generateAutomaticPolicies(Context context, Date embargoDate, String reason, DSpaceObject dso, Collection owningCollection) throws SQLException, AuthorizeException;

    public ResourcePolicy createResourcePolicy(Context context, DSpaceObject dso, Group group, EPerson eperson, int type, String rpType) throws SQLException, AuthorizeException;

    public ResourcePolicy createOrModifyPolicy(ResourcePolicy policy, Context context, String name, Group group, EPerson ePerson, Date embargoDate, int action, String reason, DSpaceObject dso) throws AuthorizeException, SQLException;

	/**
	 * Change all the policies related to the action (fromPolicy) of the
	 * specified object to the new action (toPolicy)
	 * 
	 * @param context
	 * @param dso
	 *            the dspace object
	 * @param fromAction
	 *            the action to change
	 * @param toAction
	 *            the new action to set
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	void switchPoliciesAction(Context context, DSpaceObject dso, int fromAction, int toAction)
			throws SQLException, AuthorizeException;

}
