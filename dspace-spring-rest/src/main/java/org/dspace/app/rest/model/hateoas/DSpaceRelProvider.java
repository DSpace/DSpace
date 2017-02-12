package org.dspace.app.rest.model.hateoas;

import org.springframework.hateoas.core.EvoInflectorRelProvider;

public class DSpaceRelProvider extends EvoInflectorRelProvider {

	@Override
	public String getItemResourceRelFor(Class<?> type) {
		if (DSpaceResource.class.isAssignableFrom(type)) {
			return type.getAnnotation(RelNameDSpaceResource.class).value();
		}
		return super.getItemResourceRelFor(type);
	}

}
