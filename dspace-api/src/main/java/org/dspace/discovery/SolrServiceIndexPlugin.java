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
 * Indexing plugin used when indexing the communities/collections/items into DSpace
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface SolrServiceIndexPlugin {

    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters);
}
