/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.EPersonGroupRest;
import org.dspace.app.rest.utils.Utils;

/**
 * EPerson Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RelNameDSpaceResource(EPersonGroupRest.NAME)
public class EPersonGroupResource extends DSpaceResource<EPersonGroupRest> {
	public EPersonGroupResource(EPersonGroupRest group, Utils utils, String... rels) {
		super(group, utils, rels);
	}
}
