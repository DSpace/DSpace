package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ItemRest;

@RelNameDSpaceResource(ItemRest.NAME)
public class ItemResource extends DSpaceResource<ItemRest> {
	public ItemResource(ItemRest item) {
		super(item);
	}
}
