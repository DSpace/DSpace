/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;

/**
 * Costruisce i singoli autori a partire dalla stringa allauthor
 * 
 * @author bollini
 */
public class VirtualFieldBibtexAuthors implements VirtualFieldDisseminator, VirtualFieldIngester {
	private static Logger log = Logger.getLogger(VirtualFieldBibtexAuthors.class);

	public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {

		String metadata = "dc.contributor.author";

		// Get the citation from the item
		Metadatum[] dcvs = item.getMetadataValueInDCFormat(metadata);

		if (dcvs != null && dcvs.length > 0) {
			StringBuffer sb = new StringBuffer();
			for (Metadatum a : dcvs) {
				String[] split = a.value.split(", ");
				int splitLength = split.length;
				String str = (splitLength > 1) ? split[1] : "";
				String str2 = split[0];
				if (StringUtils.isNotBlank(str2)) {
					sb.append(str).append(" ");
				}
				sb.append(str2).append(" and ");
			}
			return new String[] { sb.substring(0, sb.length() - 5) };
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
