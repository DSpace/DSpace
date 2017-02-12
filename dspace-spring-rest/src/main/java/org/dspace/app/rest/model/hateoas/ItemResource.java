package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(ItemRest.NAME)
public class ItemResource extends DSpaceResource<ItemRest> {
	public ItemResource(ItemRest item, Utils utils) {
		super(item, utils);
	}
}
