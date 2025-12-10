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
import org.dspace.core.Context;

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

    AuthorityValue generate(Context context, String authorityKey, String content, String field);

    AuthorityValue update(AuthorityValue value);

    AuthorityValue findByUID(Context context, String authorityID);

    List<AuthorityValue> findByValue(Context context, String field, String value);

    AuthorityValue findByOrcidID(Context context, String orcid_id);

    List<AuthorityValue> findByName(Context context, String schema, String element, String qualifier,
                                    String name);

    List<AuthorityValue> findByAuthorityMetadata(Context context, String schema, String element,
                                                 String qualifier, String value);

    List<AuthorityValue> findByExactValue(Context context, String field, String value);

    List<AuthorityValue> findByValue(Context context, String schema, String element, String qualifier,
                                     String value);

    List<AuthorityValue> findOrcidHolders(Context context);

    List<AuthorityValue> findAll(Context context);

    AuthorityValue fromSolr(SolrDocument solrDocument);

    AuthorityValue getAuthorityValueType(String metadataString);
}
