/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

import java.util.List;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *  {@code
 *  # names the ChoiceAuthority plugin called for this field
 *  choices.plugin.<FIELD> = name-of-plugin
 *
 *  # mode of UI presentation desired in submission UI:
 *  #  "select" is dropdown menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 *  choices.presentation.<FIELD> = "select" | "suggest"
 *
 *  # is value "closed" to the set of these choices or are non-authority values permitted?
 *  choices.closed.<FIELD> = true | false
 *  }
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public interface ChoiceAuthorityService
{

    /**
     *  Wrapper that calls getMatches method of the plugin corresponding to
     *  the metadata field defined by schema,element,qualifier.
     *
     * @see org.dspace.content.authority.ChoiceAuthority#getMatches(java.lang.String, java.lang.String, org.dspace.content.Collection, int, int, java.lang.String)
     * @param schema schema of metadata field
     * @param element element of metadata field
     * @param qualifier qualifier of metadata field
     * @param query user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String schema, String element, String qualifier,
                              String query, Collection collection, int start, int limit, String locale);
    /**
     *  Wrapper calls getMatches method of the plugin corresponding to
     *  the metadata field defined by single field key.
     *
     * @see org.dspace.content.authority.ChoiceAuthority#getMatches(java.lang.String, java.lang.String, org.dspace.content.Collection, int, int, java.lang.String)
     * @param fieldKey single string identifying metadata field
     * @param query user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String fieldKey, String query, Collection collection,
                              int start, int limit, String locale);

    public Choices getMatches(String fieldKey, String query, Collection collection, int start, int limit, String locale, boolean externalInput);

    /**
     *  Wrapper that calls getBestMatch method of the plugin corresponding to
     *  the metadata field defined by single field key.
     *
     * @see org.dspace.content.authority.ChoiceAuthority#getBestMatch(java.lang.String, java.lang.String, org.dspace.content.Collection, java.lang.String)
     * @param fieldKey single string identifying metadata field
     * @param query user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String fieldKey, String query, Collection collection,
                                String locale);

    /**
     *  Wrapper that calls getLabel method of the plugin corresponding to
     *  the metadata field defined by schema,element,qualifier.
     * @param metadataValue metadata value
     * @param locale explicit localization key if available
     * @return label
     */
    public String getLabel(MetadataValue metadataValue, String locale);

    /**
     *  Wrapper that calls getLabel method of the plugin corresponding to
     *  the metadata field defined by single field key.
     * @param fieldKey single string identifying metadata field
     * @param locale explicit localization key if available
     * @param authKey authority key
     * @return label
     */
    public String getLabel(String fieldKey, String authKey, String locale);

    /**
     * Predicate, is there a Choices configuration of any kind for the
     * given metadata field?
     * @param fieldKey single string identifying metadata field
     * @return true if choices are configured for this field.
     */
    public boolean isChoicesConfigured(String fieldKey);

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
     * @param metadataValue metadata value
     * @return List of variants
     */
    public List<String> getVariants(MetadataValue metadataValue);
}