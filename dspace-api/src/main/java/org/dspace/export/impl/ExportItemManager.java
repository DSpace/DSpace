/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.impl;

import java.util.ArrayList;
import java.util.List;

import org.dspace.core.ConfigurationManager;
import org.dspace.export.api.ExportItemProvider;
import org.dspace.export.api.ExportItemService;

/**
 * Export item service.
 * Configuration based on ConfigurationManager.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public class ExportItemManager implements ExportItemService {
	private List<ExportItemProvider> list = null;
	
	
	@Override
	public List<ExportItemProvider> getProviders() {
		if (list == null) {
			list = new ArrayList<ExportItemProvider>();
			String l = ConfigurationManager.getProperty("export", "list");
			if (l == null) l = "";
			String[] ids = l.split(",");
			for (String id : ids) {
				if (id != null && !id.trim().equals("")) {
					id = id.trim();
					DSpaceExportItemProvider p = DSpaceExportItemProvider.getInstance(id);
					if (p != null) list.add(p);
				}
			}
		}
		return list;
	}

	@Override
	public ExportItemProvider getProvider(String id) {
		return DSpaceExportItemProvider.getInstance(id);
	}

}
