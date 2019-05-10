/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.zdb.ZDBService;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class ZDBImportFiller extends ItemMetadataImportFiller {

	private static final Logger log = Logger.getLogger(ZDBImportFiller.class);

	private ZDBService source;

	private Map<String, String> mapMetadata;
	
	@Override
	public void fillRecord(Context context, Item item, List<Metadatum> metadata, String identifier, ACrisObject crisObject) {
		AuthorityValue authorityValue;
		try {
			authorityValue = getSource().details(identifier);
			populateJournal(crisObject, authorityValue);
		} catch (IOException e) {
			log.warn(e.getMessage());
		}
		super.fillRecord(context, item, metadata, identifier, crisObject);
	}

	private void populateJournal(ACrisObject crisObject, AuthorityValue authorityValue) {
		for(String metadata : authorityValue.getOtherMetadata().keySet()) {
			if(getMapMetadata().containsKey(metadata)) {
				for(String value : authorityValue.getOtherMetadata().get(metadata)) {
					ResearcherPageUtils.buildTextValue(crisObject, value, getMapMetadata().get(metadata));
				}
			}
		}
	}

	public Map<String, String> getMapMetadata() {
		return mapMetadata;
	}

	public void setMapMetadata(Map<String, String> mapMetadata) {
		this.mapMetadata = mapMetadata;
	}

	public ZDBService getSource() {
		if(source==null) {
			source = new DSpace().getServiceManager().getServiceByName("ZDBSource", ZDBService.class);
		}
		return source;
	}

	public void setSource(ZDBService source) {
		this.source = source;
	}
}
