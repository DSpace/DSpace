/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;

/**
 * Indexing plugin used when indexing the communities/collections/items into DSpace
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface SolrServiceIndexPlugin {

    public void additionalIndex(DSpaceObject dso, SolrInputDocument document);
}
