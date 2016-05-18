/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This plugin adds three fields to the solr index to make a facet with/without
 * content in the ORIGINAL Bundle possible (like full text, images...). It is
 * activated simply by adding this class as a bean to discovery.xml.
 * 
 * The facet is added to Discovery in the usual way (create a searchFilter bean
 * and add it to the expected place) just with an empty list of used metadata
 * fields because there are none.
 * 
 * @author Christian Scheible christian.scheible@uni-konstanz.de
 * 
 */
public class SolrServiceContentInOriginalBundleFilterPlugin implements SolrServiceIndexPlugin
{

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document)
    {
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            boolean hasOriginalBundleWithContent = hasOriginalBundleWithContent(item);

            // _keyword and _filter because
            // they are needed in order to work as a facet and filter.
            if (!hasOriginalBundleWithContent)
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
    }

    /**
     * Checks whether the given item has a bundle with the name ORIGINAL
     * containing at least one bitstream.
     * 
     * @param item
     *            to check
     * @return true if there is at least on bitstream in the bundle named
     *         ORIGINAL, otherwise false
     */
    private boolean hasOriginalBundleWithContent(Item item)
    {
        List<Bundle> bundles;
        bundles = item.getBundles();
        if (bundles != null)
        {
            for (Bundle curBundle : bundles)
            {
                String bName = curBundle.getName();
                if ((bName != null) && bName.equals("ORIGINAL"))
                {
                    List<Bitstream> bitstreams = curBundle.getBitstreams();
                    if (bitstreams != null && bitstreams.size() > 0)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
