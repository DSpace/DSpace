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

    /**
     * Generates an {@link AuthorityValue} based on the given parameters.
     *
     * @param authorityKey  the authority key to be assigned to the generated authority value
     * @param content       the content of the generated authority value
     * @param field         the field of the generated authority value
     * @return the generated {@link AuthorityValue}
     */
    AuthorityValue generate(String authorityKey, String content, String field);

    /**
     * Updates an AuthorityValue.
     *
     * @param value the AuthorityValue to be updated
     * @return the updated AuthorityValue
     */
    AuthorityValue update(AuthorityValue value);

    /**
     * Finds an AuthorityValue based on the provided authorityID.
     *
     * @param authorityID the authority ID used to search for the AuthorityValue
     * @return the found AuthorityValue, or null if no match is found
     */
    AuthorityValue findByUID(String authorityID);

    /**
     * Finds AuthorityValues in the given context based on field and value.
     *
     * @param field the field to search for AuthorityValues
     * @param value the value to search for AuthorityValues
     * @return a list of found AuthorityValues matching the given field and value, or an empty list if no match is found
     */
    List<AuthorityValue> findByValue(String field, String value);

    /**
     * Finds an {@link AuthorityValue} based on the provided ORCID ID.
     *
     * @param orcid_id the ORCID ID used to search for the AuthorityValue
     * @return the found AuthorityValue, or null if no match is found
     */
    AuthorityValue findByOrcidID(String orcid_id);

    /**
     * Finds {@link AuthorityValue}s based on the provided metadata schema, element, qualifier, and name.
     *
     * @param schema    the schema of the AuthorityValue
     * @param element   the element of the AuthorityValue
     * @param qualifier the qualifier of the AuthorityValue
     * @param name      the name of the AuthorityValue
     * @return a list of found AuthorityValues matching the given schema, element, qualifier, and name,
     *         or an empty list if no match is found
     */
    List<AuthorityValue> findByName(String schema, String element, String qualifier, String name);

    /**
     * Finds {@link AuthorityValue}s based on the provided metadata schema, element, qualifier, and value.
     *
     * @param schema    the schema of the AuthorityValue
     * @param element   the element of the AuthorityValue
     * @param qualifier the qualifier of the AuthorityValue
     * @param value     the value of the AuthorityValue
     * @return a list of found AuthorityValues matching the given schema, element, qualifier, and value,
     *         or an empty list if no match is found
     */
    List<AuthorityValue> findByAuthorityMetadata(String schema, String element, String qualifier, String value);

    /**
     * Finds {@link AuthorityValue}s in the given context based on the exact field and value.
     *
     * @param field the field to search for AuthorityValues
     * @param value the value to search for AuthorityValues
     * @return a list of found AuthorityValues matching the given field and value,
     *         or an empty list if no match is found
     */
    List<AuthorityValue> findByExactValue(String field, String value);

    /**
     * Finds {@link AuthorityValue}s based on the provided metadata schema, element, qualifier, and value.
     *
     * @param schema    the schema of the AuthorityValue
     * @param element   the element of the AuthorityValue
     * @param qualifier the qualifier of the AuthorityValue
     * @param value     the value of the AuthorityValue
     * @return a list of found AuthorityValues matching the given schema, element, qualifier, and value,
     *         or an empty list if no match is found
     */
    List<AuthorityValue> findByValue(String schema, String element, String qualifier,
                                            String value);

    /**
     * Finds AuthorityValues that are ORCID person authority values.
     *
     * @return a list of AuthorityValues or an empty list if no matching values are found
     */
    List<AuthorityValue> findOrcidHolders();

    /**
     * Retrieves all AuthorityValues from Solr.
     *
     * @return A list of all AuthorityValues.
     */
    List<AuthorityValue> findAll();

    /**
     * Converts a SolrDocument into an AuthorityValue object.
     *
     * @param solrDocument the SolrDocument to convert
     * @return the converted AuthorityValue object
     */
    AuthorityValue fromSolr(SolrDocument solrDocument);

    /**
     * Retrieves the type of authority value based on the provided metadata string.
     *
     * @param metadataString the metadata string used to determine the authority value type
     * @return the {@link AuthorityValue} representing the type of authority value, or null if no match is found
     */
    AuthorityValue getAuthorityValueType(String metadataString);
}
