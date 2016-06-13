/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;

/**
 * Implements virtual field processing for split pagenumber range information.
 * 
 * @author bollini
 */
public class VirtualFieldPageNumber implements VirtualFieldDisseminator, VirtualFieldIngester {
	public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
		// Check to see if the virtual field is already in the cache
		// - processing is quite intensive, so we generate all the values on
		// first request
		if (fieldCache.containsKey(fieldName))
			return new String[] { fieldCache.get(fieldName) };

		String[] virtualFieldName = fieldName.split("\\.");

		String qualifier = virtualFieldName[2];
		String separator = " - ";
		if (qualifier.equals("bibtex")) {
			separator = "--";
		}
		// Get the citation from the item
		Metadatum[] dcvs = item.getMetadataValueInDCFormat("dc.relation.firstpage");
		Metadatum[] dcvs2 = item.getMetadataValueInDCFormat("dc.relation.lastpage");

		if ((dcvs != null && dcvs.length > 0) && (dcvs2 != null && dcvs2.length > 0)) {
			String value = dcvs[0].value + separator + dcvs2[0].value;
			fieldCache.put(fieldName, value);
			return new String[] { value };
		}

		return null;
	}

	public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
		// NOOP - we won't add any metadata yet, we'll pick it up when we
		// finalise the item
		return true;
	}

	public boolean finalizeItem(Item item, Map<String, String> fieldCache) {
		return false;
	}
}
