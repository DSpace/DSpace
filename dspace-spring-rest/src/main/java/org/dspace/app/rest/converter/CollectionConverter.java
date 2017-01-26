package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CollectionRestResource;
import org.springframework.stereotype.Component;

@Component
public class CollectionConverter
		extends DSpaceObjectConverter<org.dspace.content.Collection, org.dspace.app.rest.model.CollectionRestResource> {
	@Override
	public org.dspace.content.Collection toModel(org.dspace.app.rest.model.CollectionRestResource obj) {
		return (org.dspace.content.Collection) super.toModel(obj);
	}

	@Override
	public CollectionRestResource fromModel(org.dspace.content.Collection obj) {
		return (CollectionRestResource) super.fromModel(obj);
	}
	
	@Override
	protected CollectionRestResource newInstance() {
		return new CollectionRestResource();
	}
}
