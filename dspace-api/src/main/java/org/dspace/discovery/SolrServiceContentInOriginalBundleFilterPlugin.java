/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;

/**
 * This plugin adds three fields to the solr index to make a facet with/without
 * content in the ORIGINAL Bundle possible (like full text, images...). It is
 * activated simply by adding this class as a bean to discovery.xml.
 * 
 * The facet is added to Discovery in the usual way (create a searchFilter bean
 * and add it to the expected place) just with an empty list of used metadata
 * fields because there are none.
 * 
 * @author Christian Scheible <christian.scheible@uni-konstanz.de>
 * 
 */
public class SolrServiceContentInOriginalBundleFilterPlugin implements SolrServiceIndexPlugin
{

    private static final Logger log = Logger.getLogger(SolrServiceContentInOriginalBundleFilterPlugin.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document)
    {
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            Bundle[] bundles;
            try
            {
                bundles = item.getBundles("ORIGINAL");
                // _keyword and _filter because
                // they are needed in order to work as a facet and filter.
                if (bundles.length == 0)
                {
                    // no content in the original bundle
                    document.addField("has_content_in_original_bundle", false);
                    document.addField("has_content_in_original_bundle_keyword", false);
                    document.addField("has_content_in_original_bundle_filter", false);
                }
                else
                {
                    document.addField("has_content_in_original_bundle", true);
                    document.addField("has_content_in_original_bundle_keyword", true);
                    document.addField("has_content_in_original_bundle_filter", true);
                }
            }
            catch (SQLException e)
            {
                log.error("Error adding additional solr field for original bundle content facet: " + e.getMessage());
            }
        }

    }
}
