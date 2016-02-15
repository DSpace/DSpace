/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.Map;

public class AuthoritiesFillerConfig {
	private Map<String, ImportAuthorityFiller> fillers;

	ImportAuthorityFiller getFiller(String authorityType) {
		ImportAuthorityFiller filler = null;
		if (fillers != null) {
			filler = fillers.get(authorityType);
			if (filler == null) {
				filler = fillers.get("default");
			}
		}
		return filler;
	}

	public void setFillers(Map<String, ImportAuthorityFiller> fillers) {
		this.fillers = fillers;
	}
}
