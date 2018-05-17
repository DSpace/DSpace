/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package org.dspace.resourcesync;

import java.util.ArrayList;
import java.util.List;

import org.dspace.core.ConfigurationManager;
/**
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
public class ResourceSyncConfiguration {
	
	private static List<String> exposeBundles = null;
	
	public static List<String> getBundlesToExpose()
    {
		if (exposeBundles != null) {
			return exposeBundles;
		}
		
		exposeBundles = new ArrayList<String>();
        String cfg = ConfigurationManager.getProperty("resourcesync", "expose-bundles");
        if (cfg == null || "".equals(cfg))
        {
            return exposeBundles;
        }

        String[] bits = cfg.split(",");
        for (String bundle : bits)
        {
            if (!exposeBundles.contains(bundle))
            {
                exposeBundles.add(bundle);
            }
        }
        return exposeBundles;
    }
}
