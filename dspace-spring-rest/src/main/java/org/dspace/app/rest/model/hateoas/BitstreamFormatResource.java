/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * BitstreamFormat Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RelNameDSpaceResource(BitstreamFormatRest.NAME)
public class BitstreamFormatResource extends DSpaceResource<BitstreamFormatRest> {
	public BitstreamFormatResource(BitstreamFormatRest bf, Utils utils, String... rels) {
		super(bf, utils, rels);
	}
}
