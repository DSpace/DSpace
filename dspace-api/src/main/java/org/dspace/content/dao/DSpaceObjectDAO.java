/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.DSpaceObject;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the DSpaceObject.
 * All DSpaceObject DAO classes should implement this class since it ensures that the T object is of type DSpaceObject
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> some implementation of DSpaceObject
 */
public interface DSpaceObjectDAO<T extends DSpaceObject> extends GenericDAO<T> {
}
