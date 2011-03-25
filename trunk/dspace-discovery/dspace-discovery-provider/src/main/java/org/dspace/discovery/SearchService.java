/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.util.List;

/**
 * Search interface that discovery to search in solr
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface SearchService {

    QueryResponse search(SolrQuery query) throws SearchServiceException;

    List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery);

    List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery);
}
