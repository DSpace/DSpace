/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoverySearchFilter;


/**
 * Example class that prints out the handle of the DSpace Object currently being indexed
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class SolrServiceIndexOutputPlugin implements SolrServiceIndexPlugin{

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters) {
        System.out.println("Currently indexing: " + dso.getHandle());
    }
}
