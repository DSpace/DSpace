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
 * 
 * Interface to manage simple/aggregation for extra values on authority
 * 
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public interface ItemAuthorityExtraMetadataGenerator {

    public Map<String, String> build(String authorityName, SolrDocument solrDocument);

    public List<Choice> buildAggregate(String authorityName, SolrDocument solrDocument);

}