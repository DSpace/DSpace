/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * Manage queries of the Solr authority core.
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface AuthoritySearchService {

    public QueryResponse search(SolrQuery query)
            throws SolrServerException, MalformedURLException, IOException;

    /**
     * Retrieves all the metadata fields which are indexed in the authority control.
     *
     * @return names of indexed fields.
     * @throws SolrServerException passed through.
     * @throws MalformedURLException passed through.
     */
    public List<String> getAllIndexedMetadataFields() throws Exception;

}
