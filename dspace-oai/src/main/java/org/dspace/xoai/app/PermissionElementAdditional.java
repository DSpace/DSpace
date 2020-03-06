/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.xoai.util.ItemUtils;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

public class PermissionElementAdditional implements XOAIItemCompilePlugin {

	private static Logger log = LogManager.getLogger(PermissionElementAdditional.class);

	@Override
	public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
		
		Element other;
		List<Element> elements = metadata.getElement();
		if (ItemUtils.getElement(elements, "others") != null) {
			other = ItemUtils.getElement(elements, "others");
		} else {
			other = ItemUtils.create("others");
		}

		String drm = null;

		try {
			drm = buildPermission(context, item);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}

		other.getField().add(ItemUtils.createValue("drm", drm));

		return metadata;
	}

	private String buildPermission(Context context, Item item) throws SQLException {

		String values = "metadata only access";
		Bundle[] bnds;
		try {
			bnds = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		for (Bundle bnd : bnds) {

			Bitstream bitstream = Bitstream.find(context, bnd.getPrimaryBitstreamID());
			if (bitstream == null) {
				for (Bitstream b : bnd.getBitstreams()) {
					bitstream = b;
					break;
				}
			}

			if (bitstream == null) {
				return "metadata only access";
			}
			values = ItemUtils.getDRM(AuthorizeManager.getPoliciesActionFilter(context, bitstream, Constants.READ));
		}
		return values;
	}

}