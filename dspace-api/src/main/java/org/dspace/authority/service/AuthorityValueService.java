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
    String SPLIT = "::";
    String GENERATE = "will be generated" + SPLIT;
    String REFERENCE = "will be referenced" + SPLIT;

    // Cleanup authority metadata modes
    String AUTHORITY_CLEANUP_PROPERTY_PREFIX = "item-deletion.authority-cleanup.mode.";
    String AUTHORITY_CLEANUP_BUSINESS_MODE = "business-identifier";
    String AUTHORITY_CLEANUP_CLEAN_ALL_MODE = "clean_all";

    AuthorityValue generate(String authorityKey, String content, String field);

    AuthorityValue update(AuthorityValue value);

    AuthorityValue findByUID(String authorityID);

    List<AuthorityValue> findByValue(String field, String value);

    AuthorityValue findByOrcidID(String orcid_id);

    List<AuthorityValue> findByName(String schema, String element, String qualifier,
                                    String name);

    List<AuthorityValue> findByAuthorityMetadata(String schema, String element,
                                                 String qualifier, String value);

    List<AuthorityValue> findByExactValue(String field, String value);

    List<AuthorityValue> findByValue(String schema, String element, String qualifier,
                                     String value);

    List<AuthorityValue> findAll();

    AuthorityValue fromSolr(SolrDocument solrDocument);

    AuthorityValue getAuthorityValueType(String metadataString);
}
