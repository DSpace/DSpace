/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service;

import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.Context;

/**
 * Interface used to define an index strategy used to collect item information
 * related to itemFirst and itemSecond.
 */
public interface SolrDedupServiceIndexPlugin {

    public void additionalIndex(Context context, UUID itemFirst, UUID itemSecond, SolrInputDocument document);

}
