/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.springframework.hateoas.core.EvoInflectorRelProvider;

/**
 * A DSpace Relation Provider that use the RelNameDSpaceResource to use the
 * right names for the embedded collection when a DSpaceResource is requested
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class DSpaceRelProvider extends EvoInflectorRelProvider {

	@Override
	public String getItemResourceRelFor(Class<?> type) {
		if (DSpaceResource.class.isAssignableFrom(type)) {
			return type.getAnnotation(RelNameDSpaceResource.class).value();
		}
		return super.getItemResourceRelFor(type);
	}

}
