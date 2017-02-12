package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CollectionRest;
import org.springframework.stereotype.Component;

@Component
public class CollectionConverter
		extends DSpaceObjectConverter<org.dspace.content.Collection, org.dspace.app.rest.model.CollectionRest> {
	@Override
	public org.dspace.content.Collection toModel(org.dspace.app.rest.model.CollectionRest obj) {
		return (org.dspace.content.Collection) super.toModel(obj);
	}

	@Override
	public CollectionRest fromModel(org.dspace.content.Collection obj) {
		return (CollectionRest) super.fromModel(obj);
	}
	
	@Override
	protected CollectionRest newInstance() {
		return new CollectionRest();
	}
}
