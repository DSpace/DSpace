/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.core.Context;

/**
 * Plugin from which users can add additional search parameters for every search that occurs in discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface SolrServiceSearchPlugin {

    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery);
}
