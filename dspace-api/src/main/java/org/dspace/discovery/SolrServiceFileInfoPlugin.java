/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.util.List;

/**
 * <p>
 * Adds filenames and file descriptions of all files in the ORIGINAL bundle
 * to the Solr search index.
 *
 * <p>
 * To activate the plugin, add the following line to discovery.xml
 * <pre>
 * {@code <bean id="solrServiceFileInfoPlugin" class="org.dspace.discovery.SolrServiceFileInfoPlugin"/>}
 * </pre>
 *
 * <p>
 * After activating the plugin, rebuild the discovery index by executing:
 * <pre>
 * [dspace]/bin/dspace index-discovery -b
 * </pre>
 *
 * @author Martin Walk
 */
public class SolrServiceFileInfoPlugin implements SolrServiceIndexPlugin
{
    private static final String BUNDLE_NAME = "ORIGINAL";
    private static final String SOLR_FIELD_NAME_FOR_FILENAMES = "original_bundle_filenames";
    private static final String SOLR_FIELD_NAME_FOR_DESCRIPTIONS = "original_bundle_descriptions";

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document)
    {
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            List<Bundle> bundles = item.getBundles();
            if (bundles != null)
            {
                for (Bundle bundle : bundles)
                {
                    String bundleName = bundle.getName();
                    if ((bundleName != null) && bundleName.equals(BUNDLE_NAME))
                    {
                        List<Bitstream> bitstreams = bundle.getBitstreams();
                        if (bitstreams != null)
                        {
                            for (Bitstream bitstream : bitstreams)
                            {
                                document.addField(SOLR_FIELD_NAME_FOR_FILENAMES, bitstream.getName());

                                String description = bitstream.getDescription();
                                if ((description != null) && (!description.isEmpty()))
                                {
                                    document.addField(SOLR_FIELD_NAME_FOR_DESCRIPTIONS, description);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
