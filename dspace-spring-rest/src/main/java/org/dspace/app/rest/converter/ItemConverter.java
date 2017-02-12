package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ItemRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemConverter extends DSpaceObjectConverter<org.dspace.content.Item, org.dspace.app.rest.model.ItemRest>{
	@Autowired
	private CollectionConverter collectionConverter;
	
	@Override
	public ItemRest fromModel(org.dspace.content.Item obj) {
		ItemRest item = super.fromModel(obj);
//		item.setOwningCollection(collectionConverter.fromModel(obj.getOwningCollection()));
		return item;
	}

	@Override
	public org.dspace.content.Item toModel(ItemRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ItemRest newInstance() {
		return new ItemRest();
	}
}
