/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import java.util.List;
import java.util.Set;

import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.DSpaceControlledVocabularyIndex;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 * {@code
 * # names the ChoiceAuthority plugin called for this field
 * choices.plugin.<FIELD> = name-of-plugin
 *
 * # mode of UI presentation desired in submission UI:
 * #  "select" is dropdown menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 * choices.presentation.<FIELD> = "select" | "suggest"
 *
 * # is value "closed" to the set of these choices or are non-authority values permitted?
 * choices.closed.<FIELD> = true | false
 * }
 *
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public interface ChoiceAuthorityService {

    /**
     * @return the names of all the defined choice authorities
     */
    public Set<String> getChoiceAuthoritiesNames();

    /**
     * @param schema    schema of metadata field
     * @param element   element of metadata field
     * @param qualifier qualifier of metadata field
     * @return the name of the choice authority associated with the specified
     * metadata. Throw IllegalArgumentException if the supplied metadata
     * is not associated with an authority choice
     */
    public String getChoiceAuthorityName(String schema, String element, String qualifier, Collection collection);

    /**
     * Wrapper that calls getMatches method of the plugin corresponding to
     * the metadata field defined by schema,element,qualifier.
     *
     * @param schema     schema of metadata field
     * @param element    element of metadata field
     * @param qualifier  qualifier of metadata field
     * @param query      user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start      choice at which to start, 0 is first.
     * @param limit      maximum number of choices to return, 0 for no limit.
     * @param locale     explicit localization key if available, or null
     * @return a Choices object (never null).
     * @see org.dspace.content.authority.ChoiceAuthority#getMatches(java.lang.String, java.lang.String, org.dspace
     * .content.Collection, int, int, java.lang.String)
     */
    public Choices getMatches(String schema, String element, String qualifier,
                              String query, Collection collection, int start, int limit, String locale);

    /**
     * Wrapper calls getMatches method of the plugin corresponding to
     * the metadata field defined by single field key.
     *
     * @param fieldKey   single string identifying metadata field
     * @param query      user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start      choice at which to start, 0 is first.
     * @param limit      maximum number of choices to return, 0 for no limit.
     * @param locale     explicit localization key if available, or null
     * @return a Choices object (never null).
     * @see org.dspace.content.authority.ChoiceAuthority#getMatches(java.lang.String, java.lang.String, org.dspace
     * .content.Collection, int, int, java.lang.String)
     */
    public Choices getMatches(String fieldKey, String query, Collection collection,
                              int start, int limit, String locale);

    /**
     * Wrapper that calls getBestMatch method of the plugin corresponding to
     * the metadata field defined by single field key.
     *
     * @param fieldKey   single string identifying metadata field
     * @param query      user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale     explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     * @see org.dspace.content.authority.ChoiceAuthority#getBestMatch(java.lang.String, java.lang.String, org.dspace
     * .content.Collection, java.lang.String)
     */
    public Choices getBestMatch(String fieldKey, String query, Collection collection,
                                String locale);

    /**
     * Wrapper that calls getLabel method of the plugin corresponding to
     * the metadata field defined by schema,element,qualifier.
     *
     * @param metadataValue metadata value
     * @param collection Collection owner of Item
     * @param locale        explicit localization key if available
     * @return label
     */
    public String getLabel(MetadataValue metadataValue, Collection collection, String locale);

    /**
     * Wrapper that calls getLabel method of the plugin corresponding to
     * the metadata field defined by single field key.
     *
     * @param fieldKey single string identifying metadata field
     * @param collection Collection owner of Item
     * @param locale   explicit localization key if available
     * @param authKey  authority key
     * @return label
     */
    public String getLabel(String fieldKey, Collection collection, String authKey, String locale);

    /**
     * Predicate, is there a Choices configuration of any kind for the
     * given metadata field?
     *
     * @param fieldKey single string identifying metadata field
     * @param collection Collection owner of Item
     * @return true if choices are configured for this field.
     */
    public boolean isChoicesConfigured(String fieldKey, Collection collection);

    /**
     * Get the presentation keyword (should be "lookup", "select" or "suggest", but this
     * is an informal convention so it can be easily extended) for this field.
     *
     * @param fieldKey field key
     * @return configured presentation type for this field, or null if none found
     */
    public String getPresentation(String fieldKey);

    /**
     * Get the configured "closed" value for this field.
     *
     * @param fieldKey single string identifying metadata field
     * @return true if choices are closed for this field.
     */
    public boolean isClosed(String fieldKey);

    /**
     * Wrapper to call plugin's getVariants().
     *
     * @param metadataValue metadata value
     * @return List of variants
     */
    public List<String> getVariants(MetadataValue metadataValue, Collection collection);

    /**
     * Return the ChoiceAuthority instance identified by the specified name
     * 
     * @param authorityName the ChoiceAuthority instance name
     * @return the ChoiceAuthority identified by the specified name
     */
    public ChoiceAuthority getChoiceAuthorityByAuthorityName(String authorityName);

    /**
     * This method has been created to have a way of clearing the cache kept inside the service
     */
    public void clearCache();

    /**
     * Should we store the authority key (if any) for such field key and collection?
     * 
     * @param fieldKey   single string identifying metadata field
     * @param collection Collection owner of Item or where the item is submitted to
     * @return true if the configuration allows to store the authority value
     */
    public boolean storeAuthority(String fieldKey, Collection collection);

    /**
     * Wrapper that calls getChoicesByParent method of the plugin.
     *
     * @param authorityName authority name
     * @param parentId      parent Id
     * @param start         choice at which to start, 0 is first.
     * @param limit         maximum number of choices to return, 0 for no limit.
     * @param locale        explicit localization key if available, or null
     * @return a Choices object (never null).
     * @see org.dspace.content.authority.ChoiceAuthority#getChoicesByParent(java.lang.String, java.lang.String,
     *  int, int, java.lang.String)
     */
    public Choices getChoicesByParent(String authorityName, String parentId, int start, int limit, String locale);

    /**
     * Wrapper that calls getTopChoices method of the plugin.
     *
     * @param authorityName authority name
     * @param start         choice at which to start, 0 is first.
     * @param limit         maximum number of choices to return, 0 for no limit.
     * @param locale        explicit localization key if available, or null
     * @return a Choices object (never null).
     * @see org.dspace.content.authority.ChoiceAuthority#getTopChoices(java.lang.String, int, int, java.lang.String)
     */
    public Choices getTopChoices(String authorityName, int start, int limit, String locale);

    /**
     * Return the direct parent of an entry identified by its id in an hierarchical
     * authority.
     * 
     * @param authorityName authority name
     * @param vocabularyId  child id
     * @param locale        explicit localization key if available, or null
     * @return the parent Choice object if any
     */
    public Choice getParentChoice(String authorityName, String vocabularyId, String locale);

    public DSpaceControlledVocabularyIndex getVocabularyIndex(String nameVocab);

}
