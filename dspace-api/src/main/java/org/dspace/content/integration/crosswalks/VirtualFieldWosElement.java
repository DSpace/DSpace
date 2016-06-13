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
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;

/**
 * Implements virtual field processing for generate wos html tag map element
 *
 * @author l.pascarelli
 *
 */
public class VirtualFieldWosElement implements VirtualFieldDisseminator {

	private static final String fieldPubmedID = ConfigurationManager.getProperty("cris", "ametrics.identifier.pmid");
	private static final String fieldWosID = ConfigurationManager.getProperty("cris", "ametrics.identifier.ut");
	private static final String fieldDoiID = ConfigurationManager.getProperty("cris", "ametrics.identifier.doi");

	@Override
	public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
		String result = "<map name=\"" + item.getID() + "\">";
		Metadatum[] metadatumPMID = item.getMetadataByMetadataString(fieldPubmedID);
		if (metadatumPMID != null && metadatumPMID.length > 0) {
			result += "<val name=\"pmid\">" + metadatumPMID[0].value + "</val>";
		}
		Metadatum[] metadatumDoi = item.getMetadataByMetadataString(fieldDoiID);
		if (metadatumDoi != null && metadatumDoi.length > 0) {
			result += "<val name=\"doi\">" + metadatumDoi[0].value + "</val>";
		}
		Metadatum[] metadatumWos = item.getMetadataByMetadataString(fieldWosID);
		if (metadatumWos != null && metadatumWos.length > 0) {
			result += "<val name=\"ut\">" + metadatumWos[0].value + "</val>";
		}
		result += "</map>";
		return new String[]{result};
	}
}
