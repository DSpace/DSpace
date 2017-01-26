package org.dspace.app.rest.model.hateoas;

import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.EvoInflectorRelProvider;

public class DSpaceRelProvider extends EvoInflectorRelProvider {

	@Override
	public String getItemResourceRelFor(Class<?> type) {
		if (type.isAssignableFrom(DSpaceResource.class)) {
			return "bitstream";
		}
		return super.getItemResourceRelFor(type);
	}

}
