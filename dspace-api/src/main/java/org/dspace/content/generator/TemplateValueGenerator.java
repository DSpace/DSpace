package org.dspace.content.generator;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;

public interface TemplateValueGenerator {
	Metadatum[] generator(Item targetItem, Item templateItem, Metadatum m, String extraParams);
}
