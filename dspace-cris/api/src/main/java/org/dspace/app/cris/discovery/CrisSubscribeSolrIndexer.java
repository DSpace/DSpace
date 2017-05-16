/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

public class CrisSubscribeSolrIndexer implements SolrServiceIndexPlugin {

	@Override
	public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters) {
		
       if (dso instanceof Item)
        {
            Item item = (Item) dso;
            
			Date lastModified = item.getLastModified();
			String dateAccessioned = item.getMetadata("dc.date.accessioned");

			if (lastModified != null)
			{
			    document.addField("itemLastModified_dt", lastModified);
			}
            
            if (StringUtils.isNotBlank(dateAccessioned))
            {
				document.addField("dateaccessioned_dt", dateAccessioned);
            }      
        }
	}
}
