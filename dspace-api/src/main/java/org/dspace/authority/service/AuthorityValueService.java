/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.dspace.authority.AuthorityValue;

/**
 * This service contains all methods for using authority values
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface AuthorityValueService {
    public static final String SPLIT = "::";
    public static final String GENERATE = "will be generated" + SPLIT;

    public AuthorityValue generate(String authorityKey, String content, String field);

    public AuthorityValue update(AuthorityValue value);

    public AuthorityValue findByUID(String authorityID);

    public List<AuthorityValue> findByValue(String field, String value);

    public AuthorityValue findByOrcidID(String orcid_id);

    public List<AuthorityValue> findByName(String schema, String element, String qualifier,
                                           String name);

    public List<AuthorityValue> findByAuthorityMetadata(String schema, String element,
                                                        String qualifier, String value);

    public List<AuthorityValue> findByExactValue(String field, String value);

    public List<AuthorityValue> findByValue(String schema, String element, String qualifier,
                                            String value);

    public List<AuthorityValue> findOrcidHolders();

    public List<AuthorityValue> findAll();

    public AuthorityValue fromSolr(SolrDocument solrDocument);

    public AuthorityValue getAuthorityValueType(String metadataString);
}
