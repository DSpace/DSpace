/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.content.Collection;

/**
 * Plugin interface that supplies an authority control mechanism for
 * one metadata field.
 *
 * @author Larry Stone
 * @see ChoiceAuthorityServiceImpl
 * @see MetadataAuthorityServiceImpl
 */
public interface ChoiceAuthority
{
    /**
     * Get all values from the authority that match the preferred value.
     * Note that the offering was entered by the user and may contain
     * mixed/incorrect case, whitespace, etc so the plugin should be careful
     * to clean up user data before making comparisons.
     *
     * Value of a "Name" field will be in canonical DSpace person name format,
     * which is "Lastname, Firstname(s)", e.g. "Smith, John Q.".
     *
     * Some authorities with a small set of values may simply return the whole
     * set for any sample value, although it's a good idea to set the
     * defaultSelected index in the Choices instance to the choice, if any,
     * that matches the value.
     *
     * @param field being matched for
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale);

    /**
     * Get the single "best" match (if any) of a value in the authority
     * to the given user value.  The "confidence" element of Choices is
     * expected to be set to a meaningful value about the circumstances of
     * this match.
     *
     * This call is typically used in non-interactive metadata ingest
     * where there is no interactive agent to choose from among options.
     *
     * @param field being matched for
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String field, String text, Collection collection, String locale);

    /**
     * Get the canonical user-visible "label" (i.e. short descriptive text)
     * for a key in the authority.  Can be localized given the implicit
     * or explicit locale specification.
     *
     * This may get called many times while populating a Web page so it should
     * be implemented as efficiently as possible.
     *
     * @param field being matched for     
     * @param key authority key known to this authority.
     * @param locale explicit localization key if available, or null
     * @return descriptive label - should always return something, never null.
     */
    public String getLabel(String field, String key, String locale);

    default boolean isHierarchical() {
		return false;
	}
	
    default boolean isScrollable() {
		return false;
	}

    default boolean hasIdentifier() {
		return true;
	}
    
	default public Choice getChoice(String fieldKey, String authKey, String locale) {
		Choice result = new Choice();
		result.authority = authKey;
		result.label = getLabel(fieldKey, authKey, locale);
		result.value = getLabel(fieldKey, authKey, locale);
		return result;
	}
	
}
