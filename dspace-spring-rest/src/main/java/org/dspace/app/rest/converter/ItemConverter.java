package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ItemRestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemConverter extends DSpaceObjectConverter<org.dspace.content.Item, org.dspace.app.rest.model.ItemRestResource>{
	@Autowired
	private CollectionConverter collectionConverter;
	
	@Override
	public ItemRestResource fromModel(org.dspace.content.Item obj) {
		ItemRestResource item = super.fromModel(obj);
//		item.setOwningCollection(collectionConverter.fromModel(obj.getOwningCollection()));
		return item;
	}

	@Override
	public org.dspace.content.Item toModel(ItemRestResource obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ItemRestResource newInstance() {
		return new ItemRestResource();
	}
}
