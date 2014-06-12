/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.sherpa.submit;

import java.util.List;

import org.dspace.app.sherpa.submit.MetadataValueISSNExtractor;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class CrisISSNExtractor extends
		MetadataValueISSNExtractor {

	@Override
	public List<String> getISSNs(Context context, Item item) {
		return super.getISSNs(context, item.getWrapper());
	}
}
