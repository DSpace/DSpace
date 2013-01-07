/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import org.dspace.services.auth.Action;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 *
 */
public interface IDSpaceObject {
	/**
	 * The database ID of the object.
	 * 
	 * @return
	 */
	int getID();
	/**
	 * The type of the object
	 * 
	 * @return
	 */
    DSpaceObjectType getType();
    /**
     * Method for the authorization purposes. It must return the
     * admin object this very specific DSpace Object. For example,
     * it could be the parent Community of a Collection.
     * 
     * @param action Action
     * @return
     */
    IDSpaceObject getAdminObject(Action action);
    /**
     * Parent object
     * 
     * @return
     */
    IDSpaceObject getParentObject ();
    
    /**
     * Tell's if the eperson is the admin of this object.
     * 
     * @param e Eperson to test
     * @return is admin of this object
     */
    boolean isAdmin (Eperson e);
}
