/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.List;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

public class ORCIDImportFiller extends ItemMetadataImportFiller {

	@Override
	public void fillRecord(Context context, Item item, List<Metadatum> metadata, String ORCID, ACrisObject crisObject) {
		OrcidPreferencesUtils.populateRP((ResearcherPage) crisObject, ORCID);
		super.fillRecord(context, item, metadata, ORCID, crisObject);
	}
}
