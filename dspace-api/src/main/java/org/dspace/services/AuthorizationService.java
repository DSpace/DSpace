/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.services;

import org.dspace.orm.entity.IDSpaceObject;
import org.dspace.services.auth.AuthorizationException;

/**
 * AuthorizationService handles all authorization checks for DSpace. For better
 * security, DSpace assumes that you do not have the right to do something
 * unless that permission is spelled out somewhere. That "somewhere" is the
 * ResourcePolicy table. The AuthorizationService is given a user, an object, and an
 * action, and it then does a lookup in the ResourcePolicy table to see if there
 * are any policies giving the user permission to do that action.
 * <p>
 * ResourcePolicies now apply to single objects (such as submit (ADD) permission
 * to a collection.)
 * <p>
 * Note: If an eperson is a member of the administrator group (id 1), then they
 * are automatically given permission for all requests another special group is
 * group 0, which is anonymous - all Epeople are members of group 0.
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public interface AuthorizationService {
	/**
     * Utility method, checks that the current user of the given context can
     * perform all of the specified actions on the given object. An
     * <code>AuthorizeException</code> if all the authorizations fail.
     * 
	 * @param dspaceObject DSpace object user is attempting to perform action on
	 * @param actions array of action IDs from
     *         <code>org.dspace.core.Constants</code>
	 * @throws AuthorizationServiceException if something goes wrong
	 */
	void authorizedAnyOf (IDSpaceObject dspaceObject, int[] actions) throws AuthorizationException;
	/**
     * Checks that the context's current user can perform the given action on
     * the given object. Throws an exception if the user is not authorized,
     * otherwise the method call does nothing.
     * 
	 * @param object DSpaceObject
	 * @param action Action to check
	 * @throws AuthorizationServiceException if something goes wrong
	 */
	void authorized (IDSpaceObject object, int action) throws AuthorizationException;
	/**
     * Checks that the context's current user can perform the given action on
     * the given object. Throws an exception if the user is not authorized,
     * otherwise the method call does nothing.
	 * 
	 * @param object DSpaceObject
	 * @param action Action to check
	 * @param inheritance flag to say if ADMIN action on the current object or parent
     *         object can be used
	 * @throws AuthorizationServiceException if something goes wrong
	 */
	void authorized (IDSpaceObject object, int action, boolean inheritance) throws AuthorizationException;
}
