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
