/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import org.dspace.app.cris.model.ACrisObject;

public interface ImportAuthorityFiller {
	void fillRecord(String authorityID, ACrisObject crisObject);
}
