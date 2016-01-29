package org.dspace.app.cris.integration.authority;

import org.dspace.app.cris.model.ACrisObject;

public interface ImportAuthorityFiller {
	void fillRecord(String authorityID, ACrisObject crisObject);
}
