/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface TemplateItemService {
	void applyTemplate(Context context, Item targetItem, Item templateItem);
	void clearTemplate(Context context, Item item, Item item1);
}
