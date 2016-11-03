/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;

public interface TemplateValueGenerator {
	Metadatum[] generator(Item targetItem, Item templateItem, Metadatum m, String extraParams);
}
