/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

/**
 * Interface for generating additional metadata (extras) for Item authority results.
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public interface ItemAuthorityExtraMetadataGenerator {

    /**
     * Builds a map of additional metadata for a single authority choice.
     *
     * @param authorityName the name of the authority being queried
     * @param solrDocument  the source Solr document containing the data
     * @return a map of key-value pairs to be stored in the Choice's extras
     */
    Map<String, String> build(String authorityName, SolrDocument solrDocument);

    /**
     * Generates a list of multiple choices from a single Solr document.
     * <p>
     * This is used when one authority entry (like a Person) might yield several
     * selectable options based on its internal metadata (like different affiliations).
     * </p>
     *
     * @param authorityName the name of the authority being queried
     * @param solrDocument  the source Solr document
     * @return a list of Choice objects derived from the document
     */
    List<Choice> buildAggregate(String authorityName, SolrDocument solrDocument);

}