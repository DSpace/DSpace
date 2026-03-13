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

    /**
     * Generates an {@link AuthorityValue} based on the given parameters.
     *
     * @param authorityKey  the authority key to be assigned to the generated authority value
     * @param content       the content of the generated authority value
     * @param field         the field of the generated authority value
     * @return the generated {@link AuthorityValue}
     */
    public AuthorityValue generate(String authorityKey, String content, String field);

    /**
     * Updates an AuthorityValue.
     *
     * @param value the AuthorityValue to be updated
     * @return the updated AuthorityValue
     */
    public AuthorityValue update(AuthorityValue value);

    /**
     * Finds an AuthorityValue based on the provided authorityID.
     *
     * @param authorityID the authority ID used to search for the AuthorityValue
     * @return the found AuthorityValue, or null if no match is found
     */
    public AuthorityValue findByUID(String authorityID);

    /**
     * Finds AuthorityValues in the given context based on field and value.
     *
     * @param field the field to search for AuthorityValues
     * @param value the value to search for AuthorityValues
     * @return a list of found AuthorityValues matching the given field and value, or an empty list if no match is found
     */
    public List<AuthorityValue> findByValue(String field, String value);

    /**
     * Finds an {@link AuthorityValue} based on the provided ORCID ID.
     *
     * @param orcid_id the ORCID ID used to search for the AuthorityValue
     * @return the found AuthorityValue, or null if no match is found
     */
    public AuthorityValue findByOrcidID(String orcid_id);

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
    public List<AuthorityValue> findByName(String schema, String element, String qualifier,
                                           String name);

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
    public List<AuthorityValue> findByAuthorityMetadata(String schema, String element,
                                                        String qualifier, String value);

    /**
     * Finds {@link AuthorityValue}s in the given context based on the exact field and value.
     *
     * @param field the field to search for AuthorityValues
     * @param value the value to search for AuthorityValues
     * @return a list of found AuthorityValues matching the given field and value,
     *         or an empty list if no match is found
     */
    public List<AuthorityValue> findByExactValue(String field, String value);

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
    public List<AuthorityValue> findByValue(String schema, String element, String qualifier,
                                            String value);

    /**
     * Finds AuthorityValues that are ORCID person authority values.
     *
     * @return a list of AuthorityValues or an empty list if no matching values are found
     */
    public List<AuthorityValue> findOrcidHolders();

    /**
     * Retrieves all AuthorityValues from Solr.
     *
     * @return A list of all AuthorityValues.
     */
    public List<AuthorityValue> findAll();

    /**
     * Converts a SolrDocument into an AuthorityValue object.
     *
     * @param solrDocument the SolrDocument to convert
     * @return the converted AuthorityValue object
     */
    public AuthorityValue fromSolr(SolrDocument solrDocument);

    /**
     * Retrieves the type of authority value based on the provided metadata string.
     *
     * @param metadataString the metadata string used to determine the authority value type
     * @return the {@link AuthorityValue} representing the type of authority value, or null if no match is found
     */
    public AuthorityValue getAuthorityValueType(String metadataString);
}
