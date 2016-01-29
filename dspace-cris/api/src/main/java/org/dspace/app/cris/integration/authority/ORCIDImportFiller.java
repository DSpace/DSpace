package org.dspace.app.cris.integration.authority;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;

public class ORCIDImportFiller implements ImportAuthorityFiller {

	@Override
	public void fillRecord(String ORCID, ACrisObject crisObject) {
		OrcidPreferencesUtils.populateRP((ResearcherPage) crisObject, ORCID);
	}
}
