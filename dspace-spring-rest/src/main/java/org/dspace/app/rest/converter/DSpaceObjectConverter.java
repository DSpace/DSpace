package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.springframework.core.convert.converter.Converter;

public abstract class DSpaceObjectConverter <M extends DSpaceObject, R extends org.dspace.app.rest.model.DSpaceRestObject> extends DSpaceConverter<M, R> implements Converter<M, R> {	
	@Override
	public R convert(M source) {
		return fromModel(source);
	}
	
	@Override
	public R fromModel(M obj) {
		R resource = newInstance();
		resource.setHandle(obj.getHandle());
		if (obj.getID() != null) {
			resource.setUuid(obj.getID().toString());
		}
		resource.setName(obj.getName());
		List<MetadataEntryRest> metadata = new ArrayList<MetadataEntryRest>();
		for (MetadataValue mv : obj.getMetadata()) {
			MetadataEntryRest me = new MetadataEntryRest();
			me.setKey(mv.getMetadataField().toString('.'));
			me.setValue(mv.getValue());
			me.setLanguage(mv.getLanguage());
			metadata.add(me);
		}
		resource.setMetadata(metadata);
		return resource;
	}
	
	@Override
	public M toModel(R obj) {
		return null;
	}

	protected abstract R newInstance();
}