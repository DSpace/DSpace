/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.api;

import java.util.List;

import org.dspace.orm.entity.IDSpaceObject;
import org.dspace.orm.entity.ResourcePolicy;
import org.dspace.services.auth.Action;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

public interface IResourcePolicyDao extends IDSpaceDao<ResourcePolicy>{
	List<ResourcePolicy> selectByObjectAndAction(IDSpaceObject dSpaceObject, Action admin);
}
