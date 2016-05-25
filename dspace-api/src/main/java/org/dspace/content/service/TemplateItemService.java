package org.dspace.content.service;

import org.dspace.content.Item;

public interface TemplateItemService {
	void applyTemplate(Item targetItem, Item templateItem);
}
