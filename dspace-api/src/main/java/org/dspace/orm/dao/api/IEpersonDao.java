/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.api;

import org.dspace.orm.entity.Eperson;

public interface IEpersonDao extends IDSpaceDao<Eperson>{

	Eperson selectByEmail(String eperson);
	Eperson getAnonymous();
    
}
