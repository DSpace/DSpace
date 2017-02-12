package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(ItemRest.NAME)
public class ItemRestRepository extends DSpaceRestRepository<ItemRest, UUID> {
	ItemService is = ContentServiceFactory.getInstance().getItemService();
	@Autowired
	ItemConverter converter;
	
	
	public ItemRestRepository() {
		System.out.println("Repository initialized by Spring");
	}

	@Override
	public ItemRest findOne(Context context, UUID id) {
		Item item = null;
		try {
			item = is.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return converter.fromModel(item);
	}

	@Override
	public Page<ItemRest> findAll(Context context, Pageable pageable) {
		Iterator<Item> it = null;
		List<Item> items = new ArrayList<Item>();
		int total = 0;
		try {
			total = is.countTotal(context);
			it = is.findAll(context);
			while (it.hasNext()) {
				Item i = it.next();
				items.add(i);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<ItemRest> page = new PageImpl<Item>(items, pageable, total).map(converter);
		return page;
	}
	
	@Override
	public Class<ItemRest> getDomainClass() {
		return ItemRest.class;
	}
	
	@Override
	public ItemResource wrapResource(ItemRest item) {
		return new ItemResource(item);
	}

}