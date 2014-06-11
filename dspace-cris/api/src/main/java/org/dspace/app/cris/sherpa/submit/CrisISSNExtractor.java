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
