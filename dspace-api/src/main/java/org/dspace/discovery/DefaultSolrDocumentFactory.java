/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;

/**
 * Factory default implementation to create documents object for Solr indexing.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class DefaultSolrDocumentFactory implements SolrDocumentFactory {
    @Override
    public SolrInputDocument create() {
        return new SolrInputDocument();
    }
}

