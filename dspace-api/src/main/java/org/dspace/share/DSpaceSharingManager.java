/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.share;

import java.util.ArrayList;
import java.util.List;

import org.dspace.core.ConfigurationManager;
import org.dspace.services.SharingService;
import org.dspace.services.share.ShareProvider;

/**
 * DSpace generic share service (url based)
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceSharingManager implements SharingService {
	private static List<ShareProvider> providers = null;
	
	private void loadSimpleProviders () {
		String list = ConfigurationManager.getProperty("sharingbar", "list");
		if (list == null) list = "";
		String[] ids = list.split(",");
		for (String id : ids) {
			if (id != null && !id.trim().equals("")) {
				id = id.trim();
				providers.add(DSpaceSharingProvider.getProvider(id));
			}
		}
	}
	
	@Override
	public List<ShareProvider> getProviders() {
		if (providers == null) {
			providers = new ArrayList<ShareProvider>();
			loadSimpleProviders();
		}
		return providers;
	}

}
