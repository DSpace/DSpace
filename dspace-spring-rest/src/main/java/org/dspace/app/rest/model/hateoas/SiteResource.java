/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.CommunityResourceController;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Item Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RelNameDSpaceResource(SiteRest.NAME)
public class SiteResource extends DSpaceResource<SiteRest> {
	public SiteResource(SiteRest site, Utils utils, String... rels) {
		super(site, utils, rels);
		super.add(linkTo(CommunityResourceController.class, "/tops").withSelfRel());
	}
}